#!/usr/bin/env node
/**
 * upgrade-web-funnel-13-locales.mjs
 *
 * 1. Atualiza as 13 páginas de idiomas (docs/index.html + docs/{en,es,ar,ru,hi,id,bn,ko,de,fr,ja,zh}/index.html)
 *    para o padrão Preto OLED v2.7.6 (#000000 / #1C1C1E / #0A84FF), 100% Limpador Puro (compliance Google Ads),
 *    removendo menções antigas a jogos/estúdio/relógio/mágica.
 * 2. Injeta em docs/index.html e docs/en/index.html:
 *    - Tabela Comparativa de Alta Conversão (Midiastore 5,9 MB vs Limpadores Comuns)
 *    - Seção de Psicologia de Vendas & Planos Transparentes (Grátis vs Anual -56% R$ 0,22/dia vs Vitalício)
 *    - FAQ Visível em Acordeão (<details>) + Schema.org FAQPage rico com 5 perguntas de alta busca no Google.
 * 3. Atualiza docs/sitemap.xml com <lastmod>2026-11-21</lastmod> para todas as páginas e guias pSEO.
 */

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");
const docsDir = path.join(projectRoot, "docs");

const LOCALE_DIRS = ["", "en", "es", "ar", "ru", "hi", "id", "bn", "ko", "de", "fr", "ja", "zh"];

