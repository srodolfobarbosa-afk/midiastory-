import fs from "node:fs";
import path from "node:path";
import { getSupabaseToken, SUPABASE_PROJECT_REF } from "./lib/supabase-auth.mjs";

export const CHECKLIST_TAG = "[CHECKLIST_VIVO:MIDIASTORE]";

const FORBIDDEN_SECRET_PATTERNS = [
  /AIza[0-9A-Za-z\-_]{30,}/g,
  /sk-or-v1-[0-9a-f]{20,}/gi,
  /sk-proj-[0-9A-Za-z\-_]{20,}/gi,
  /sbp_[0-9a-f]{20,}/gi,
  /ghp_[0-9A-Za-z]{20,}/g,
  /github_pat_[0-9A-Za-z_]{20,}/g,
  /AQ\.Ab8RN[0-9A-Za-z\-_]{10,}/g,
  /-----BEGIN [A-Z ]*PRIVATE KEY-----/g,
  /GOCSPX-[0-9A-Za-z\-_]{15,}/g,
];

export function assertNoSecrets(text) {
  for (const re of FORBIDDEN_SECRET_PATTERNS) {
    re.lastIndex = 0;
    if (re.test(text)) {
      throw new Error(`ABORTED: Conteudo contem padrao proibido de segredo (${re.source}).`);
    }
  }
}

export async function runSupabaseSql(query) {
  const token = getSupabaseToken();
  const res = await fetch(
    `https://api.supabase.com/v1/projects/${SUPABASE_PROJECT_REF}/database/query`,
    {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ query }),
    }
  );
  const text = await res.text();
  if (!res.ok) {
    throw new Error(`Supabase SQL HTTP ${res.status}: ${text}`);
  }
  return JSON.parse(text);
}

function sqlLiteral(value) {
  return `'${String(value).replace(/'/g, "''")}'`;
}

export function renderChecklistMarkdown(state) {
  const lines = [];
  lines.push(CHECKLIST_TAG);
  lines.push(`# Bloco de Notas Checklist Vivo — Midiastore (${state.packageName || "com.rdf112.midiastore"})`);
  lines.push(`Atualizado em: ${state.updatedAt}`);
  lines.push(`Meta Fundamental: ${state.goal || "R$ 1.000.000,00 em vendas reais na Google Play"}`);
  lines.push(`Snapshot DB/Play: ${state.snapshotSummary || "verificado"}`);
  lines.push("");

  const active = (state.items || []).filter((i) => i.status !== "done");
  const done = (state.items || []).filter((i) => i.status === "done");

  lines.push(`## ABERTOS E EM ANDAMENTO (${active.length})`);
  if (active.length === 0) {
    lines.push("- Nenhum item aberto.");
  } else {
    for (const item of active) {
      const box = item.status === "in_progress" ? "[~]" : item.status === "blocked_owner" ? "[!]" : "[ ]";
      const weight = item.weight ? ` (Peso ${item.weight})` : "";
      lines.push(`- ${box} **${item.id}**${weight} [${item.area}] — ${item.title}`);
      if (item.finding) lines.push(`  - Diagnostico/Pesquisa: ${item.finding}`);
      if (item.nextAction) lines.push(`  - Proxima acao: ${item.nextAction}`);
      if (item.ownerGate) lines.push(`  - Depende do dono: ${item.ownerGate}`);
    }
  }

  lines.push("");
  lines.push(`## RESOLVIDOS E CONCLUIDOS (${done.length})`);
  if (done.length === 0) {
    lines.push("- Nenhum ainda.");
  } else {
    for (const item of done) {
      lines.push(`- [x] **${item.id}** [${item.area}] — ${item.title}`);
      if (item.proof) lines.push(`  - Prova real: ${item.proof}`);
    }
  }

  lines.push("");
  lines.push("<!-- CHECKLIST_JSON_START");
  lines.push(JSON.stringify(state, null, 2));
  lines.push("CHECKLIST_JSON_END -->");
  return lines.join("\n");
}

export function parseChecklistState(content) {
  const match = String(content || "").match(/<!-- CHECKLIST_JSON_START\s*([\s\S]*?)\s*CHECKLIST_JSON_END -->/);
  if (!match) return null;
  return JSON.parse(match[1]);
}

export async function getChecklistRow() {
  const rows = await runSupabaseSql(`
    SELECT id, content, created_at
    FROM public.documents
    WHERE content LIKE '${CHECKLIST_TAG}%'
    ORDER BY id DESC
    LIMIT 1;
  `);
  return rows[0] || null;
}

