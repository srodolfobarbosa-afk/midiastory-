#!/usr/bin/env node
/**
 * PULO DO GATO — Ponte de Voz pelo Celular (Zero Microfone no PC!)
 * 100% Node.js (Zero Python)
 *
 * Permite falar com o Antigravity usando o microfone do CELULAR na mesma rede Wi-Fi:
 *  1) Abre http://<IP-DO-PC>:8765 no celular -> 1 toque grava/envia áudio -> transcreve com voice-transcribe.mjs
 *     e digita automaticamente no terminal do Antigravity + ENTER!
 *  2) Também monitora a pasta `C:\Users\rodolfo\Downloads\voz-para-ia\` (qualquer .ogg/.mp3/.m4a/.wav jogado lá
 *     é transcrito e enviado automaticamente para o chat!).
 */

import http from "node:http";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");
const TRANSCRIBE_SCRIPT = path.join(projectRoot, "scripts", "voice-transcribe.mjs");
const FFMPEG = "C:\\Users\\rodolfo\\tools\\ffmpeg\\bin\\ffmpeg.exe";
const DROP_DIR = path.join(process.env.USERPROFILE || os.homedir(), "Downloads", "voz-para-ia");
const PORT = Number(process.env.VOICE_BRIDGE_PORT || 8765);

fs.mkdirSync(DROP_DIR, { recursive: true });

function getLocalIps() {
  const nets = os.networkInterfaces();
  const ips = [];
  for (const name of Object.keys(nets)) {
    for (const net of nets[name] || []) {
      if (net.family === "IPv4" && !net.internal) {
        ips.push(net.address);
      }
    }
  }
  return ips;
}

function sendTextToTerminal(text) {
  const clean = String(text || "").trim();
  if (!clean) return false;
  const tmpTxt = path.join(os.tmpdir(), `antigravity-bridge-send-${Date.now()}.txt`);
  fs.writeFileSync(tmpTxt, clean, "utf8");
  const ps = `
Add-Type @"
using System;
using System.Runtime.InteropServices;
public class BridgeFocus {
  [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr hWnd);
  [DllImport("user32.dll")] public static extern bool BringWindowToTop(IntPtr hWnd);
  [DllImport("user32.dll")] public static extern IntPtr GetForegroundWindow();
  [DllImport("user32.dll")] public static extern uint GetWindowThreadProcessId(IntPtr hWnd, out uint pid);
  [DllImport("user32.dll")] public static extern bool AttachThreadInput(uint a, uint b, bool f);
  [DllImport("kernel32.dll")] public static extern uint GetCurrentThreadId();
  [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr hWnd, int n);
  [DllImport("user32.dll", CharSet = CharSet.Unicode)] public static extern bool PostMessageW(IntPtr hWnd, uint Msg, IntPtr w, IntPtr l);
  public static void Send(IntPtr hWnd, string text) {
    IntPtr fg = GetForegroundWindow(); uint d=0;
    uint ft = fg != IntPtr.Zero ? GetWindowThreadProcessId(fg, out d) : 0;
    uint mt = GetCurrentThreadId();
    bool att = (ft != 0 && ft != mt) ? AttachThreadInput(mt, ft, true) : false;
    ShowWindow(hWnd, 9); BringWindowToTop(hWnd); SetForegroundWindow(hWnd);
    if (att) AttachThreadInput(mt, ft, false);
    System.Threading.Thread.Sleep(120);
    int i = 0;
    foreach (char c in text) {
      PostMessageW(hWnd, 0x0102, (IntPtr)(c == '\\r' || c == '\\n' ? ' ' : c), IntPtr.Zero);
      if (++i % 25 == 0) System.Threading.Thread.Sleep(4);
    }
    System.Threading.Thread.Sleep(180);
    PostMessageW(hWnd, 0x0100, (IntPtr)0x0D, IntPtr.Zero);
    PostMessageW(hWnd, 0x0101, (IntPtr)0x0D, IntPtr.Zero);
  }
}
"@
$txt = (Get-Content -LiteralPath '${tmpTxt.replace(/'/g, "''")}' -Raw -Encoding UTF8).Trim()
$t = Get-Process -Name "WindowsTerminal","cmd","powershell","pwsh" -ErrorAction SilentlyContinue |
  Where-Object { $_.MainWindowHandle -ne 0 } |
  Sort-Object { if ($_.MainWindowTitle -match "cmd\\.exe|Antigravity|Terminal") { 0 } else { 1 } }, StartTime -Descending |
  Select-Object -First 1
if ($t) { [BridgeFocus]::Send($t.MainWindowHandle, $txt); Write-Output "SENT_OK" }
`;
  const r = spawnSync("powershell.exe", ["-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", ps], {
    encoding: "utf8",
    windowsHide: true,
    timeout: 15000,
  });
  try { fs.unlinkSync(tmpTxt); } catch {}
  return (r.stdout || "").includes("SENT_OK");
}