const LOCALE_META = {
  "": {
    lang: "pt-BR",
    title: "Midiastore — Limpador de Armazenamento Android Leve (5,9 MB) & Duplicatas SHA-256",
    desc: "Libere espaço no Android em 2 toques com revisão visual segura: vídeos grandes (>10 MB), mídias antigas (+30 dias), WhatsApp e duplicatas exatas SHA-256. Apenas 5,9 MB e 100% no aparelho.",
    keywords: "limpador de armazenamento android, limpar fotos duplicadas sha-256, liberar espaço celular cheio, arquivos grandes android, limpar vídeos pesados whatsapp, limpador leve 5.9 mb, scoped storage android, midiastore",
  },
  en: {
    lang: "en-US",
    title: "Midiastore — Fast 5.9 MB Android Storage Cleaner & Exact SHA-256 Duplicate Finder",
    desc: "Free up Android storage in 2 taps with visual thumbnail review: large videos (>10 MB), old media (+30 days), WhatsApp clutter, and exact SHA-256 duplicates. 100% on-device privacy.",
    keywords: "android storage cleaner, duplicate photo finder sha-256, free up space android, large video cleaner, whatsapp storage cleaner, lightweight cleaner 5.9 mb, scoped storage cleaner, midiastore",
  },
  es: {
    lang: "es-ES",
    title: "Midiastore — Limpiador de Almacenamiento Android Ligero (5,9 MB) y Duplicados SHA-256",
    desc: "Libera espacio en Android en 2 toques con revisión visual segura: videos grandes (>10 MB), archivos antiguos (+30 días) y duplicados exactos SHA-256. 100% privado en tu móvil.",
    keywords: "limpiador de almacenamiento android, fotos duplicadas sha-256, liberar espacio android, limpiar videos grandes whatsapp, limpiador ligero 5.9 mb, midiastore",
  },
  ar: {
    lang: "ar",
    title: "Midiastore — منظف مساحة تخزين أندرويد خفيف (5.9 MB) وكاشف التكرار SHA-256",
    desc: "حرر مساحة هاتفك الأندرويد بخطوتين بأمان تام: الفيديوهات الكبيرة (>10 MB)، الوسائط القديمة (+30 يوماً) والملفات المكررة بدقة SHA-256. خصوصية 100% على جهازك.",
    keywords: "منظف مساحة أندرويد, حذف الصور المكررة sha-256, تفريغ مساحة الهاتف, تنظيف فيديوهات واتساب, منظف خفيف 5.9 ميجا, midiastore",
  },
  ru: {
    lang: "ru-RU",
    title: "Midiastore — Легкий Очиститель Памяти Android (5,9 МБ) и Поиск Дубликатов SHA-256",
    desc: "Освободите память Android в 2 касания: крупные видео (>10 МБ), старые медиафайлы (+30 дней) и точные дубликаты SHA-256 с защитой оригинала. 100% локально на устройстве.",
    keywords: "очистка памяти android, поиск дубликатов фото sha-256, освободить место на телефоне, очистка видео whatsapp, легкий клинер 5.9 мб, midiastore",
  },
  hi: {
    lang: "hi-IN",
    title: "Midiastore — तेज़ 5.9 MB Android स्टोरेज क्लीनर और SHA-256 डुप्लीकेट फ़ाइंडर",
    desc: "2 टैप में Android स्टोरेज खाली करें: बड़े वीडियो (>10 MB), पुराना मीडिया (+30 दिन), WhatsApp फ़ाइलें और सटीक SHA-256 डुप्लीकेट। 100% ऑन-डिवाइस प्राइवेसी।",
    keywords: "android storage cleaner hindi, sha-256 duplicate photo remover, phone memory cleaner 5.9 mb, whatsapp video cleaner, midiastore",
  },
  id: {
    lang: "id",
    title: "Midiastore — Pembersih Penyimpanan Android Ringan (5,9 MB) & Duplikat SHA-256",
    desc: "Bebaskan memori Android dalam 2 ketukan: video besar (>10 MB), media lama (+30 hari), file WhatsApp, dan duplikat akurat SHA-256. 100% privat di perangkat Anda.",
    keywords: "pembersih memori android, hapus foto duplikat sha-256, bersihkan penyimpanan penuh, pembersih ringan 5.9 mb, midiastore",
  },
  bn: {
    lang: "bn-BD",
    title: "Midiastore — হালকা 5.9 MB Android স্টোরেজ ক্লিনার ও SHA-256 ডুপ্লিকেট ফাইন্ডার",
    desc: "২ ট্যাপে নিরাপদে Android স্টোরেজ খালি করুন: বড় ভিডিও (>10 MB), পুরনো মিডিয়া (+30 দিন) এবং সঠিক SHA-256 ডুপ্লিকেট ফাইল। ১০০% অন-ডিভাইস প্রাইভেসি।",
    keywords: "android storage cleaner bangla, duplicate photo finder sha-256, free up phone memory 5.9 mb, midiastore",
  },
  ko: {
    lang: "ko-KR",
    title: "Midiastore — 초경량 5.9MB Android 저장공간 정리 및 SHA-256 중복 파일 검사",
    desc: "2번의 탭으로 안전하게 Android 저장공간을 확보하세요: 대용량 동영상(>10MB), 오래된 미디어(+30일), 정밀 SHA-256 중복 파일 정리. 100% 기기 내 처리.",
    keywords: "안드로이드 저장공간 정리, 중복 사진 제거 sha-256, 휴대폰 용량 늘리기, 초경량 클리너 5.9mb, midiastore",
  },
  de: {
    lang: "de-DE",
    title: "Midiastore — Schlanker 5,9 MB Android Speicher-Reiniger & SHA-256 Duplikatfinder",
    desc: "Android-Speicher in 2 Tipps sicher bereinigen: große Videos (>10 MB), alte Medien (+30 Tage) und exakte SHA-256-Duplikate mit Originalschutz. 100 % lokal auf Ihrem Gerät.",
    keywords: "android speicher береinigen, doppelte fotos löschen sha-256, speicherplatz freigeben android, schlanker cleaner 5.9 mb, midiastore",
  },
  fr: {
    lang: "fr-FR",
    title: "Midiastore — Nettoyeur de Stockage Android Léger (5,9 Mo) & Doublons SHA-256",
    desc: "Libérez de l'espace Android en 2 gestes avec aperçu réel : grosses vidéos (>10 Mo), médias anciens (+30 jours) et doublons exacts SHA-256. 100 % privé sur votre téléphone.",
    keywords: "nettoyeur stockage android, supprimer photos doubles sha-256, libérer espace android, nettoyeur léger 5.9 mo, midiastore",
  },
  ja: {
    lang: "ja-JP",
    title: "Midiastore — 軽量5.9MB Androidストレージ整理＆SHA-256重複ファイル検出",
    desc: "実サムネイル確認と2タップでAndroidの空き容量を安全に確保：大容量動画（10MB以上）、古いメディア（30日以上）、厳密なSHA-256重複検出。100%端末内処理。",
    keywords: "Android ストレージ 整理, 重複写真 削除 SHA-256, 容量不足 解消 アプリ 軽量 5.9MB, midiastore",
  },
  zh: {
    lang: "zh-CN",
    title: "Midiastore — 5.9MB 轻量 Android 存储空间清理与 SHA-256 精准查重工具",
    desc: "2步快速核对真实缩略图，安全释放 Android 手机空间：超大视频（>10 MB）、陈旧媒体（+30天）及字节级 SHA-256 重复文件清理。100% 本机隐私处理。",
    keywords: "Android 手机清理, SHA-256 重复照片清理, 释放存储空间, 5.9MB 轻量清理工具, midiastore",
  },
};

