#!/usr/bin/env node
/**
 * sync-conversion-triggers-i18n.mjs
 * Traduz as 5 chaves dos gatilhos eticos de conversao (Selo Anual 56% + Momento de Alivio)
 * nos 12 idiomas restantes, garantindo 100% de cobertura nos 14 idiomas sem fallback.
 */
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");
const resDir = path.join(projectRoot, "app", "src", "main", "res");

const TRANSLATIONS = {
  "values-en": {
    pro_annual_savings_badge: "MOST POPULAR • SAVE 56%",
    post_cleanup_relief_title: "Space freed successfully!",
    post_cleanup_relief_body: "You just safely freed %1$s (%2$d file(s)).\\n\\nUnlock Midiastore Pro (unused apps 90d+, audited reports &amp; priority support) and save 56%% with the Annual Plan.",
    post_cleanup_relief_cta: "Unlock Annual Plan · Save 56%",
    post_cleanup_relief_later: "Not now",
  },
  "values-es": {
    pro_annual_savings_badge: "MÁS ELEGIDO • AHORRA 56%",
    post_cleanup_relief_title: "¡Espacio liberado con éxito!",
    post_cleanup_relief_body: "Acabas de liberar %1$s (%2$d archivo(s)) con seguridad.\\n\\nDesbloquea Midiastore Pro (apps sin uso +90d, informes auditados y soporte prioritario) ahorrando un 56%% en el Plan Anual.",
    post_cleanup_relief_cta: "Ver Plan Anual · Ahorra 56%",
    post_cleanup_relief_later: "Ahora no",
  },
  "values-fr": {
    pro_annual_savings_badge: "PLUS POPULAIRE • ÉCONOMISEZ 56%",
    post_cleanup_relief_title: "Espace libéré avec succès !",
    post_cleanup_relief_body: "Vous venez de libérer %1$s (%2$d fichier(s)) en toute sécurité.\\n\\nDébloquez Midiastore Pro (applis inutilisées 90j+, rapports audités et support prioritaire) en économisant 56%% avec l\'offre Annuelle.",
    post_cleanup_relief_cta: "Voir l\'offre Annuelle · -56%",
    post_cleanup_relief_later: "Plus tard",
  },
  "values-de": {
    pro_annual_savings_badge: "AM BELIEBTESTEN • 56% SPAREN",
    post_cleanup_relief_title: "Speicher erfolgreich freigegeben!",
    post_cleanup_relief_body: "Du hast gerade %1$s (%2$d Datei(en)) sicher freigegeben.\\n\\nSchalte Midiastore Pro frei (ungenutzte Apps 90T+, Prüfberichte &amp; Prioritäts-Support) und spare 56%% im Jahresabo.",
    post_cleanup_relief_cta: "Jahresabo ansehen · 56% sparen",
    post_cleanup_relief_later: "Nicht jetzt",
  },
  "values-hi": {
    pro_annual_savings_badge: "सबसे लोकप्रिय • 56% बचाएँ",
    post_cleanup_relief_title: "स्टोरेज सफलतापूर्वक खाली हुआ!",
    post_cleanup_relief_body: "आपने अभी सुरक्षित रूप से %1$s (%2$d फ़ाइलें) खाली की हैं।\\n\\nवार्षिक प्लान पर 56%% बचत के साथ Midiastore Pro (90+ दिन से अप्रयुक्त ऐप्स, ऑडिट रिपोर्ट और प्राथमिकता सहायता) अनलॉक करें।",
    post_cleanup_relief_cta: "वार्षिक प्लान देखें · 56% बचाएँ",
    post_cleanup_relief_later: "अभी नहीं",
  },
  "values-ja": {
    pro_annual_savings_badge: "一番人気 • 56% お得",
    post_cleanup_relief_title: "容量を安全に解放しました！",
    post_cleanup_relief_body: "%1$s（%2$d 件のファイル）を安全に解放しました。\\n\\n年額プランなら 56%% お得に Midiastore Pro（90日以上未使用のアプリ検出、監査レポート、優先サポート）をご利用いただけます。",
    post_cleanup_relief_cta: "年額プランを見る・56% お得",
    post_cleanup_relief_later: "あとで",
  },
  "values-zh-rCN": {
    pro_annual_savings_badge: "最受欢迎 • 省 56%",
    post_cleanup_relief_title: "空间已安全释放！",
    post_cleanup_relief_body: "您刚刚安全释放了 %1$s（%2$d 个文件）。\\n\\n选择年度方案立省 56%%，解锁 Midiastore Pro（90天以上未使用应用检测、审计报告与优先支持）。",
    post_cleanup_relief_cta: "查看年度方案 · 省 56%",
    post_cleanup_relief_later: "暂不",
  },
  "values-ar": {
    pro_annual_savings_badge: "الأكثر اختيارًا • وفّر 56%",
    post_cleanup_relief_title: "تم تحرير المساحة بنجاح!",
    post_cleanup_relief_body: "لقد حرّرت للتو %1$s (%2$d ملف) بأمان.\\n\\nافتح Midiastore Pro (التطبيقات غير المستخدمة +90 يومًا، التقارير المدققة والدعم ذي الأولوية) ووفّر 56%% مع الخطة السنوية.",
    post_cleanup_relief_cta: "عرض الخطة السنوية · وفّر 56%",
    post_cleanup_relief_later: "ليس الآن",
  },
  "values-bn": {
    pro_annual_savings_badge: "সবচেয়ে জনপ্রিয় • ৫৬% সাশ্রয়",
    post_cleanup_relief_title: "সফলভাবে স্পেস খালি হয়েছে!",
    post_cleanup_relief_body: "আপনি নিরাপদে %1$s (%2$d টি ফাইল) খালি করেছেন।\\n\\nবার্ষিক প্ল্যানে ৫৬%% সাশ্রয়ে Midiastore Pro (৯০+ দিন অব্যবহৃত অ্যাপ, অডিট রিপোর্ট ও অগ্রাধিকার সহায়তা) আনলক করুন।",
    post_cleanup_relief_cta: "বার্ষিক প্ল্যান দেখুন · ৫৬% সাশ্রয়",
    post_cleanup_relief_later: "এখন নয়",
  },
  "values-in": {
    pro_annual_savings_badge: "PALING POPULER • HEMAT 56%",
    post_cleanup_relief_title: "Ruang berhasil dikosongkan!",
    post_cleanup_relief_body: "Anda baru saja mengosongkan %1$s (%2$d file) dengan aman.\\n\\nBuka Midiastore Pro (aplikasi tidak dipakai 90h+, laporan audit &amp; dukungan prioritas) dan hemat 56%% dengan Paket Tahunan.",
    post_cleanup_relief_cta: "Lihat Paket Tahunan · Hemat 56%",
    post_cleanup_relief_later: "Nanti saja",
  },
  "values-ko": {
    pro_annual_savings_badge: "가장 인기 • 56% 절약",
    post_cleanup_relief_title: "공간이 안전하게 확보되었습니다!",
    post_cleanup_relief_body: "방금 %1$s(%2$d개 파일)를 안전하게 정리했습니다.\\n\\n연간 플랜으로 56%% 절약하며 Midiastore Pro(90일 이상 미사용 앱, 감사 보고서, 우선 지원)를 사용해 보세요.",
    post_cleanup_relief_cta: "연간 플랜 보기 · 56% 절약",
    post_cleanup_relief_later: "나중에",
  },
  "values-ru": {
    pro_annual_savings_badge: "ХИТ ВЫБОРА • ЭКОНОМИЯ 56%",
    post_cleanup_relief_title: "Память успешно освобождена!",
    post_cleanup_relief_body: "Вы безопасно освободили %1$s (%2$d файл(ов)).\\n\\nОткройте Midiastore Pro (неиспользуемые приложения 90+ дн., отчёты и приоритетная поддержка) со скидкой 56%% на Годовом плане.",
    post_cleanup_relief_cta: "Годовой план · Экономия 56%",
    post_cleanup_relief_later: "Не сейчас",
  },
};

for (const [folder, entries] of Object.entries(TRANSLATIONS)) {
  const filePath = path.join(resDir, folder, "strings.xml");
  if (!fs.existsSync(filePath)) continue;
  let xml = fs.readFileSync(filePath, "utf8");
  let added = 0;
  const toInsert = [];
  for (const [key, value] of Object.entries(entries)) {
    const regex = new RegExp(`<string\\s+name="${key}"`);
    if (!regex.test(xml)) {
      toInsert.push(`    <string name="${key}">${value}</string>`);
      added++;
    }
  }
  if (toInsert.length > 0) {
    xml = xml.replace(/<\/resources>\s*$/, `${toInsert.join("\n")}\n</resources>\n`);
    fs.writeFileSync(filePath, xml, "utf8");
  }
  console.log(`${folder.padEnd(14)} -> +${added} chaves de conversao adicionadas`);
}
