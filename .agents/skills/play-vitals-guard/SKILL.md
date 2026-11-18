---
name: play-vitals-guard
description: Monitoramento contínuo de produção, testadores (regra 14 dias), telemetria do funil e saúde do app via Play Console API v3 e Supabase.
---

# 🛡️ Skill: Play Vitals & Growth Guard

Use esta skill para auditar a saúde do app, o relógio dos 14 dias de teste fechado e a telemetria do funil web.

## 1. Checagem ao vivo (nunca usar dado de memória)
```powershell
node scripts/monitor-site-telemetry.mjs
node scripts/list-play-testers-live.mjs
node scripts/play-console-api.mjs com.rdf112.midiastore status
```

## 2. Regras críticas
- Sempre que houver novo e-mail em `public.beta_testers`, listar imediatamente para inclusão na Play Console UI (nunca sobrescrever a lista parcial via API).
- Monitorar tamanho do banco Supabase (`node scripts/supabase-maintenance.mjs`) para manter abaixo de 400 MB (limite gratuito 500 MB).
- Auditar qualquer `.aab` com `java -jar C:\Users\rodolfo\tools\bundletool.jar dump manifest --bundle=...` antes de publicar.
