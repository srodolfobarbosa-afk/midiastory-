import fs from "node:fs";
import path from "node:path";
import { execFileSync } from "node:child_process";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");
const docsDir = path.join(projectRoot, "docs");
const projectRef = "usmqtnlsaavxqguwrtix";

const LOCALE_PAGES = [
  "index.html",
  "en/index.html",
  "es/index.html",
  "de/index.html",
  "fr/index.html",
  "hi/index.html",
  "ja/index.html",
  "zh/index.html",
];

const EXTRA_PAGES = [
  "diagnostico.html",
  "ferramentas.html",
  "privacidade.html",
];

const PROHIBITED_PATTERNS = [
  { label: "Fake compression claim", regex: /compressão inteligente|reduz até 80%/i },
  { label: "RAM boost / cooler claim", regex: /ram boost|cpu cooler|resfriamento de cpu|aceleração de ram/i },
  { label: "Unimplemented blur / visual similarity claim", regex: /blur detection|visual similarity|fotos borradas/i },
  { label: "Unimplemented periodic background reminders claim", regex: /alertas de limpeza periódica|recurring background cleaning reminders|recordatorios periódicos inteligentes|automatische Bereinigungs-Erinnerungen|rappels périodiques intelligents|ऑटोमैटिक रिमाइंडर|定期的な空き容量通知|定期清理智能提醒/i },
  { label: "Hindi superfast phone claim", regex: /अपने फोन को सुपरफास्ट बनाएं/ },
  { label: "Chinese extreme performance claim", regex: /释放极致手机性能/ },
  { label: "Revoked/fake Supabase anon key", regex: /zP5bQ9gJ2e-m6zW-b2c9d7q5s4t3r2a1x9w8v7u6t5r/ },
  { label: "Broken Google Group URL (302 -> access-error)", regex: /groups\.google\.com\/g\/midiastore-testers/i },
  { label: "Unverified 3 months free Pro promo claim", regex: /3\s*meses\s*de\s*(?:midiastore\s*)?pro\s*gr[aá]tis|3\s*months\s*of\s*midiastore\s*pro\s*free|3\s*monate\s*midiastore\s*pro\s*gratis|3\s*mois\s*de\s*midiastore\s*pro\s*gratuits|3\s*महीने\s*का\s*midiastore\s*pro|midiastore\s*pro\s*3ヶ月分|3个月\s*midiastore\s*pro/i },
  { label: "Outdated 20 testers quota claim", regex: /20\s*testadores|20\s*active\s*testers|20\s*volunteer\s*testers|20\s*probadores|20\s*freiwillige\s*tester|20\s*aktive\s*tester|20\s*testeurs|20\s*परीक्षकों|20\s*टेस्टर|20名の|20名测试者/i },
];

function resolveMcpConfigPath() {
  const localPath = path.join(projectRoot, ".agents", "plugins", "supabase", "mcp_config.json");
  if (fs.existsSync(localPath)) return localPath;
  try {
    const gitCommonDir = execFileSync("git", ["rev-parse", "--git-common-dir"], {
      cwd: projectRoot,
      encoding: "utf8",
      stdio: ["ignore", "pipe", "ignore"],
    }).trim();
    if (gitCommonDir) {
      const mainRepoRoot = path.resolve(projectRoot, gitCommonDir, "..");
      const mainPath = path.join(mainRepoRoot, ".agents", "plugins", "supabase", "mcp_config.json");
      if (fs.existsSync(mainPath)) return mainPath;
    }
  } catch {
    // ignore
  }
  return localPath;
}

function checkInternalLinks(relFile, html) {
  const hrefRegex = /href="(\/[^"#?]*)(?:[?#][^"]*)?"/g;
  const broken = [];
  let match;
  while ((match = hrefRegex.exec(html)) !== null) {
    const rawPath = match[1];
    let targetFile;
    if (rawPath === "/") {
      targetFile = path.join(docsDir, "index.html");
    } else if (rawPath.endsWith("/")) {
      targetFile = path.join(docsDir, rawPath.slice(1), "index.html");
    } else {
      targetFile = path.join(docsDir, rawPath.slice(1));
    }
    if (!fs.existsSync(targetFile)) {
      broken.push(`${rawPath} (missing ${path.relative(projectRoot, targetFile)})`);
    }
  }
  return broken;
}

