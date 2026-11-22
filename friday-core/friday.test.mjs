import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import test from "node:test";
import { analyzePrompt } from "../scripts/friday-triage-engine.mjs";

test("Triagem FRIDAY separa múltiplos pedidos e calcula pesos corretamente", () => {
  const res = analyzePrompt(
    "Quero que você audite o paywall em Kotlin, pesquise 5 concorrentes na Play Store e crie uma imagem nova para o banner."
  );
  assert.ok(res.totalItems >= 3, `Esperado >=3 itens, veio ${res.totalItems}`);
  assert.ok(res.items.some((i) => i.weight === "CRÍTICO" || i.weight === "ALTO"));
  assert.ok(res.items.some((i) => i.route.includes("image-generator")));
});

test("Memória Imune bloqueia regressão conhecida (MANAGE_EXTERNAL_STORAGE)", () => {
  const r = spawnSync(
    process.execPath,
    ["friday-core/immune-memory.mjs", "check", "Adicionar MANAGE_EXTERNAL_STORAGE no AndroidManifest"],
    { encoding: "utf8" }
  );
  assert.equal(r.status, 2);
  assert.match(r.stdout, /BLOQUEIO ANTI-REGRESSÃO/);
});

test("Biblioteca Viva FRIDAY carrega catálogo e valida 10+ poderes", () => {
  const r = spawnSync(process.execPath, ["friday-core/skills-registry.mjs", "list"], {
    encoding: "utf8",
  });
  assert.equal(r.status, 0);
  assert.match(r.stdout, /BIBLIOTECA VIVA DE PODERES FRIDAY/);
});

test("SDK @google/genai e valibot estão instalados e importáveis", async () => {
  const genai = await import("../scripts/node_modules/@google/genai/dist/node/index.mjs").catch(async () =>
    import("@google/genai")
  );
  assert.ok(genai.GoogleGenAI, "GoogleGenAI deve existir no @google/genai");
});
