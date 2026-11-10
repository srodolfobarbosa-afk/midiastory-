import fs from "node:fs";

const content = fs.readFileSync("C:/Users/rodolfo/.gemini/antigravity-cli/brain/12d4c83d-c93e-424b-b8c0-e2b7f0aa2ee9/.system_generated/steps/626/output.txt", "utf8");
const matches = content.match(/"productId":\s*"([^"]+)"/g) || [];
const ids = matches.map((m) => m.replace(/"productId":\s*"/, "").replace(/"/, ""));
console.log("Produtos encontrados no Google Play:", Array.from(new Set(ids)));