async function verify() {
  console.log("=== VERIFICAÇÃO END-TO-END DO FUNIL DE TESTADORES (LOCAL + SUPABASE + HOSTING) ===");
  let failures = 0;

  function assertCheck(name, condition, detail = "") {
    if (condition) {
      console.log(` ✅ [PASS] ${name}`);
    } else {
      console.error(` ❌ [FAIL] ${name}${detail ? ` -> ${detail}` : ""}`);
      failures++;
    }
  }

  // 1. Verificar todas as 8 páginas de idioma locais
  console.log("\n1. Validando as 8 páginas internacionais do funil (docs/**/index.html)...");
  let extractedAnonKey = "";

  for (const relPage of LOCALE_PAGES) {
    const fullPath = path.join(docsDir, relPage);
    if (!fs.existsSync(fullPath)) {
      assertCheck(`Arquivo existe: docs/${relPage}`, false, "Arquivo não encontrado");
      continue;
    }
    const html = fs.readFileSync(fullPath, "utf8");
    const hasVip = html.includes('id="vip-testers"') && html.includes('id="testerForm"');
    const hasOnboarding =
      html.includes('href="#testerEmail"') &&
      html.includes("https://play.google.com/apps/testing/com.rdf112.midiastore") &&
      html.includes("https://play.google.com/store/apps/details?id=com.rdf112.midiastore") &&
      html.includes("12+") &&
      html.includes("15+");
    const brokenLinks = checkInternalLinks(relPage, html);
    const prohibitedHit = PROHIBITED_PATTERNS.find(p => p.regex.test(html));

    const keyMatch = html.match(/const\s+ANON_KEY\s*=\s*"([^"]+)"/);
    if (keyMatch && !extractedAnonKey) {
      extractedAnonKey = keyMatch[1];
    }

    assertCheck(
      `Locale [docs/${relPage}] estrutura + 3 passos Google Play + meta 12+/15+ + zero claims/links proibidos`,
      hasVip && hasOnboarding && brokenLinks.length === 0 && !prohibitedHit && Boolean(keyMatch?.[1]),
      !hasVip
        ? "Seção #vip-testers ou #testerForm ausente"
        : !hasOnboarding
          ? "Passos de onboarding (#testerEmail / Opt-in / Play Store / meta 12+/15+) ausentes"
          : brokenLinks.length > 0
            ? `Links quebrados: ${brokenLinks.join(", ")}`
            : prohibitedHit
              ? `Padrão proibido detectado: ${prohibitedHit.label}`
              : "ANON_KEY ausente"
    );
  }

  // 2. Verificar páginas complementares (diagnostico.html, ferramentas.html, privacidade.html, sitemap.xml, llms.txt)
  console.log("\n2. Validando páginas complementares (diagnostico.html, ferramentas.html, privacidade.html, sitemap.xml, llms.txt)...");
  for (const relPage of EXTRA_PAGES) {
    const fullPath = path.join(docsDir, relPage);
    const html = fs.readFileSync(fullPath, "utf8");
    const brokenLinks = checkInternalLinks(relPage, html);
    const prohibitedHit = PROHIBITED_PATTERNS.find(p => p.regex.test(html));
    const hasTerminalMisroute = relPage === "diagnostico.html" && html.includes('href="/terminal.html"');
    const hasOptInIfFunnelPage =
      relPage === "privacidade.html" ||
      (html.includes("https://play.google.com/apps/testing/com.rdf112.midiastore") &&
        html.includes("/#vip-testers"));

    assertCheck(
      `Página [docs/${relPage}] links válidos + CTA funil + zero claims proibidos`,
      brokenLinks.length === 0 && !prohibitedHit && !hasTerminalMisroute && hasOptInIfFunnelPage,
      hasTerminalMisroute
        ? "CTA apontando para /terminal.html em vez de /#vip-testers"
        : !hasOptInIfFunnelPage
          ? "Falta link para /#vip-testers ou Opt-in da Google Play"
          : brokenLinks.length > 0
            ? `Links quebrados: ${brokenLinks.join(", ")}`
            : prohibitedHit?.label
    );
  }

  const sitemapXml = fs.readFileSync(path.join(docsDir, "sitemap.xml"), "utf8");
  assertCheck(
    "Sitemap XML inclui /diagnostico.html, /ferramentas.html e /privacidade.html",
    sitemapXml.includes("/diagnostico.html") &&
      sitemapXml.includes("/ferramentas.html") &&
      sitemapXml.includes("/privacidade.html")
  );

  const llmsTxt = fs.readFileSync(path.join(docsDir, "llms.txt"), "utf8");
  assertCheck(
    "llms.txt aponta para /privacidade.html, inclui link de Opt-in e zero Google Group quebrado",
    llmsTxt.includes("https://midiastory-df6a0.web.app/privacidade.html") &&
      llmsTxt.includes("https://play.google.com/apps/testing/com.rdf112.midiastore") &&
      !llmsTxt.includes("groups.google.com/g/midiastore-testers")
  );

  // 3. Testar ANON_KEY contra o gateway Supabase (sem inserir lixo no banco)
  console.log("\n3. Testando autenticação da ANON_KEY do funil no Supabase REST API...");
  if (extractedAnonKey) {
    const anonRes = await fetch(`https://${projectRef}.supabase.co/rest/v1/beta_testers`, {
      method: "POST",
      headers: {
        apikey: extractedAnonKey,
        Authorization: `Bearer ${extractedAnonKey}`,
        "Content-Type": "application/json",
        Prefer: "return=minimal",
      },
      body: JSON.stringify({}),
    });
    assertCheck(
      "Supabase REST API aceita a ANON_KEY do funil (zero erro 401 Invalid API key)",
      anonRes.status === 400 || anonRes.status === 201 || anonRes.status === 409,
      `Status inesperado: HTTP ${anonRes.status} (${await anonRes.text()})`
    );
  } else {
    assertCheck("ANON_KEY extraída de docs/index.html", false, "Não encontrada");
  }

  // 4. Consultar contagem atual de beta_testers no Supabase
  const mcpConfigPath = resolveMcpConfigPath();
  let token = process.env.SUPABASE_ACCESS_TOKEN || "";
  if (!token && fs.existsSync(mcpConfigPath)) {
    const raw = JSON.parse(fs.readFileSync(mcpConfigPath, "utf8"));
    token =
      raw?.mcpServers?.supabase?.env?.SUPABASE_ACCESS_TOKEN ||
      raw?.mcpServers?.supabase?.headers?.Authorization?.replace(/^Bearer\s+/i, "") ||
      "";
  }

  if (token) {
    const restRes = await fetch(`https://api.supabase.com/v1/projects/${projectRef}/database/query`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token.trim()}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        query: "SELECT count(*) AS total, count(*) FILTER (WHERE email NOT LIKE 'teste_%') AS real_count FROM public.beta_testers;",
      }),
    });
    if (restRes.ok) {
      const restData = await restRes.json();
      console.log(` ℹ️ Banco public.beta_testers: ${restData[0]?.real_count} testador(es) reais (${restData[0]?.total} registros totais)`);
    }
  }

  if (failures > 0) {
    console.error(`\n⚠️ Verificação encontrou ${failures} falha(s)!`);
    process.exit(1);
  }

  console.log("\n🎉 FUNIL LOCAL 100% VALIDADO E PRONTO PARA CONVERSÃO DE TESTADORES!");
}

verify().catch(e => {
  console.error("Erro na verificação:", e.message);
  process.exit(1);
});