const EXTRA_CSS = `
    .compare-section{margin:40px 0;padding:28px 24px;border:1px solid var(--line);border-radius:22px;background:#0B0B0D}
    .compare-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(280px,1fr));gap:16px;margin-top:18px}
    .compare-col{padding:20px;border-radius:16px;border:1px solid var(--line);background:var(--card)}
    .compare-col.highlight{border-color:var(--mint);box-shadow:0 0 28px rgba(10,132,255,0.16)}
    .compare-col h3{margin:0 0 12px;font-size:1.15rem;display:flex;align-items:center;gap:8px}
    .compare-list{list-style:none;padding:0;margin:0;display:flex;flex-direction:column;gap:10px;font-size:0.92rem;color:var(--muted)}
    .compare-list li strong{color:var(--white)}
    .pricing-section{margin:40px 0}
    .pricing-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(250px,1fr));gap:16px;margin-top:18px}
    .price-card{background:var(--card);border:1px solid var(--line);border-radius:20px;padding:24px;display:flex;flex-direction:column;justify-content:space-between;gap:16px;position:relative}
    .price-card.featured{border:2px solid var(--mint);box-shadow:0 12px 36px rgba(10,132,255,0.22)}
    .price-ribbon{display:inline-block;background:#163322;color:#30D158;border:1px solid #30D158;font-size:0.75rem;font-weight:800;padding:4px 10px;border-radius:999px;letter-spacing:0.04em;text-transform:uppercase;width:fit-content}
    .price-title{font-size:1.2rem;font-weight:800;margin:6px 0 4px;color:var(--white)}
    .price-amount{font-size:2rem;font-weight:900;color:var(--white);letter-spacing:-.03em}
    .price-amount small{font-size:0.9rem;color:var(--muted);font-weight:600}
    .price-daily{font-size:0.84rem;color:#30D158;font-weight:700;margin-top:2px}
    .price-features{list-style:none;padding:0;margin:12px 0 0;display:flex;flex-direction:column;gap:8px;font-size:0.88rem;color:var(--muted)}
    .faq-section{margin:40px 0}
    .faq-list{display:flex;flex-direction:column;gap:10px;margin-top:16px}
    .faq-item{background:var(--card);border:1px solid var(--line);border-radius:14px;padding:16px 20px}
    .faq-item summary{font-weight:800;font-size:1rem;color:var(--white);cursor:pointer;list-style:none;display:flex;justify-content:space-between;align-items:center}
    .faq-item summary::-webkit-details-marker{display:none}
    .faq-item p{margin:10px 0 0;color:var(--muted);font-size:0.92rem;line-height:1.55}
`;

