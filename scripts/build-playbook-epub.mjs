import fs from "node:fs";
import path from "node:path";
import zlib from "node:zlib";
import { execFileSync } from "node:child_process";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");
const ebookDir = path.join(projectRoot, "docs", "ebook");
const coverPs1 = path.join(__dirname, "render-book-cover.ps1");
const coverPngPath = path.join(ebookDir, "capa-play-books-1600x2560.png");
const epubOutPath = path.join(ebookDir, "Celular_Lotado_Nunca_Mais_Midiastore.epub");

const BOOK_META = {
  id: "urn:uuid:8c3b9f10-4b2a-4e8e-9a4b-6f2d8c112a01",
  title: "Celular Lotado Nunca Mais: O Guia Definitivo da Memória Android",
  subtitle: "Como Recuperar Gigabytes Reais, Proteger Sua Privacidade EXIF/GPS e Evitar Apps Enganosos",
  author: "Midiastore Editorial (Rodolfo Barbosa)",
  publisher: "Midiastore Engineering",
  lang: "pt-BR",
  modified: "2026-11-14T06:00:00Z",
};

const CTA_URL = "https://midiastory-df6a0.web.app/?utm_source=play_books&utm_medium=ebook_cta&utm_campaign=livro_memoria_android";
const TOOLS_URL = "https://midiastory-df6a0.web.app/ferramentas.html?utm_source=play_books&utm_medium=ebook_cta";
const DIAG_URL = "https://midiastory-df6a0.web.app/diagnostico.html?utm_source=play_books&utm_medium=ebook_cta";

function crc32(buf) {
  let c = 0xffffffff;
  for (let i = 0; i < buf.length; i++) {
    c ^= buf[i];
    for (let k = 0; k < 8; k++) {
      c = (c >>> 1) ^ (0xedb88320 & -(c & 1));
    }
  }
  return (c ^ 0xffffffff) >>> 0;
}

function buildEpubZip(entries) {
  const localParts = [];
  const centralParts = [];
  let offset = 0;

  for (const entry of entries) {
    const nameBuf = Buffer.from(entry.name, "utf8");
    const rawBuf = Buffer.isBuffer(entry.data) ? entry.data : Buffer.from(entry.data, "utf8");
    const method = entry.store ? 0 : 8;
    const compBuf = entry.store ? rawBuf : zlib.deflateRawSync(rawBuf, { level: 9 });
    const crc = crc32(rawBuf);

    const localHdr = Buffer.alloc(30);
    localHdr.writeUInt32LE(0x04034b50, 0);
    localHdr.writeUInt16LE(20, 4);
    localHdr.writeUInt16LE(0x0800, 6); // UTF-8 filename
    localHdr.writeUInt16LE(method, 8);
    localHdr.writeUInt16LE(0, 10);
    localHdr.writeUInt16LE(0x5421, 12);
    localHdr.writeUInt32LE(crc, 14);
    localHdr.writeUInt32LE(compBuf.length, 18);
    localHdr.writeUInt32LE(rawBuf.length, 22);
    localHdr.writeUInt16LE(nameBuf.length, 26);
    localHdr.writeUInt16LE(0, 28);

    const centralHdr = Buffer.alloc(46);
    centralHdr.writeUInt32LE(0x02014b50, 0);
    centralHdr.writeUInt16LE(20, 4);
    centralHdr.writeUInt16LE(20, 6);
    centralHdr.writeUInt16LE(0x0800, 8);
    centralHdr.writeUInt16LE(method, 10);
    centralHdr.writeUInt16LE(0, 12);
    centralHdr.writeUInt16LE(0x5421, 14);
    centralHdr.writeUInt32LE(crc, 16);
    centralHdr.writeUInt32LE(compBuf.length, 20);
    centralHdr.writeUInt32LE(rawBuf.length, 24);
    centralHdr.writeUInt16LE(nameBuf.length, 28);
    centralHdr.writeUInt16LE(0, 30);
    centralHdr.writeUInt16LE(0, 32);
    centralHdr.writeUInt16LE(0, 34);
    centralHdr.writeUInt16LE(0, 36);
    centralHdr.writeUInt32LE(0, 38);
    centralHdr.writeUInt32LE(offset, 42);

    localParts.push(localHdr, nameBuf, compBuf);
    centralParts.push(centralHdr, nameBuf);
    offset += localHdr.length + nameBuf.length + compBuf.length;
  }

  const centralSize = centralParts.reduce((a, b) => a + b.length, 0);
  const eocd = Buffer.alloc(22);
  eocd.writeUInt32LE(0x06054b50, 0);
  eocd.writeUInt16LE(0, 4);
  eocd.writeUInt16LE(0, 6);
  eocd.writeUInt16LE(entries.length, 8);
  eocd.writeUInt16LE(entries.length, 10);
  eocd.writeUInt32LE(centralSize, 12);
  eocd.writeUInt32LE(offset, 16);
  eocd.writeUInt16LE(0, 20);

  return Buffer.concat([...localParts, ...centralParts, eocd]);
}