function processAudioFile(inputPath) {
  const ts = Date.now();
  const mp3Path = path.join(os.tmpdir(), `antigravity-phone-${ts}.mp3`);
  const outTxt = `${mp3Path}.txt`;

  // Converte qualquer formato (ogg/m4a/3gp/webm/wav/amr/mp3) para MP3 16kHz mono limpo
  const conv = spawnSync(
    FFMPEG,
    ["-y", "-i", inputPath, "-ar", "16000", "-ac", "1", "-c:a", "libmp3lame", "-b:a", "32k", mp3Path, "-loglevel", "error"],
    { windowsHide: true, timeout: 30000 }
  );
  if (conv.status !== 0 || !fs.existsSync(mp3Path)) {
    throw new Error("Falha ao converter áudio com FFmpeg.");
  }

  const stt = spawnSync(process.execPath, [TRANSCRIBE_SCRIPT, mp3Path, outTxt], {
    cwd: projectRoot,
    encoding: "utf8",
    windowsHide: true,
    timeout: 45000,
  });

  let text = "";
  if (fs.existsSync(outTxt)) {
    text = fs.readFileSync(outTxt, "utf8").trim();
    try { fs.unlinkSync(outTxt); } catch {}
  }
  try { fs.unlinkSync(mp3Path); } catch {}

  if (stt.status !== 0 || !text) {
    throw new Error("Não foi possível transcrever fala clara.");
  }
  const sent = sendTextToTerminal(text);
  return { text, sent };
}

const HTML_PAGE = `<!doctype html>
<html lang="pt-BR">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Microfone Celular → Antigravity</title>
<style>
  body{margin:0;background:#000;color:#fff;font-family:system-ui,-apple-system,sans-serif;display:flex;align-items:center;justify-content:center;min-height:100vh;padding:20px;box-sizing:border-box}
  .card{width:100%;max-width:420px;background:#1C1C1E;border:1px solid #0A84FF;border-radius:24px;padding:28px 22px;text-align:center;box-shadow:0 0 32px rgba(10,132,255,.25)}
  h1{margin:0 0 8px;font-size:1.35rem}
  p{margin:0 0 22px;color:#AEAEB2;font-size:.92rem}
  .btn{display:block;width:100%;padding:18px;border-radius:16px;background:#0A84FF;color:#fff;font-size:1.1rem;font-weight:800;border:none;cursor:pointer;text-align:center;box-sizing:border-box}
  input[type=file]{display:none}
  #status{margin-top:18px;font-size:.92rem;color:#64D2FF;min-height:24px;white-space:pre-wrap}
  .ok{color:#30D158!important}
  .err{color:#FF453A!important}
</style>
</head>
<body>
  <div class="card">
    <h1>🎤 Falar com a IA pelo Celular</h1>
    <p>Toque no botão abaixo para gravar no microfone do celular (ou escolher um áudio) e enviar direto para o Antigravity no PC.</p>
    <label class="btn" for="audioInput">🎙️ Gravar / Enviar Áudio</label>
    <input id="audioInput" type="file" accept="audio/*" capture="microphone">
    <div id="status">Pronto para receber sua voz.</div>
  </div>
<script>
const inp = document.getElementById('audioInput');
const st = document.getElementById('status');
inp.addEventListener('change', async () => {
  const file = inp.files && inp.files[0];
  if (!file) return;
  st.className = '';
  st.textContent = '⚡ Enviando e transcrevendo (' + Math.round(file.size/1024) + ' KB)...';
  try {
    const res = await fetch('/upload?ext=' + encodeURIComponent((file.name.split('.').pop() || 'm4a')), {
      method: 'POST',
      body: await file.arrayBuffer(),
    });
    const data = await res.json();
    if (res.ok && data.ok) {
      st.className = 'ok';
      st.textContent = '✅ Enviado para a IA:\\n"' + data.text + '"';
    } else {
      st.className = 'err';
      st.textContent = '❌ ' + (data.error || 'Falha ao transcrever');
    }
  } catch (e) {
    st.className = 'err';
    st.textContent = '❌ Erro de conexão: ' + e.message;
  }
  inp.value = '';
});
</script>
</body>
</html>`;