export async function saveChecklistState(state) {
  state.updatedAt = new Date().toISOString();
  const markdown = renderChecklistMarkdown(state);
  assertNoSecrets(markdown);

  const existing = await getChecklistRow();
  if (existing) {
    const updated = await runSupabaseSql(`
      UPDATE public.documents
      SET content = ${sqlLiteral(markdown)},
          created_at = NOW()
      WHERE id = ${Number(existing.id)}
      RETURNING id, length(content) AS len, created_at;
    `);
    return { action: "updated", row: updated[0], markdown };
  }

  const inserted = await runSupabaseSql(`
    INSERT INTO public.documents (content, embedding)
    VALUES (${sqlLiteral(markdown)}, NULL)
    RETURNING id, length(content) AS len, created_at;
  `);
  return { action: "inserted", row: inserted[0], markdown };
}

function parseArgs(argv) {
  const opts = { _: [] };
  for (let i = 0; i < argv.length; i++) {
    const a = argv[i];
    if (a.startsWith("--")) {
      const k = a.slice(2);
      const next = argv[i + 1];
      if (!next || next.startsWith("--")) {
        opts[k] = true;
      } else {
        opts[k] = next;
        i++;
      }
    } else {
      opts._.push(a);
    }
  }
  return opts;
}

async function main() {
  const opts = parseArgs(process.argv.slice(2));
  const cmd = opts._[0] || "show";

  if (cmd === "show") {
    const row = await getChecklistRow();
    if (!row) {
      console.log("Nenhum checklist encontrado no Supabase ainda.");
      return;
    }
    console.log(`=== public.documents id=${row.id} (${row.created_at}) ===`);
    console.log(row.content.replace(/<!-- CHECKLIST_JSON_START[\s\S]*?CHECKLIST_JSON_END -->/, "").trim());
    return;
  }

  if (cmd === "upsert-json") {
    if (!opts.file) throw new Error("Uso: upsert-json --file caminho.json [--mirror caminho.md]");
    const raw = fs.readFileSync(path.resolve(opts.file), "utf8");
    const state = JSON.parse(raw);
    const res = await saveChecklistState(state);
    if (opts.mirror) {
      fs.writeFileSync(path.resolve(opts.mirror), res.markdown.replace(/<!-- CHECKLIST_JSON_START[\s\S]*?CHECKLIST_JSON_END -->\s*$/, "").trim() + "\n", "utf8");
    }
    console.log(JSON.stringify({ ok: true, action: res.action, row: res.row }, null, 2));
    return;
  }

  const row = await getChecklistRow();
  if (!row) throw new Error("Checklist base nao encontrado. Rode upsert-json primeiro.");
  const state = parseChecklistState(row.content);
  if (!state) throw new Error("Bloco JSON nao encontrado no checklist.");

  if (cmd === "add") {
    if (!opts.id || !opts.title) throw new Error("Uso: add --id ID --area AREA --title TITULO [--status open|in_progress|blocked_owner] [--weight 0.90] [--finding ...] [--next ...] [--owner ...]");
    const idx = state.items.findIndex((i) => i.id === opts.id);
    const entry = {
      id: opts.id,
      area: opts.area || "GERAL",
      status: opts.status || "open",
      weight: opts.weight || undefined,
      title: opts.title,
      finding: opts.finding || undefined,
      nextAction: opts.next || undefined,
      ownerGate: opts.owner || undefined,
    };
    if (idx >= 0) state.items[idx] = { ...state.items[idx], ...entry };
    else state.items.unshift(entry);
    const res = await saveChecklistState(state);
    console.log(JSON.stringify({ ok: true, action: res.action, row: res.row, item: entry }, null, 2));
    return;
  }

  if (cmd === "resolve") {
    if (!opts.id || !opts.proof) throw new Error("Uso: resolve --id ID --proof PROVA");
    const item = state.items.find((i) => i.id === opts.id);
    if (!item) throw new Error(`Item '${opts.id}' nao encontrado.`);
    item.status = "done";
    item.proof = opts.proof;
    delete item.ownerGate;
    delete item.nextAction;
    const res = await saveChecklistState(state);
    console.log(JSON.stringify({ ok: true, action: res.action, row: res.row, resolvedId: opts.id }, null, 2));
    return;
  }

  if (cmd === "remove") {
    if (!opts.id) throw new Error("Uso: remove --id ID");
    const before = state.items.length;
    state.items = state.items.filter((i) => i.id !== opts.id);
    if (state.items.length === before) throw new Error(`Item '${opts.id}' nao encontrado.`);
    const res = await saveChecklistState(state);
    console.log(JSON.stringify({ ok: true, action: res.action, row: res.row, removedId: opts.id }, null, 2));
    return;
  }

  throw new Error(`Comando desconhecido: ${cmd}`);
}

if (process.argv[1] && path.resolve(process.argv[1]) === path.resolve(new URL(import.meta.url).pathname.replace(/^\/([A-Za-z]:)/, "$1"))) {
  main().catch((err) => {
    console.error("Erro:", err.message);
    process.exit(1);
  });
}