function escapeXml(str) {
  return str
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&apos;");
}

function wrapXhtml(title, bodyHtml) {
  return `<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops" xml:lang="pt-BR" lang="pt-BR">
<head>
  <meta charset="utf-8" />
  <title>${escapeXml(title)}</title>
  <link rel="stylesheet" type="text/css" href="styles/book.css" />
</head>
<body>
${bodyHtml}
</body>
</html>`;
}

const BOOK_CSS = `
body {
  font-family: Georgia, "Times New Roman", serif;
  line-height: 1.68;
  margin: 4% 6%;
  font-size: 1.02em;
}
h1, h2, h3 {
  font-family: system-ui, -apple-system, "Segoe UI", sans-serif;
  line-height: 1.25;
  margin-top: 1.5em;
  margin-bottom: 0.6em;
}
h1 {
  font-size: 1.65em;
  border-bottom: 2px solid #2b8a78;
  padding-bottom: 0.35em;
}
h2 {
  font-size: 1.25em;
  color: #1d6b5d;
}
p {
  margin: 0.75em 0;
  text-align: justify;
}
ul, ol {
  margin: 0.8em 0 1em 1.4em;
}
li {
  margin: 0.45em 0;
}
.callout {
  border-left: 4px solid #2b8a78;
  background-color: rgba(43, 138, 120, 0.08);
  padding: 0.9em 1.1em;
  margin: 1.2em 0;
  border-radius: 6px;
}
.callout strong {
  display: block;
  margin-bottom: 0.35em;
  font-family: system-ui, -apple-system, sans-serif;
}
.chapter-tag {
  font-family: system-ui, -apple-system, sans-serif;
  font-size: 0.8em;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: #2b8a78;
  font-weight: bold;
}
table {
  width: 100%;
  border-collapse: collapse;
  margin: 1.2em 0;
  font-size: 0.92em;
}
th, td {
  border: 1px solid #8aa0b8;
  padding: 0.55em 0.7em;
  text-align: left;
}
th {
  background-color: rgba(43, 138, 120, 0.15);
  font-family: system-ui, -apple-system, sans-serif;
}
.cover-page {
  text-align: center;
  margin: 0;
  padding: 0;
}
.cover-page img {
  max-width: 100%;
  max-height: 98vh;
  height: auto;
}
.cta-box {
  border: 2px solid #2b8a78;
  padding: 1.1em;
  border-radius: 10px;
  margin: 1.4em 0;
}
code {
  font-family: monospace;
  font-size: 0.9em;
}
`;