const server = http.createServer((req, res) => {
  if (req.method === "GET" && (req.url === "/" || req.url.startsWith("/?"))) {
    res.writeHead(200, { "Content-Type": "text/html; charset=utf-8" });
    res.end(HTML_PAGE);
    return;
  }
  if (req.method === "POST" && req.url.startsWith("/upload")) {
    const chunks = [];
    req.on("data", (c) => chunks.push(c));
    req.on("end", () => {
      const buf = Buffer.concat(chunks);
      const u = new URL(req.url, "http://localhost");
      const ext = (u.searchParams.get("ext") || "m4a").replace(/[^a-z0-9]/gi, "") || "m4a";
      const tmpIn = path.join(os.tmpdir(), `phone-upload-${Date.now()}.${ext}`);
      try {
        fs.writeFileSync(tmpIn, buf);
        const result = processAudioFile(tmpIn);
        res.writeHead(200, { "Content-Type": "application/json; charset=utf-8" });
        res.end(JSON.stringify({ ok: true, ...result }));
      } catch (err) {
        res.writeHead(500, { "Content-Type": "application/json; charset=utf-8" });
        res.end(JSON.stringify({ ok: false, error: err.message }));
      } finally {
        try { fs.unlinkSync(tmpIn); } catch {}
      }
    });
    return;
  }
  res.writeHead(404);
  res.end("Not found");
});

// Também vigia a pasta C:\Users\rodolfo\Downloads\voz-para-ia\ a cada 2s (áudios baixados do WhatsApp Web!)
const seenFiles = new Set();
setInterval(() => {
  try {
    const files = fs.readdirSync(DROP_DIR).filter((f) => /\.(ogg|opus|mp3|m4a|wav|aac|3gp|webm)$/i.test(f));
    for (const f of files) {
      const full = path.join(DROP_DIR, f);
      if (seenFiles.has(full)) continue;
      seenFiles.add(full);
      try {
        const r = processAudioFile(full);
        console.log(`[DROP_DIR] Transcrito e enviado: "${r.text}"`);
        fs.unlinkSync(full);
      } catch (e) {
        console.error(`[DROP_DIR] Erro em ${f}:`, e.message);
      }
    }
  } catch {}
}, 2000);

server.listen(PORT, "0.0.0.0", () => {
  const ips = getLocalIps();
  console.log("PHONE_MIC_BRIDGE_ONLINE=true");
  console.log("DROP_DIR=" + DROP_DIR);
  for (const ip of ips) {
    console.log(`PHONE_URL=http://${ip}:${PORT}/`);
  }
});