const PT_CONVERSION_SECTIONS = `
    <section class="compare-section" aria-label="Comparativo Técnico Midiastore vs Limpadores Comuns">
      <div class="tester-badge">⚡ Por que o Midiastore v2.7.6 converte e retém mais usuários</div>
      <h2 class="section-title" style="margin-top:4px;">Limpador de Armazenamento Puro vs. Limpadores Tradicionais</h2>
      <div class="compare-grid">
        <div class="compare-col">
          <h3>❌ Limpadores Comuns da Loja</h3>
          <ul class="compare-list">
            <li>• <strong>Pesados (80 MB a 150 MB):</strong> não instalam quando o celular já está lotado.</li>
            <li>• <strong>Anúncios invasivos em tela cheia:</strong> interrompem cada clique antes de limpar.</li>
            <li>• <strong>Exclusão arriscada:</strong> agrupam fotos parecidas e apagam boas lembranças sem mostrar.</li>
            <li>• <strong>Permissões excessivas:</strong> pedem acesso total irrestrito aos arquivos do aparelho.</li>
          </ul>
        </div>
        <div class="compare-col highlight">
          <h3>✅ Midiastore v2.7.6 (Preto OLED)</h3>
          <ul class="compare-list">
            <li>• <strong>Ultraleve (Apenas 5,9 MB):</strong> baixa em 3 segundos até em celular com memória cheia.</li>
            <li>• <strong>Limpeza Real em 1 Botão (Máximo 2 Cliques):</strong> pré-analisa ao abrir e mostra miniaturas reais.</li>
            <li>• <strong>Hash Criptográfico SHA-256:</strong> prova cópias 100% idênticas byte a byte e blinda 1 original.</li>
            <li>• <strong>100% On-Device (Scoped Storage):</strong> nada vai para a nuvem e toda exclusão usa confirmação oficial do Android.</li>
          </ul>
        </div>
      </div>
    </section>

    <section class="pricing-section" id="planos" aria-label="Planos Midiastore Google Play">
      <h2 class="section-title">Comece Grátis hoje — evolua para o Pro quando quiser</h2>
      <p style="color:var(--muted);max-width:680px;margin:0 0 18px;">A limpeza essencial é 100% gratuita para sempre. Os planos Pro são processados diretamente pelo Google Play Billing com cancelamento em 1 toque.</p>
      <div class="pricing-grid">
        <div class="price-card">
          <div>
            <span class="price-ribbon" style="background:#1C1C1E;color:#98989D;border-color:#2C2C2E;">Grátis Para Sempre</span>
            <div class="price-title">Midiastore Free</div>
            <div class="price-amount">R$ 0 <small>/ sempre</small></div>
            <ul class="price-features">
              <li>✓ Limpeza rápida em 1 botão (máx. 2 toques)</li>
              <li>✓ Vídeos grandes (&gt;10 MB) e mídias +30 dias</li>
              <li>✓ Duplicatas exatas SHA-256 e WhatsApp</li>
              <li>✓ Lixeira segura Android 11+ e proteção do original</li>
            </ul>
          </div>
          <a class="button secondary" href="https://play.google.com/store/apps/details?id=com.rdf112.midiastore" data-track-cta="price_free_play" target="_blank" rel="noopener">Baixar Grátis na Google Play</a>
        </div>

        <div class="price-card featured">
          <div>
            <span class="price-ribbon">⭐ Mais Escolhido • Economize 56%</span>
            <div class="price-title">Midiastore Pro Anual</div>
            <div class="price-amount">R$ 79,99 <small>/ ano</small></div>
            <div class="price-daily">Equivale a R$ 6,66/mês (menos de R$ 0,22 por dia)</div>
            <ul class="price-features">
              <li>✓ <strong>Tudo do Plano Grátis sem limites</strong></li>
              <li>✓ <strong>Detector de Apps Esquecidos (+90 dias sem uso)</strong></li>
              <li>✓ <strong>Exportação de Relatórios Auditados de Limpeza</strong></li>
              <li>✓ <strong>Suporte Prioritário &amp; Garantia Google Play</strong></li>
            </ul>
          </div>
          <a class="button" href="https://play.google.com/store/apps/details?id=com.rdf112.midiastore" data-track-cta="price_annual_play" target="_blank" rel="noopener">Desbloquear Pro Anual (-56%)</a>
        </div>

        <div class="price-card">
          <div>
            <span class="price-ribbon" style="background:#102238;color:#0A84FF;border-color:#0A84FF;">👑 Pagamento Único</span>
            <div class="price-title">Pro Vitalício (Lifetime)</div>
            <div class="price-amount">R$ 149,99 <small>/ único</small></div>
            <div class="price-daily" style="color:#0A84FF;">Ou Plano Mensal flexível por R$ 14,99/mês</div>
            <ul class="price-features">
              <li>✓ Acesso Pro Vitalício sem nenhuma mensalidade</li>
              <li>✓ Detector de Apps sem uso (+90 dias) liberado</li>
              <li>✓ Relatórios auditados e todas as atualizações Pro</li>
              <li>✓ Vinculado à sua conta oficial da Google Play</li>
            </ul>
          </div>
          <a class="button secondary" href="https://play.google.com/store/apps/details?id=com.rdf112.midiastore" data-track-cta="price_lifetime_play" target="_blank" rel="noopener">Ver na Google Play Store</a>
        </div>
      </div>
    </section>

    <section class="faq-section" id="faq" aria-label="Perguntas Frequentes sobre Limpeza de Armazenamento Android">
      <h2 class="section-title">Perguntas Frequentes (FAQ)</h2>
      <div class="faq-list">
        <details class="faq-item" open>
          <summary>1. Como limpar o armazenamento do Android rápido em 2 cliques sem perder fotos importantes? <span>＋</span></summary>
          <p>Ao abrir o Midiastore (v2.7.6), o app já pré-analisa automaticamente os arquivos candidatos. Basta tocar no botão azul <strong>"Limpar Meu Espaço Agora"</strong> (1º clique) para ver as miniaturas reais e tocar em <strong>"Excluir definitivamente"</strong> ou <strong>"Mover para a Lixeira"</strong> (2º clique), confirmando no diálogo oficial do Android.</p>
        </details>
        <details class="faq-item">
          <summary>2. Como funciona o detector de fotos e vídeos duplicados por SHA-256? <span>＋</span></summary>
          <p>Em vez de adivinhar apenas pelo nome do arquivo, o Midiastore lê o conteúdo em streaming e calcula a assinatura criptográfica <strong>SHA-256 byte a byte</strong>. Ele só sugere arquivos 100% idênticos e mantém automaticamente 1 cópia original desmarcada e protegida.</p>
        </details>
        <details class="faq-item">
          <summary>3. Minhas fotos ou vídeos são enviados para a internet ou nuvem? <span>＋</span></summary>
          <p><strong>Nunca.</strong> Toda a análise acontece 100% localmente dentro do processador do seu celular usando a API oficial Android MediaStore (Scoped Storage, sem a permissão invasiva MANAGE_EXTERNAL_STORAGE).</p>
        </details>
        <details class="faq-item">
          <summary>4. Por que o Midiastore pesa apenas 5,9 MB? <span>＋</span></summary>
          <p>Quando o celular avisa "Armazenamento cheio", aplicativos pesados de 100 MB não conseguem nem ser baixados. O Midiastore foi construído em Kotlin nativo com otimização R8 e interface Preto OLED pura para ocupar apenas 5,9 MB e rodar rápido em Samsung Galaxy, Motorola Moto G, Xiaomi Redmi e POCO.</p>
        </details>
      </div>
    </section>
`;