const CHAPTERS = [
  {
    id: "ch01",
    file: "ch01.xhtml",
    title: "Prefácio e Capítulo 1 — A Anatomia Invisível dos Gigabytes Perdidos",
    body: `
      <div class="chapter-tag">Prefácio &amp; Fundamentos</div>
      <h1>Capítulo 1: A Anatomia Invisível dos Gigabytes Perdidos</h1>
      <p>Todo usuário Android já enfrentou o mesmo roteiro frustrante: você tenta gravar um vídeo rápido em família, baixar um anexo de trabalho ou atualizar um aplicativo essencial, e o sistema trava com o alerta vermelho: <strong>"Armazenamento quase cheio"</strong>. A reação instintiva da maioria das pessoas é sair apagando cinco ou dez fotos aleatórias da galeria — e, dez minutos depois, perceber que o aviso voltou exatamente igual.</p>
      <p>Isso acontece porque o problema raramente está nas últimas dez fotos que você tirou hoje. Em aparelhos Android modernos com 64 GB, 128 GB ou até 256 GB, a saturação real do disco vem da combinação silenciosa de quatro forças acumulativas: vídeos curtos gravados em 4K/60fps, cópias idênticas recebidas dezenas de vezes em grupos de mensagens, notas de voz antigas sem indexação visual e arquivos movidos para a lixeira do sistema que ainda não foram removidos de verdade.</p>

      <h2>1.1 Para onde vão os seus primeiros 30 GB antes mesmo da primeira foto</h2>
      <p>Ao ligar um smartphone novo de 128 GB, você não recebe 128 GB úteis. A conversão decimal de fabricantes (1 GB = 1.000³ bytes) contra a leitura binária do sistema operacional (1 GiB = 1.024³ bytes) já reduz a capacidade exibida para cerca de 119 GiB. Somando as partições do sistema Android, partição A/B de atualização contínua e aplicativos pré-instalados, um aparelho novo frequentemente inicia com 18 GB a 26 GB já ocupados.</p>
      <p>É sobre a fatia restante que suas fotos, áudios, downloads e aplicativos competem diariamente. Quando o espaço livre cai abaixo de 10%, a própria controladora de memória flash UFS do aparelho começa a sofrer queda de desempenho em gravação e abertura de apps, criando a sensação de que o telefone ficou "lento e travado".</p>

      <h2>1.2 O papel do Android MediaStore</h2>
      <p>A partir do Android 10 (API 29), o Google reforçou um modelo chamado <strong>Scoped Storage</strong>. Em vez de permitir que qualquer aplicativo de terceiros vasculhe todas as pastas sensíveis do usuário indiscriminadamente, o sistema centraliza fotos, vídeos e áudios compartilhados no índice oficial <code>MediaStore</code>.</p>
      <div class="callout">
        <strong>Regra de Segurança Prática</strong>
        <p>Sempre desconfie de utilitários comuns que pedem permissão irrestrita de "Acesso a todos os arquivos" (<code>MANAGE_EXTERNAL_STORAGE</code>) apenas para listar imagens e vídeos da sua galeria. Ferramentas bem construídas utilizam o MediaStore granular e pedem autorização explícita ao sistema Android na hora de enviar itens à lixeira ou apagar permanentemente.</p>
      </div>

      <h2>1.3 Lixeira não é espaço livre imediato</h2>
      <p>Outro ponto que confunde milhões de usuários desde o Android 11: quando você move uma gravação de 800 MB para a Lixeira na galeria padrão, aqueles bytes continuam fisicamente ocupados no armazenamento por até 30 dias aguardando expiração ou esvaziamento manual. Só considere espaço verdadeiramente recuperado após a exclusão definitiva — com prévia conferência para evitar perdas definitivas.</p>
    `,
  },
  {
    id: "ch02",
    file: "ch02.xhtml",
    title: "Capítulo 2 — O Fenômeno WhatsApp e Mensageiros Sem Perder Memórias",
    body: `
      <div class="chapter-tag">Capítulo 2 • Mensageiros &amp; Mídia Recebida</div>
      <h1>O Fenômeno WhatsApp: Por Que Seu Telefone Multiplica Vídeos Sozinho</h1>
      <p>Imagine o seguinte cenário real em qualquer grupo ativo de trabalho, condomínio ou família: alguém publica um vídeo de 42 MB. Cinco minutos depois, outra pessoa encaminha o mesmo arquivo para outro grupo onde você também está. Ao longo de um mês, vídeos divertidos, panfletos em vídeo, áudios longos e fotos repetidas entram na fila de download automático sem que você perceba o efeito composto.</p>

      <h2>2.1 O impacto matemático dos arquivos acima de 10 MB</h2>
      <p>Uma foto JPEG comum de celular costuma pesar entre 2 MB e 4 MB. Já um único vídeo de dois minutos encaminhado em resolução média pode consumir 60 MB a 120 MB — o equivalente a trinta ou quarenta fotos inteiras. Tentar liberar 10 GB apagando apenas imagens pequenas exige revisar milhares de momentos afetivos, enquanto revisar os 30 maiores vídeos (>10 MB) pode recuperar vários gigabytes em menos de cinco minutos.</p>

      <h2>2.2 Áudios antigos e gravações esquecidas (+30 dias)</h2>
      <p>Conversas antigas deixam centenas ou milhares de arquivos de áudio acumulados em diretórios de mídia indexados pelo sistema. Individualmente, uma nota de voz pesa apenas 50 KB a 400 KB. Porém, em usuários com dois ou três anos de histórico no mesmo aparelho ou restauração de backup, a soma de áudios + mídias antigas com mais de 30, 90 ou 180 dias atinge gigabytes preciosos.</p>
      <ul>
        <li><strong>Desative o download automático em redes móveis e Wi-Fi para grupos ruidosos:</strong> você continua vendo a prévia borrada e só baixa o que realmente quer assistir.</li>
        <li><strong>Revise arquivos grandes periodicamente:</strong> classifique vídeos por tamanho descendente antes de sair deletando memórias pequenas.</li>
        <li><strong>Use extração de áudio quando só precisar da fala:</strong> aulas, palestras ou reuniões gravadas em vídeo de 500 MB podem virar uma faixa leve em áudio <code>M4A</code> antes de remover o vídeo bruto.</li>
      </ul>
    `,
  },
  {
    id: "ch03",
    file: "ch03.xhtml",
    title: "Capítulo 3 — O Mito dos Apps 'Turbinadores de RAM' e Como Identificar Golpes",
    body: `
      <div class="chapter-tag">Capítulo 3 • Segurança &amp; Engenharia Android</div>
      <h1>O Mito dos Apps "Turbinadores de RAM" e Limpadores Enganosos</h1>
      <p>Durante anos, lojas de aplicativos foram inundadas por promessas chamativas: botões vermelhos piscando com "CPU a 92 graus! Resfriar agora!", "Limpe 40 GB de cache oculto em 1 toque!" ou "Acelere sua memória RAM em 300%!". Na engenharia moderna do Android, praticamente todas essas afirmações são tecnicamente falsas ou proibidas pelas políticas atuais do Google Play.</p>

      <h2>3.1 Por que matar processos da RAM piora a bateria e a velocidade</h2>
      <p>O Android gerencia memória RAM com base no kernel Linux seguindo a regra de ouro: <strong>memória RAM livre é memória desperdiçada</strong>. O sistema mantém os aplicativos mais usados pré-carregados na RAM em estado suspenso (consumindo zero bateria adicional) exatamente para que abram instantaneamente quando você toca no ícone. Quando um utilitário enganoso força o encerramento em massa desses apps, o Android precisa gastar ciclos pesados de CPU e bateria para reiniciá-los do zero segundos depois.</p>

      <h2>3.2 Sinais claros de um utilitário confiável vs. app enganoso</h2>
      <table>
        <thead>
          <tr><th>Critério Técnico</th><th>App Enganoso / Risco Alto</th><th>Abordagem Transparente (Midiastore)</th></tr>
        </thead>
        <tbody>
          <tr><td>Permissão de arquivos</td><td>Pede <code>MANAGE_EXTERNAL_STORAGE</code> para varrer tudo sem motivo</td><td>Usa Android <code>MediaStore</code> e acesso granular por mídia</td></tr>
          <tr><td>Exclusão</td><td>Promete apagar "automaticamente em 1 clique sem perguntar"</td><td>Exige revisão manual + confirmação nativa oficial do Android</td></tr>
          <tr><td>Detecção de duplicatas</td><td>Compara apenas tamanho de arquivo ou nome parecido</td><td>Calcula hash criptográfico <strong>SHA-256</strong> byte a byte e protege 1 original</td></tr>
          <tr><td>Privacidade</td><td>Envia miniaturas ou uso de apps para servidores de anúncios</td><td>Processamento 100% local no aparelho, sem upload de mídias</td></tr>
        </tbody>
      </table>
    `,
  },
  {
    id: "ch04",
    file: "ch04.xhtml",
    title: "Capítulo 4 — A Ciência das Duplicatas Exatas (SHA-256 Byte a Byte)",
    body: `
      <div class="chapter-tag">Capítulo 4 • Precisão Criptográfica</div>
      <h1>SHA-256 vs. Olhômetro: Por Que Tamanho Igual Nunca Prova Cópia Idêntica</h1>
      <p>Um dos erros mais perigosos cometidos por ferramentas improvisadas é considerar dois arquivos "duplicados" apenas porque possuem o mesmo tamanho em bytes. Na prática, duas fotos tiradas em sequência com o mesmo enquadramento ou dois PDFs/áudios com tamanho parecido podem ter conteúdo completamente diferente.</p>

      <h2>4.1 Como funciona o hash criptográfico SHA-256</h2>
      <p>O algoritmo <strong>SHA-256</strong> lê os bytes reais do arquivo do início ao fim e produz uma assinatura digital única de 256 bits (64 caracteres hexadecimais). Se você alterar um único bit de cor em uma fotografia de 12 megapixels, toda a assinatura SHA-256 resultante muda completamente.</p>
      <p>Por outro lado, se você recebeu a mesma fotografia em três grupos diferentes com nomes distintos — como <code>IMG_20260410.jpg</code>, <code>IMG-WA0042.jpg</code> e <code>Copia (1).jpg</code> — o conteúdo binário é rigorosamente idêntico. A verificação por tamanho funciona apenas como filtro rápido inicial: apenas grupos com o mesmo tamanho entram na fila de leitura streaming SHA-256, evitando consumo desnecessário de bateria.</p>

      <div class="callout">
        <strong>A Regra Inviolável de Preservação</strong>
        <p>Mesmo após confirmar duas ou cinco cópias com o mesmo SHA-256, um sistema seguro <strong>nunca</strong> marca todas as cópias para exclusão. Pelo menos uma versão — preferencialmente a original na câmera ou pasta principal — permanece desmarcada e protegida durante a seleção.</p>
      </div>
    `,
  },
  {
    id: "ch05",
    file: "ch05.xhtml",
    title: "Capítulo 5 — Privacidade Invisível: O Perigo dos Metadados EXIF e GPS",
    body: `
      <div class="chapter-tag">Capítulo 5 • Segurança Pessoal</div>
      <h1>O Mapa Secreto Dentro da Sua Foto: Metadados EXIF e GPS</h1>
      <p>Quando você fotografa um produto para vender em marketplace, registra um documento para enviar por e-mail ou compartilha a foto do seu animal de estimação em um grupo aberto, quase sempre envia muito mais do que pixels visíveis. O cabeçalho <strong>EXIF</strong> embutido no arquivo JPEG pode carregar:</p>
      <ul>
        <li>Coordenadas exatas de latitude e longitude (GPS) da sua residência ou trabalho;</li>
        <li>Modelo exato do telefone e versão de sistema;</li>
        <li>Data e hora exatas com precisão de segundos;</li>
        <li>Configurações de lente, ISO e orientação.</li>
      </ul>

      <h2>5.1 Como higienizar fotos antes de compartilhar</h2>
      <p>Muitas plataformas sociais grandes removem tags EXIF na compressão do upload, porém o envio como "Documento" em mensageiros, anexos de e-mail, links em nuvem ou anúncios em sites menores frequentemente preserva os bytes originais intactos com a localização GPS embutida.</p>
      <p>A solução recomendada é passar imagens destinadas a terceiros por uma ferramenta local de <strong>limpeza de metadados EXIF</strong> (como a aba de privacidade do Estúdio de Mídia do Midiastore ou nossa ferramenta gratuita no navegador), gerando uma nova cópia visual limpa sem coordenadas nem dados do dispositivo.</p>
    `,
  },
  {
    id: "ch06",
    file: "ch06.xhtml",
    title: "Capítulo 6 — Estúdio Portátil: Áudio M4A, Fotos para PDF A4 e QR Codes",
    body: `
      <div class="chapter-tag">Capítulo 6 • Produtividade Sem Bloatware</div>
      <h1>Um Único Utilitário Leve em Vez de Cinco Apps Cheios de Anúncios</h1>
      <p>Ironicamente, quem sofre com falta de espaço acaba instalando quatro ou cinco aplicativos separados para resolver pequenas tarefas diárias: um editor de fotos, um gerador de QR Code cheio de pop-ups, um conversor de comprovantes para PDF e um extrator de áudio que envia tudo para servidores externos.</p>

      <h2>6.1 Os 5 fluxos de produtividade offline</h2>
      <ol>
        <li><strong>Extração de vídeo para áudio M4A local:</strong> ideal para guardar apenas a explicação de uma videoaula de 300 MB em um arquivo M4A de 8 MB no aparelho.</li>
        <li><strong>Fotos para PDF A4 paginado:</strong> digitalize recibos, contratos ou páginas de caderno com a câmera, combine em um único PDF A4 legível diretamente na fila do Android sem depender de upload na nuvem.</li>
        <li><strong>Filtros rápidos com preservação do original:</strong> ajuste brilho, contraste ou preto e branco localmente com salvamento limpo.</li>
        <li><strong>Gerador de QR Code instantâneo (PIX, Wi-Fi e Links):</strong> gere códigos offline no próprio aparelho para visitas, vendas ou balcão.</li>
        <li><strong>Removedor EXIF/GPS antes do envio:</strong> citado no Capítulo 5 para proteger sua localização real.</li>
      </ol>
    `,
  },
  {
    id: "ch07",
    file: "ch07.xhtml",
    title: "Capítulo 7 — O Ritual Semanal de 3 Minutos Anti-Lotação",
    body: `
      <div class="chapter-tag">Capítulo 7 • Método Contínuo</div>
      <h1>O Ritual de Domingo em 3 Minutos Para Manter 20%+ de Espaço Livre</h1>
      <p>Não espere o celular travar no meio de uma viagem ou pagamento importante. Aplicando uma rotina simples uma vez por semana, seu Android permanece ágil durante anos:</p>

      <h2>Checklist Executivo de 3 Minutos</h2>
      <ul>
        <li><strong>Minuto 1 — Arquivos Gigantes (&gt;10 MB):</strong> liste os maiores vídeos recentes, assista rapidamente aos que não têm mais valor (tutoriais antigos, gravações de tela temporárias) e envie para revisão/Lixeira com confirmação oficial do Android.</li>
        <li><strong>Minuto 2 — Duplicatas Exatas SHA-256:</strong> execute uma varredura nas mídias acessíveis e revise grupos onde exatamente o mesmo vídeo ou foto foi baixado duas ou três vezes. Confira se 1 original ficou preservado e limpe apenas as cópias sobressalentes.</li>
        <li><strong>Minuto 3 — Esvaziamento Consciente da Lixeira &amp; Backup:</strong> fotos realmente insubstituíveis merecem cópia em HD externo, computador ou nuvem privada confiável antes da exclusão permanente.</li>
      </ul>

      <h2>Qual é a margem saudável no Android?</h2>
      <p>Mantenha sempre entre <strong>15% e 20% da capacidade total livre</strong> (por exemplo, ~19 GB a 25 GB em um aparelho de 128 GB). Abaixo desse patamar, atualizações de sistema, gravações longas e indexação de cache do WhatsApp começam a gerar gargalos visíveis.</p>
    `,
  },
  {
    id: "ch08",
    file: "ch08.xhtml",
    title: "Capítulo 8 — Ecossistema Midiastore, Ferramentas Gratuitas e Próximos Passos",
    body: `
      <div class="chapter-tag">Capítulo 8 • Prática Real com o Midiastore</div>
      <h1>Colocando Tudo em Prática com Segurança Total</h1>
      <p>Todo método técnico descrito neste livro guiou a arquitetura do aplicativo <strong>Midiastore</strong> (<code>com.rdf112.midiastore</code>) para Android 10 até Android 16: Scoped Storage sem <code>MANAGE_EXTERNAL_STORAGE</code>, hash criptográfico SHA-256 em streaming para duplicatas exatas, revisão guiada de vídeos grandes (&gt;10 MB) e mídias antigas (+30 dias), remoção de metadados EXIF/GPS e Estúdio de Mídia 100% no dispositivo.</p>

      <div class="cta-box">
        <strong>Acesse Agora Suas Ferramentas Oficiais e Lista VIP de Lançamento</strong>
        <p>Você pode testar gratuitamente hoje mesmo o diagnóstico de espaço estimado, utilizar o Estúdio Web direto no navegador ou garantir sua vaga no lançamento oficial para Android na Google Play:</p>
        <ul>
          <li><strong>Página Oficial e Acesso Antecipado Google Play:</strong> <a href="${escapeXml(CTA_URL)}">${escapeXml(CTA_URL)}</a></li>
          <li><strong>Simulador Gratuito de Espaço Ocupado:</strong> <a href="${escapeXml(DIAG_URL)}">${escapeXml(DIAG_URL)}</a></li>
          <li><strong>Estúdio Online Gratuito (EXIF, Filtros, QR, PDF):</strong> <a href="${escapeXml(TOOLS_URL)}">${escapeXml(TOOLS_URL)}</a></li>
        </ul>
      </div>

      <h2>Agradecimento</h2>
      <p>Obrigado por investir na saúde digital e na privacidade dos seus dados. Compartilhe os aprendizados deste guia com familiares que vivem recebendo a mensagem "armazenamento cheio" — muitas vezes, dez minutos de revisão consciente evitam a compra prematura de um celular novo apenas por falta de espaço.</p>
    `,
  },
];

