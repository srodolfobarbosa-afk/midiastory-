# Guia de Publicação — E-book Oficial Google Play Books (`Midiastore Series`)

> [!IMPORTANT]
> Este pacote está pronto para envio imediato no **Google Play Books Partner Center**. O upload, cadastro bancário/fiscal e revisão de direitos autorais na conta de editora são etapas exclusivas do proprietário da conta.

---

## 1. Arquivos Prontos no Repositório (`docs/ebook/`)
*(Protegidos na exclusão do Firebase Hosting para que o livro pago não fique exposto gratuitamente no site).*

| Arquivo | Especificação | Uso na Publicação |
|---|---|---|
| `Celular_Lotado_Nunca_Mais_Midiastore.epub` | **EPUB 3.0 válido** (8 capítulos completos + TOC EPUB3/NCX + CSS compatível com tema Claro/Sépia/Escuro + link UTM `?utm_source=play_books`) | Arquivo principal do livro (`Book file`) no Play Books, KDP ou Hotmart |
| `capa-play-books-1600x2560.png` | **PNG `1600 × 2560 px`** (lado menor 1600 px no padrão recomendado pelo Google Play Books) | Arquivo de Capa (`Cover image`) |

Para regerar capa + EPUB após qualquer alteração:
```bash
node scripts/build-playbook-epub.mjs
```

---

## 2. Metadados Prontos Para Copiar & Colar no Google Play Books Partner Center

- **Acesse:** [play.google.com/books/publish](https://play.google.com/books/publish)
- **Título:** `Celular Lotado Nunca Mais: O Guia Definitivo da Memória Android`
- **Subtítulo:** `Como Recuperar Gigabytes Reais, Proteger Sua Privacidade EXIF/GPS e Evitar Apps Enganosos`
- **Autor:** `Rodolfo Barbosa` (ou `Midiastore Editorial`)
- **Idioma:** `Português (Brasil)`
- **Descrição / Sinopse Comercial:**
  > Chega de apagar dez fotos sem efeito e ver o alerta "Armazenamento quase cheio" voltar dez minutos depois.
  >
  > Neste guia prático de engenharia Android traduzida para o dia a dia, você aprende exatamente por que os gigabytes somem em aparelhos modernos, como mensageiros multiplicam vídeos repetidos silenciosamente, por que apps que prometem "turbinar RAM ou resfriar CPU" enganam o usuário e como usar verificação criptográfica SHA-256 byte a byte, remoção de metadados GPS (EXIF), conversão local de áudio M4A, geração de QR Code e PDFs A4 sem entregar seus arquivos para a nuvem de desconhecidos.
  >
  > Inclui checklist semanal de 3 minutos e links diretos para ferramentas gratuitas no navegador e acesso antecipado oficial do projeto **Midiastore** (`com.rdf112.midiastore`).

- **Categorias BISAC sugeridas:**
  1. `COMPUTERS / Operating Systems / Android` (`COM046040`)
  2. `COMPUTERS / Security / Privacy & Surveillance` (`COM060080`)
  3. `COMPUTERS / Utilities` (`COM000000`)
- **Preço Sugerido (Faixa de Royalties 70% no Google Play Books):**
  - **Brasil (`BRL`):** `R$ 19,90` (promoção inicial) ou `R$ 24,90` (padrão). *Com ~500 vendas/mês já gera caixa recorrente significativo para financiar campanhas de mídia e infraestrutura de IA.*
  - **EUA / Global (`USD`):** `US$ 4.99` (ou `US$ 6.99`).
- **DRM:** Opcional (recomenda-se ativar DRM padrão da loja ou manter marca d'água social + CTA rastreável no Capítulo 8 para converter leitores em usuários pagantes do **Midiastore Pro**!).

---

## 3. Fluxo de 4 Passos no Partner Center

1. Crie a conta gratuita de editor/autor no **Google Play Books Partner Center** e conclua seu **Perfil de Pagamentos** (pode vincular o mesmo perfil financeiro habilitado em `2026-11-03`).
2. Clique em **Adicionar Livro → Vender e-book no Google Play**.
3. Preencha os metadados da Seção 2 acima e faça upload de **`Celular_Lotado_Nunca_Mais_Midiastore.epub`** + **`capa-play-books-1600x2560.png`** na aba **Conteúdo**.
4. Defina países (`Todo o mundo` ou `Brasil + mercados lusófonos` se publicado em PT-BR), informe o preço (`R$ 19,90` / `US$ 4.99`) na aba **Preços** e clique em **Publicar**.