const EN_CONVERSION_SECTIONS = `
    <section class="compare-section" aria-label="Technical Comparison Midiastore vs Ordinary Cleaners">
      <div class="tester-badge">⚡ Why Midiastore v2.7.6 outperforms bloated phone cleaners</div>
      <h2 class="section-title" style="margin-top:4px;">Pure Storage Cleaner vs. Traditional Cleaners</h2>
      <div class="compare-grid">
        <div class="compare-col">
          <h3>❌ Typical Store Cleaners</h3>
          <ul class="compare-list">
            <li>• <strong>Bloated (80 MB to 150 MB):</strong> fail to install when your phone is already full.</li>
            <li>• <strong>Intrusive full-screen ads:</strong> interrupt every tap before cleaning anything.</li>
            <li>• <strong>Risky auto-deletion:</strong> group similar-looking photos and delete memories blindly.</li>
            <li>• <strong>Invasive permissions:</strong> demand broad all-files access across your device.</li>
          </ul>
        </div>
        <div class="compare-col highlight">
          <h3>✅ Midiastore v2.7.6 (Pure OLED)</h3>
          <ul class="compare-list">
            <li>• <strong>Ultra-light (Only 5.9 MB):</strong> downloads in seconds even on low-storage phones.</li>
            <li>• <strong>1-Button Cleanup (Max 2 Taps):</strong> auto pre-scans on launch with real thumbnails.</li>
            <li>• <strong>Cryptographic SHA-256 Hash:</strong> proves byte-identical copies and protects 1 original.</li>
            <li>• <strong>100% On-Device (Scoped Storage):</strong> zero cloud uploads and official Android system confirmation.</li>
          </ul>
        </div>
      </div>
    </section>

    <section class="faq-section" id="faq" aria-label="Frequently Asked Questions">
      <h2 class="section-title">Frequently Asked Questions (FAQ)</h2>
      <div class="faq-list">
        <details class="faq-item" open>
          <summary>1. How does 2-tap storage cleanup work without deleting important photos? <span>＋</span></summary>
          <p>When you open Midiastore (v2.7.6), it automatically pre-scans space hogs in the background. Tap <strong>"Clean My Storage Now"</strong> (Tap 1) to inspect real thumbnails, then tap <strong>"Delete permanently"</strong> or <strong>"Move to Trash"</strong> (Tap 2) via Android's official system confirmation dialog.</p>
        </details>
        <details class="faq-item">
          <summary>2. How does exact SHA-256 duplicate detection protect my originals? <span>＋</span></summary>
          <p>Midiastore streams file contents on-device to compute a cryptographic <strong>SHA-256 hash</strong>. Only 100% byte-identical files are grouped as duplicates, and one original file is always kept unselected and safe.</p>
        </details>
        <details class="faq-item">
          <summary>3. Are my photos or videos ever uploaded to the cloud? <span>＋</span></summary>
          <p><strong>Never.</strong> Every scan, thumbnail preview, and SHA-256 calculation runs 100% locally on your Android device using official Scoped Storage APIs (zero MANAGE_EXTERNAL_STORAGE).</p>
        </details>
      </div>
    </section>
`;

