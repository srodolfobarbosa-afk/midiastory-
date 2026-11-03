# Checklist Oficial de Publicação na Google Play Console — Sweep v2.1.0 (`versionCode = 13`)

## 1. Identidade e Versão do Pacote
- [x] **Nome Comercial:** `Sweep` — *Armazenamento sob seu controle.*
- [x] **Application ID Oficial:** `com.rdf112.sweep` *(Nota: o script `COMPILAR_TUDO_WINDOWS.ps1` também gera automaticamente um segundo `.aab` assinado com `com.rdf112.midiastore` caso você queira atualizar os dois cadastros na sua conta do Google Play Console!)*
- [x] **Version Code:** `13` (superior às versões 9, 10, 11 e 12 já enviadas na Play Console).
- [x] **Version Name:** `2.1.0`
- [x] **SDK Mínimo / Alvo:** `minSdk = 29` (Android 10) / `targetSdk = 36` (Android 16 — em conformidade com a exigência da Google Play).
- [x] **Ofuscação e Símbolos R8:** `isMinifyEnabled = true` ativado no build `release`, gerando automaticamente a tabela de símbolos de desofuscação dentro do `.aab`.

## 2. Por que a Versão 12 não tinha aparecido no celular antes? (Atenção no Play Console!)
Na sua captura de tela do Google Play Console (`releases/2/prepare`), o pacote `12 (2.0.0)` já havia sido aceito na tabela inferior, mas a tela ainda estava na etapa **Preparar (Rascunho)**.
Para que a atualização chegue de verdade ao seu celular e ao celular dos testadores:
1. Remova os pacotes antigos da mesma versão clicando nos **três pontinhos (`⋮`) -> Remover app bundle** se houver mais de um na lista.
2. Envie o novo `.aab` de **Versão `13 (2.1.0)`**.
3. Clique obrigatoriamente no botão azul **`Avançar`** (canto inferior direito).
4. Na tela seguinte (*Revisar e lançar*), clique no botão azul **`Salvar e publicar`** (ou **`Iniciar lançamento para Teste interno`**) e confirme!
5. Verifique também qual dos dois links de Teste Interno está instalado no seu celular:
   - Cadastro 1 (`com.rdf112.sweep` — App ID `4975897323990286380`)
   - Cadastro 2 (`com.rdf112.midiastore` — App ID `4976071564595130630`)
   O script `COMPILAR_TUDO_WINDOWS.ps1` gera os dois arquivos `.aab` prontos na sua pasta `Downloads` (`SWEEP_2.1.0_VC13_OFICIAL.aab` e `MIDIASTORE_2.1.0_VC13_OFICIAL.aab`) para você atualizar qualquer um dos dois sem erro!