function generateCoverPng() {
  fs.mkdirSync(ebookDir, { recursive: true });
  execFileSync(
    "powershell.exe",
    [
      "-Sta",
      "-NoProfile",
      "-Command",
      "& ([ScriptBlock]::Create([System.IO.File]::ReadAllText($env:MIDI_BOOK_PS1, [System.Text.Encoding]::UTF8)))",
    ],
    {
      env: {
        ...process.env,
        MIDI_BOOK_PS1: coverPs1,
        MIDI_BOOK_COVER_OUT: coverPngPath,
      },
      stdio: "inherit",
    }
  );
}

function main() {
  generateCoverPng();
  const coverBytes = fs.readFileSync(coverPngPath);
  const w = coverBytes.readUInt32BE(16);
  const h = coverBytes.readUInt32BE(20);
  if (w !== 1600 || h !== 2560) {
    throw new Error(`Cover dimensions mismatch: expected 1600x2560, got ${w}x${h}`);
  }
  console.log(`✅ Capa verificada: ${w}x${h} px (${(coverBytes.length / 1024).toFixed(1)} KB)`);

  const containerXml = `<?xml version="1.0" encoding="UTF-8"?>
<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
  <rootfiles>
    <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml" />
  </rootfiles>
</container>`;

  const coverXhtml = wrapXhtml(
    "Capa",
    `<div class="cover-page"><img src="images/cover.png" alt="${escapeXml(BOOK_META.title)}" /></div>`
  );

  const navXhtml = `<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops" xml:lang="pt-BR" lang="pt-BR">
<head>
  <meta charset="utf-8" />
  <title>Sumário</title>
  <link rel="stylesheet" type="text/css" href="styles/book.css" />
</head>
<body>
  <nav epub:type="toc" id="toc">
    <h1>Sumário</h1>
    <ol>
      <li><a href="cover.xhtml">Capa</a></li>
${CHAPTERS.map((c) => `      <li><a href="${c.file}">${escapeXml(c.title)}</a></li>`).join("\n")}
    </ol>
  </nav>
</body>
</html>`;

  const tocNcx = `<?xml version="1.0" encoding="UTF-8"?>
<ncx xmlns="http://www.daisy.org/z3986/2005/ncx/" version="2005-1">
  <head>
    <meta name="dtb:uid" content="${BOOK_META.id}" />
    <meta name="dtb:depth" content="1" />
    <meta name="dtb:totalPageCount" content="0" />
    <meta name="dtb:maxPageNumber" content="0" />
  </head>
  <docTitle><text>${escapeXml(BOOK_META.title)}</text></docTitle>
  <navMap>
    <navPoint id="nav-cover" playOrder="1">
      <navLabel><text>Capa</text></navLabel>
      <content src="cover.xhtml" />
    </navPoint>
${CHAPTERS.map(
  (c, idx) => `    <navPoint id="nav-${c.id}" playOrder="${idx + 2}">
      <navLabel><text>${escapeXml(c.title)}</text></navLabel>
      <content src="${c.file}" />
    </navPoint>`
).join("\n")}
  </navMap>
</ncx>`;

  const contentOpf = `<?xml version="1.0" encoding="UTF-8"?>
<package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="bookid" xml:lang="${BOOK_META.lang}">
  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
    <dc:identifier id="bookid">${BOOK_META.id}</dc:identifier>
    <dc:title>${escapeXml(BOOK_META.title)}</dc:title>
    <dc:creator>${escapeXml(BOOK_META.author)}</dc:creator>
    <dc:publisher>${escapeXml(BOOK_META.publisher)}</dc:publisher>
    <dc:language>${BOOK_META.lang}</dc:language>
    <dc:description>${escapeXml(BOOK_META.subtitle)}</dc:description>
    <meta property="dcterms:modified">${BOOK_META.modified}</meta>
    <meta name="cover" content="cover-image" />
  </metadata>
  <manifest>
    <item id="css" href="styles/book.css" media-type="text/css" />
    <item id="cover-image" href="images/cover.png" media-type="image/png" properties="cover-image" />
    <item id="cover" href="cover.xhtml" media-type="application/xhtml+xml" />
    <item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav" />
    <item id="ncx" href="toc.ncx" media-type="application/x-dtbncx+xml" />
${CHAPTERS.map((c) => `    <item id="${c.id}" href="${c.file}" media-type="application/xhtml+xml" />`).join("\n")}
  </manifest>
  <spine toc="ncx">
    <itemref idref="cover" />
    <itemref idref="nav" />
${CHAPTERS.map((c) => `    <itemref idref="${c.id}" />`).join("\n")}
  </spine>
</package>`;

  const entries = [
    { name: "mimetype", data: "application/epub+zip", store: true },
    { name: "META-INF/container.xml", data: containerXml },
    { name: "OEBPS/content.opf", data: contentOpf },
    { name: "OEBPS/nav.xhtml", data: navXhtml },
    { name: "OEBPS/toc.ncx", data: tocNcx },
    { name: "OEBPS/styles/book.css", data: BOOK_CSS },
    { name: "OEBPS/images/cover.png", data: coverBytes, store: true },
    { name: "OEBPS/cover.xhtml", data: coverXhtml },
    ...CHAPTERS.map((c) => ({
      name: `OEBPS/${c.file}`,
      data: wrapXhtml(c.title, c.body),
    })),
  ];

  const epubBuf = buildEpubZip(entries);
  fs.writeFileSync(epubOutPath, epubBuf);
  console.log(`✅ EPUB 3.0 gerado com sucesso: ${epubOutPath} (${(epubBuf.length / 1024).toFixed(1)} KB, ${CHAPTERS.length} capitulos)`);
}

main();