function upgradeHtmlFile(relDir) {
  const filePath = relDir ? path.join(docsDir, relDir, "index.html") : path.join(docsDir, "index.html");
  if (!fs.existsSync(filePath)) return;
  const meta = LOCALE_META[relDir] || LOCALE_META.en;
  const pageUrl = relDir ? `https://midiastory-df6a0.web.app/${relDir}/` : "https://midiastory-df6a0.web.app/";

  let html = fs.readFileSync(filePath, "utf8");

  // 1. Replace theme-color, title, description, keywords, og:*, twitter:*
  html = html.replace(/<meta name="theme-color" content="[^"]*">/, '<meta name="theme-color" content="#000000">');
  html = html.replace(/<title>[\s\S]*?<\/title>/, `<title>${meta.title}</title>`);
  html = html.replace(/<meta name="description" content="[^"]*">/, `<meta name="description" content="${meta.desc}">`);
  html = html.replace(/<meta name="keywords" content="[^"]*">/, `<meta name="keywords" content="${meta.keywords}">`);
  html = html.replace(/<meta property="og:title" content="[^"]*">/, `<meta property="og:title" content="${meta.title}">`);
  html = html.replace(/<meta property="og:description" content="[^"]*">/, `<meta property="og:description" content="${meta.desc}">`);
  html = html.replace(/<meta name="twitter:title" content="[^"]*">/, `<meta name="twitter:title" content="${meta.title}">`);
  html = html.replace(/<meta name="twitter:description" content="[^"]*">/, `<meta name="twitter:description" content="${meta.desc}">`);

  // 2. Replace SoftwareApplication JSON-LD with clean v2.7.6 pure storage cleaner schema
  const cleanAppSchema = JSON.stringify({
    "@context": "https://schema.org",
    "@type": "SoftwareApplication",
    name: "Midiastore",
    alternateName: meta.title,
    operatingSystem: "Android 10+",
    applicationCategory: "UtilitiesApplication",
    inLanguage: meta.lang,
    softwareVersion: "2.7.6",
    fileSize: "5.9 MB",
    url: pageUrl,
    downloadUrl: "https://play.google.com/store/apps/details?id=com.rdf112.midiastore",
    installUrl: "https://play.google.com/apps/testing/com.rdf112.midiastore",
    description: meta.desc,
    keywords: meta.keywords,
    featureList:
      "1-Button 2-Tap Visual Cleanup, Exact SHA-256 Duplicate Finder, Large Video Cleaner (>10 MB), Old Media & Screenshots (+30 days), WhatsApp Media Cleaner, Official Android Trash & Consent Flow, 100% On-Device Scoped Storage",
    offers: { "@type": "Offer", price: "0", priceCurrency: relDir === "" ? "BRL" : "USD" },
  });
  html = html.replace(
    /<script type="application\/ld\+json">\{"@context":"https:\/\/schema\.org","@type":"SoftwareApplication"[\s\S]*?<\/script>/,
    `<script type="application/ld+json">${cleanAppSchema}</script>`
  );

  // 3. Upgrade CSS palette to Pure OLED (#000000 / #1C1C1E / #0A84FF) across all 13 locales
  html = html.replace(
    /:root\{--bg:#07111f;--card:#0f1d31;--line:#223652;--text:#edf3fc;--muted:#9ab0ce;--mint:#5de3bf;--white:#fff;--max:980px\}/g,
    ":root{--bg:#000000;--card:#1C1C1E;--line:#2C2C2E;--text:#F5F5F7;--muted:#98989D;--mint:#0A84FF;--white:#fff;--max:980px}"
  );
  html = html.replace(
    /background:var\(--mint\);color:#041019;/g,
    "background:var(--mint);color:#ffffff;"
  );

  // 4. Inject extra CSS & conversion sections into PT-BR and EN if not already present
  if (!html.includes(".compare-section{")) {
    html = html.replace("</style>", `${EXTRA_CSS}  </style>`);
  }
  if (relDir === "" && !html.includes('id="planos"')) {
    html = html.replace(
      '<section style="margin-top: 36px;">',
      `${PT_CONVERSION_SECTIONS}\n    <section style="margin-top: 36px;">`
    );
  } else if (relDir === "en" && !html.includes('id="faq"')) {
    html = html.replace(
      "</main>",
      `${EN_CONVERSION_SECTIONS}\n  </main>`
    );
  }

  fs.writeFileSync(filePath, html, "utf8");
  console.log(`Updated locale page: ${relDir || "pt-BR (root)"}`);
}

function updateSitemapLastmod() {
  const sitemapPath = path.join(docsDir, "sitemap.xml");
  if (!fs.existsSync(sitemapPath)) return;
  let xml = fs.readFileSync(sitemapPath, "utf8");
  xml = xml.replace(/<lastmod>[^<]+<\/lastmod>/g, "<lastmod>2026-11-21</lastmod>");
  fs.writeFileSync(sitemapPath, xml, "utf8");
  const urlCount = (xml.match(/<loc>/g) || []).length;
  console.log(`Updated docs/sitemap.xml (${urlCount} URLs with lastmod=2026-11-21)`);
}

for (const dir of LOCALE_DIRS) {
  upgradeHtmlFile(dir);
}
updateSitemapLastmod();
