# Handoff — APK universal e seis lojas (2026-11-28)

Branch isolada: `feat/v2.9.1-hub-viral`.

- `.github/workflows/android.yml`: opção `workflow_dispatch` `include_universal_apk`, ligada por padrão; APK universal assinado em artifact próprio `midiastore-release-universal-apk-v2.9.0`; AAB em `midiastore-release-aab`.
- `docs/distribuicao-lojas/PACOTE_6_LOJAS.md`: título, descrições, tags e cadastro para Samsung Galaxy Store, Uptodown, Xiaomi GetApps, OPPO App Market, Indus Appstore, APKPure e AlternativeTo.
- Release script valida pacote/certificado e gera AAB/APK. Configuração atual não define splits por ABI.
- O job requer environment `play-release` e segredos de release configurados, não publica em loja automaticamente.
- Sem Gradle local e sem execução do workflow nesta tarefa. Memória Supabase indisponível por falta de `SUPABASE_ACCESS_TOKEN`.
