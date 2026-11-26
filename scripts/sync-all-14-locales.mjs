#!/usr/bin/env node
/**
 * sync-all-14-locales.mjs
 * Garante 100% de cobertura nos 14 idiomas Android (mais values/ fallback),
 * incluindo as traduções nativas de cleaner_home_analyze ("Limpar Meu Espaço Agora"),
 * cleanup_sheet_title, filter_screenshots_30d e o Card de Psicologia de Vendas (sales_psych_*).
 */

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");
const resRoot = path.join(projectRoot, "app", "src", "main", "res");

const NATIVE_OVERRIDES = {
  "values-zh-rCN": {
    cleaner_home_analyze: "立即清理我的存储空间",
    cleaner_home_scanning: "正在准备快速清理…",
    sales_psych_badge: "PRO 模式 • 年度订阅省 56%",
    sales_psych_title: "释放闲置应用（90天+）的隐藏空间并解锁 Pro 审计报告",
    sales_psych_subtitle: "年度计划每天不到 $0.05 • Google Play 官方保障（随时一键取消）。",
    sales_psych_cta: "立即解锁 Pro 模式",
    cleanup_sheet_title: "核对并清理",
    wa_sheet_subtitle: "选择要核对的 WhatsApp 媒体类别，然后再移至回收站或永久删除。",
    filter_screenshots_30d: "旧截图 (+30天)",
    trash_unsupported_on_device: "Android 11+ 支持系统回收站；在此设备上请使用永久删除。",
    oaas_mission_screenshots: "清理旧截图 (+30天)",
    quick_clean_title: "快速清理",
  },
  "values-es": {
    cleaner_home_analyze: "Limpiar Mi Espacio Ahora",
    cleaner_home_scanning: "Preparando limpieza rápida…",
    sales_psych_badge: "MODO PRO • AHORRA 56% EN PLAN ANUAL",
    sales_psych_title: "Libera espacio oculto de apps sin uso (+90 días) e informes Pro",
    sales_psych_subtitle: "Menos de $0.05/día en el Plan Anual • Garantía oficial de Google Play (cancela en 1 toque).",
    sales_psych_cta: "Desbloquear Modo Pro Ahora",
    cleanup_sheet_title: "Revisar y Limpiar",
    wa_sheet_subtitle: "Elige las categorías de WhatsApp para revisar antes de mover a la papelera o borrar.",
    filter_screenshots_30d: "Capturas (+30d)",
    trash_unsupported_on_device: "La papelera requiere Android 11+; usa eliminación permanente en este dispositivo.",
    oaas_mission_screenshots: "Limpiar capturas antiguas (+30 días)",
    quick_clean_title: "Limpieza Rápida",
  },
  "values-fr": {
    cleaner_home_analyze: "Nettoyer Mon Espace Maintenant",
    cleaner_home_scanning: "Préparation du nettoyage rapide…",
    sales_psych_badge: "MODE PRO • ÉCONOMISEZ 56% À L'ANNÉE",
    sales_psych_title: "Libérez l'espace caché des applis inutilisées (+90 j) et rapports Pro",
    sales_psych_subtitle: "Moins de 0,05 $/jour avec l'offre annuelle • Garantie Google Play (annulez en 1 geste).",
    sales_psych_cta: "Débloquer le Mode Pro Maintenant",
    cleanup_sheet_title: "Vérifier et Nettoyer",
    wa_sheet_subtitle: "Sélectionnez les catégories WhatsApp à vérifier avant la corbeille ou suppression.",
    filter_screenshots_30d: "Captures (+30j)",
    trash_unsupported_on_device: "La corbeille nécessite Android 11+ ; utilisez la suppression définitive.",
    oaas_mission_screenshots: "Nettoyer les anciennes captures (+30 jours)",
    quick_clean_title: "Nettoyage Rapide",
  },
  "values-de": {
    cleaner_home_analyze: "Meinen Speicher Jetzt Reinigen",
    cleaner_home_scanning: "Schnellreinigung wird vorbereitet…",
    sales_psych_badge: "PRO-MODUS • 56% SPAREN IM JAHRESABO",
    sales_psych_title: "Versteckten Speicher ungenutzter Apps (90+ Tage) & Pro-Berichte freischalten",
    sales_psych_subtitle: "Weniger als 0,05 $/Tag im Jahresabo • Offizieller Google Play-Schutz (jederzeit kündbar).",
    sales_psych_cta: "Pro-Modus Jetzt Freischalten",
    cleanup_sheet_title: "Prüfen & Reinigen",
    wa_sheet_subtitle: "WhatsApp-Kategorien auswählen und vor dem Löschen prüfen.",
    filter_screenshots_30d: "Screenshots (+30T)",
    trash_unsupported_on_device: "System-Papierkorb erfordert Android 11+; dauerhaftes Löschen verwenden.",
    oaas_mission_screenshots: "Alte Screenshots bereinigen (+30 Tage)",
    quick_clean_title: "Schnellreinigung",
  },
  "values-hi": {
    cleaner_home_analyze: "मेरा स्टोरेज अभी साफ़ करें",
    cleaner_home_scanning: "तेज़ क्लीनअप तैयार हो रहा है…",
    sales_psych_badge: "PRO मोड • वार्षिक प्लान पर 56% बचाएं",
    sales_psych_title: "बिना उपयोग वाले ऐप्स (90+ दिन) की छिपी जगह और Pro रिपोर्ट अनलॉक करें",
    sales_psych_subtitle: "वार्षिक प्लान में $0.05/दिन से कम • आधिकारिक Google Play सुरक्षा (कभी भी रद्द करें)।",
    sales_psych_cta: "अभी Pro मोड अनलॉक करें",
    cleanup_sheet_title: "समीक्षा करें और साफ़ करें",
    wa_sheet_subtitle: "ट्रैश या स्थायी रूप से हटाने से पहले WhatsApp श्रेणियों की समीक्षा करें।",
    filter_screenshots_30d: "स्क्रीनशॉट (+30 दिन)",
    trash_unsupported_on_device: "सिस्टम ट्रैश के लिए Android 11+ आवश्यक है।",
    oaas_mission_screenshots: "पुराने स्क्रीनशॉट साफ़ करें (+30 दिन)",
    quick_clean_title: "त्वरित सफ़ाई",
  },
  "values-ja": {
    cleaner_home_analyze: "今すぐストレージを整理",
    cleaner_home_scanning: "クイック整理を準備中…",
    sales_psych_badge: "PROモード • 年間プランで56%お得",
    sales_psych_title: "未使用アプリ（90日以上）の隠れた容量とPro監査レポートを解放",
    sales_psych_subtitle: "年間プランなら1日あたり約$0.05未満 • Google Play公式保護（いつでも解約可能）。",
    sales_psych_cta: "今すぐProモードを解除",
    cleanup_sheet_title: "確認して整理",
    wa_sheet_subtitle: "ゴミ箱移動や完全削除の前にWhatsAppカテゴリを確認してください。",
    filter_screenshots_30d: "スクショ (+30日)",
    trash_unsupported_on_device: "システムゴミ箱はAndroid 11以降で対応しています。",
    oaas_mission_screenshots: "古いスクリーンショットを整理 (+30日)",
    quick_clean_title: "クイック整理",
  },
  "values-ar": {
    cleaner_home_analyze: "تنظيف مساحتي الآن",
    cleaner_home_scanning: "جارٍ تحضير التنظيف السريع…",
    sales_psych_badge: "وضع PRO • وفّر 56% في الخطة السنوية",
    sales_psych_title: "حرّر المساحة المخفية للتطبيقات غير المستخدمة (+90 يوماً) وتقارير Pro",
    sales_psych_subtitle: "أقل من 0.05$/يوم في الخطة السنوية • حماية رسمية من Google Play (إلغاء بلمسة واحدة).",
    sales_psych_cta: "فتح وضع Pro الآن",
    cleanup_sheet_title: "مراجعة وتنظيف",
    wa_sheet_subtitle: "اختر فئات وسائط واتساب للمراجعة قبل النقل إلى المهملات أو الحذف النهائي.",
    filter_screenshots_30d: "لقطات الشاشة (+30ي)",
    trash_unsupported_on_device: "سلة المهملات تتطلب Android 11+.",
    oaas_mission_screenshots: "تنظيف لقطات الشاشة القديمة (+30 يوماً)",
    quick_clean_title: "تنظيف سريع",
  },
  "values-ru": {
    cleaner_home_analyze: "Очистить Память Сейчас",
    cleaner_home_scanning: "Подготовка быстрой очистки…",
    sales_psych_badge: "РЕЖИМ PRO • СКИДКА 56% НА ГОД",
    sales_psych_title: "Освободите скрытое место от неиспользуемых приложений (90+ дн.) и отчетов Pro",
    sales_psych_subtitle: "Менее $0.05/день в годовом плане • Защита Google Play (отмена в 1 касание).",
    sales_psych_cta: "Открыть Режим Pro Сейчас",
    cleanup_sheet_title: "Проверка и Очистка",
    wa_sheet_subtitle: "Выберите категории WhatsApp для проверки перед удалением.",
    filter_screenshots_30d: "Скриншоты (+30д)",
    trash_unsupported_on_device: "Системная корзина доступна на Android 11+.",
    oaas_mission_screenshots: "Очистить старые скриншоты (+30 дней)",
    quick_clean_title: "Быстрая Очистка",
  },
  "values-in": {
    cleaner_home_analyze: "Bersihkan Memori Sekarang",
    cleaner_home_scanning: "Menyiapkan pembersihan cepat…",
    sales_psych_badge: "MODE PRO • HEMAT 56% PAKET TAHUNAN",
    sales_psych_title: "Bebaskan ruang tersembunyi dari aplikasi tak terpakai (90+ hari) & laporan Pro",
    sales_psych_subtitle: "Kurang dari $0.05/hari di Paket Tahunan • Jaminan resmi Google Play (batalkan kapan saja).",
    sales_psych_cta: "Buka Mode Pro Sekarang",
    cleanup_sheet_title: "Tinjau & Bersihkan",
    wa_sheet_subtitle: "Pilih kategori media WhatsApp untuk ditinjau sebelum dihapus.",
    filter_screenshots_30d: "Tangkapan layar (+30h)",
    trash_unsupported_on_device: "Sampah sistem memerlukan Android 11+.",
    oaas_mission_screenshots: "Bersihkan tangkapan layar lama (+30 hari)",
    quick_clean_title: "Pembersihan Cepat",
  },
  "values-bn": {
    cleaner_home_analyze: "এখনই আমার স্টোরেজ পরিষ্কার করুন",
    cleaner_home_scanning: "দ্রুত ক্লিনআপ প্রস্তুত করা হচ্ছে…",
    sales_psych_badge: "PRO মোড • বার্ষিক প্ল্যানে 56% সাশ্রয়",
    sales_psych_title: "অব্যবহৃত অ্যাপ (৯০+ দিন) থেকে লুকানো জায়গা এবং Pro রিপোর্ট আনলক করুন",
    sales_psych_subtitle: "বার্ষিক প্ল্যানে প্রতিদিন $0.05-এর কম • অফিসিয়াল Google Play সুরক্ষা (যেকোনো সময় বাতিল)।",
    sales_psych_cta: "এখনই Pro মোড আনলক করুন",
    cleanup_sheet_title: "রিভিউ ও পরিষ্কার",
    wa_sheet_subtitle: "ট্র্যাশ বা স্থায়ীভাবে মুছে ফেলার আগে WhatsApp ক্যাটাগরি যাচাই করুন।",
    filter_screenshots_30d: "স্ক্রিনশট (+৩০ দিন)",
    trash_unsupported_on_device: "সিস্টেম ট্র্যাশের জন্য Android 11+ প্রয়োজন।",
    oaas_mission_screenshots: "পুরনো স্ক্রিনশট পরিষ্কার (+৩০ দিন)",
    quick_clean_title: "দ্রুত ক্লিনআপ",
  },
  "values-ko": {
    cleaner_home_analyze: "지금 내 저장공간 정리하기",
    cleaner_home_scanning: "빠른 정리 준비 중…",
    sales_psych_badge: "PRO 모드 • 연간 플랜 56% 할인",
    sales_psych_title: "미사용 앱(90일+)의 숨겨진 공간 확보 및 Pro 감사 리포트 잠금 해제",
    sales_psych_subtitle: "연간 플랜 기준 하루 $0.05 미만 • Google Play 공식 보호(언제든 해지 가능).",
    sales_psych_cta: "지금 Pro 모드 잠금 해제",
    cleanup_sheet_title: "검토 및 정리",
    wa_sheet_subtitle: "휴지통 이동 또는 영구 삭제 전에 WhatsApp 미디어 카테고리를 검토하세요.",
    filter_screenshots_30d: "스크린샷 (+30일)",
    trash_unsupported_on_device: "시스템 휴지통은 Android 11 이상에서 지원됩니다.",
    oaas_mission_screenshots: "오래된 스크린샷 정리 (+30일)",
    quick_clean_title: "빠른 정리",
  },
  "values-it": {
    cleaner_home_analyze: "Pulisci subito la memoria",
    cleaner_home_scanning: "Preparazione della pulizia rapida…",
    sales_psych_badge: "MODALITÀ PRO • RISPARMIA IL 56% CON L’ANNUALE",
    sales_psych_title: "Libera spazio dalle app inutilizzate (90+ giorni) e sblocca i report Pro",
    sales_psych_subtitle: "Meno di 0,05 $/giorno con il piano annuale • Protezione ufficiale Google Play (annulla quando vuoi).",
    sales_psych_cta: "Sblocca subito la modalità Pro",
    cleanup_sheet_title: "Controlla e pulisci",
    wa_sheet_subtitle: "Seleziona le categorie WhatsApp da controllare prima di spostarle nel cestino o eliminarle.",
    filter_screenshots_30d: "Screenshot meno recenti (+30 giorni)",
    trash_unsupported_on_device: "Il cestino di sistema richiede Android 11+; su questo dispositivo usa l’eliminazione definitiva.",
    oaas_mission_screenshots: "Pulisci gli screenshot meno recenti (+30 giorni)",
    quick_clean_title: "Pulizia rapida",
  },
  "values-en": {
    cleaner_home_analyze: "Clean My Storage Now",
    cleaner_home_scanning: "Preparing quick cleanup…",
    sales_psych_badge: "PRO MODE • SAVE 56% ON ANNUAL",
    sales_psych_title: "Free hidden space from unused apps (90d+) & audited Pro reports",
    sales_psych_subtitle: "Less than $0.05/day on the Annual Plan • Official Google Play protection (cancel anytime).",
    sales_psych_cta: "Unlock Pro Mode Now",
    cleanup_sheet_title: "Review & Clean",
    wa_sheet_subtitle: "Select WhatsApp media categories to review before Trash or permanent deletion.",
    filter_screenshots_30d: "Screenshots (+30d)",
    trash_unsupported_on_device: "System Trash requires Android 11+; use permanent delete on this device.",
    oaas_mission_screenshots: "Clean old screenshots (+30 days)",
    quick_clean_title: "Quick Cleanup",
  },
};

function escapeAndroidXml(text) {
  return text
    .replace(/&(?!(amp|lt|gt|quot|apos);)/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/'/g, "\\'");
}

function parseStringsMap(xmlContent) {
  const map = new Map();
  const regex = /<string\s+name="([^"]+)"[^>]*>([\s\S]*?)<\/string>/g;
  let match;
  while ((match = regex.exec(xmlContent)) !== null) {
    map.set(match[1], match[2]);
  }
  return map;
}

const baseXml = fs.readFileSync(path.join(resRoot, "values", "strings.xml"), "utf8");
const baseMap = parseStringsMap(baseXml);

for (const [dirName, overrides] of Object.entries(NATIVE_OVERRIDES)) {
  const filePath = path.join(resRoot, dirName, "strings.xml");
  if (!fs.existsSync(filePath)) continue;
  let content = fs.readFileSync(filePath, "utf8");
  const localMap = parseStringsMap(content);

  // 1. Update existing keys that have native overrides
  for (const [k, v] of Object.entries(overrides)) {
    const escaped = escapeAndroidXml(v);
    if (localMap.has(k)) {
      const keyRegex = new RegExp(`(<string\\s+name="${k}"[^>]*>)[\\s\\S]*?(<\\/string>)`);
      content = content.replace(keyRegex, `$1${escaped}$2`);
      localMap.set(k, escaped);
    }
  }

  // 2. Append any missing keys from baseMap (using native override if defined, else base fallback)
  const missingLines = [];
  for (const [k, baseVal] of baseMap.entries()) {
    if (!localMap.has(k)) {
      const val = overrides[k] ? escapeAndroidXml(overrides[k]) : baseVal;
      missingLines.push(`    <string name="${k}">${val}</string>`);
    }
  }

  if (missingLines.length > 0) {
    content = content.replace("</resources>", `${missingLines.join("\n")}\n</resources>`);
  }

  fs.writeFileSync(filePath, content, "utf8");
  const finalMap = parseStringsMap(content);
  console.log(`${dirName}: ${finalMap.size}/${baseMap.size} strings sincronizadas`);
}
