import fs from "node:fs";
import path from "node:path";

const RES_DIR = path.resolve("app/src/main/res");

function escapeXml(str) {
  return String(str)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/'/g, "\\'")
    .replace(/"/g, '\\"')
    .replace(/\n/g, "\\n");
}

const data = {
  pt: {
    home_pro_cta_badge: "👑 MIDIASTORE PRO • ASSINATURA VIP",
    home_pro_cta_title: "Desbloqueie Todo o Poder do Modo Pro",
    home_pro_cta_subtitle: "Detector de apps sem uso (+90 dias), exportação de relatórios auditados e prioridade VIP no plano Pro.",
    home_pro_subscribe_button: "⭐ ASSINAR MIDIASTORE PRO AGORA",
    home_pro_subscribe_button_with_price: "⭐ ASSINAR PRO AGORA · %1$s",
    home_pro_active_button: "✅ MIDIASTORE PRO ATIVO · VER BENEFÍCIOS",
    home_pro_cta_active_title: "Modo Pro VIP Desbloqueado no Seu Aparelho",
    home_pro_cta_footnote: "Planos Anual, Mensal ou Vitalício · Cobrança segura pelo Google Play · Cancele quando quiser",
    quick_pro_dialog_title: "👑 Assinar Midiastore Pro",
    quick_pro_dialog_subtitle: "Escolha seu plano abaixo para iniciar a assinatura oficial via Google Play Store:",
    quick_pro_open_tab_btn: "🎁 Testadores VIP / Detalhes",
    quick_pro_restore_btn: "Restaurar Compra",
    top_pro_active_pill: "⭐ VIP",
  },
  en: {
    home_pro_cta_badge: "👑 MIDIASTORE PRO • VIP ACCESS",
    home_pro_cta_title: "Unlock Full Midiastore Pro Power",
    home_pro_cta_subtitle: "90-day unused app detector, audited cleanup report exports & priority VIP Pro status.",
    home_pro_subscribe_button: "⭐ SUBSCRIBE TO MIDIASTORE PRO",
    home_pro_subscribe_button_with_price: "⭐ SUBSCRIBE PRO NOW · %1$s",
    home_pro_active_button: "✅ MIDIASTORE PRO ACTIVE · MANAGE PLAN",
    home_pro_cta_active_title: "Pro VIP Mode Unlocked on Your Device",
    home_pro_cta_footnote: "Choose Annual, Monthly, or Lifetime · Transparent Google Play billing · Cancel anytime",
    quick_pro_dialog_title: "👑 Subscribe to Midiastore Pro",
    quick_pro_dialog_subtitle: "Choose your plan below to start official checkout via Google Play Store:",
    quick_pro_open_tab_btn: "🎁 Tester VIP / Details",
    quick_pro_restore_btn: "Restore Purchase",
    top_pro_active_pill: "⭐ VIP",
  },
  es: {
    home_pro_cta_badge: "👑 MIDIASTORE PRO • ACCESO VIP",
    home_pro_cta_title: "Desbloquea Todo el Poder del Modo Pro",
    home_pro_cta_subtitle: "Detector de apps sin uso (+90 días), exportación de informes auditados y estado VIP Pro prioritario.",
    home_pro_subscribe_button: "⭐ SUSCRIBIRSE A MIDIASTORE PRO",
    home_pro_subscribe_button_with_price: "⭐ SUSCRIBIRSE PRO · %1$s",
    home_pro_active_button: "✅ MIDIASTORE PRO ACTIVO · VER PLAN",
    home_pro_cta_active_title: "Modo Pro VIP Desbloqueado en Tu Teléfono",
    home_pro_cta_footnote: "Elige Anual, Mensual o Vitalicio · Cobro seguro en Google Play · Cancela cuando quieras",
    quick_pro_dialog_title: "👑 Suscribirse a Midiastore Pro",
    quick_pro_dialog_subtitle: "Elige tu plan abajo para iniciar la suscripción oficial mediante Google Play Store:",
    quick_pro_open_tab_btn: "🎁 VIP Testers / Detalles",
    quick_pro_restore_btn: "Restaurar Compra",
    top_pro_active_pill: "⭐ VIP",
  },
  de: {
    home_pro_cta_badge: "👑 MIDIASTORE PRO • VIP-ZUGANG",
    home_pro_cta_title: "Volle Midiastore Pro-Power freischalten",
    home_pro_cta_subtitle: "90-Tage-App-Analyse, geprüfter Berichtsexport und VIP-Pro-Prioritätsstatus.",
    home_pro_subscribe_button: "⭐ MIDIASTORE PRO ABONNIEREN",
    home_pro_subscribe_button_with_price: "⭐ PRO JETZT ABONNIEREN · %1$s",
    home_pro_active_button: "✅ MIDIASTORE PRO AKTIV · VERWALTEN",
    home_pro_cta_active_title: "Pro-VIP-Modus auf Ihrem Gerät aktiv",
    home_pro_cta_footnote: "Jährlich, Monatlich oder Einmalig · Transparente Google Play-Abrechnung · Jederzeit kündbar",
    quick_pro_dialog_title: "👑 Midiastore Pro abonnieren",
    quick_pro_dialog_subtitle: "Wählen Sie unten Ihren Tarif für die offizielle Google Play-Zahlung:",
    quick_pro_open_tab_btn: "🎁 Tester-VIP / Details",
    quick_pro_restore_btn: "Kauf wiederherstellen",
    top_pro_active_pill: "⭐ VIP",
  },
  fr: {
    home_pro_cta_badge: "👑 MIDIASTORE PRO • ACCÈS VIP",
    home_pro_cta_title: "Débloquez Toute la Puissance du Mode Pro",
    home_pro_cta_subtitle: "Détecteur d'apps inutilisées (+90 j), export de rapports audités et statut VIP Pro prioritaire.",
    home_pro_subscribe_button: "⭐ S'ABONNER À MIDIASTORE PRO",
    home_pro_subscribe_button_with_price: "⭐ S'ABONNER PRO · %1$s",
    home_pro_active_button: "✅ MIDIASTORE PRO ACTIF · GÉRER",
    home_pro_cta_active_title: "Mode Pro VIP Débloqué sur Votre Appareil",
    home_pro_cta_footnote: "Annuel, Mensuel ou À vie · Paiement sécurisé Google Play · Annulez à tout moment",
    quick_pro_dialog_title: "👑 S'abonner à Midiastore Pro",
    quick_pro_dialog_subtitle: "Choisissez votre formule ci-dessous pour régler via Google Play Store :",
    quick_pro_open_tab_btn: "🎁 VIP Testeurs / Détails",
    quick_pro_restore_btn: "Restaurer l'achat",
    top_pro_active_pill: "⭐ VIP",
  },
  hi: {
    home_pro_cta_badge: "👑 MIDIASTORE PRO • VIP एक्सेस",
    home_pro_cta_title: "Midiastore Pro की पूरी शक्ति अनलॉक करें",
    home_pro_cta_subtitle: "90 दिनों से अनुपयोगी ऐप डिटेक्टर, ऑडिट की गई रिपोर्ट एक्सपोर्ट और VIP Pro स्टेटस।",
    home_pro_subscribe_button: "⭐ अभी MIDIASTORE PRO सब्सक्राइब करें",
    home_pro_subscribe_button_with_price: "⭐ अभी PRO लें · %1$s",
    home_pro_active_button: "✅ MIDIASTORE PRO सक्रिय · प्लान देखें",
    home_pro_cta_active_title: "आपके डिवाइस पर Pro VIP मोड अनलॉक है",
    home_pro_cta_footnote: "वार्षिक, मासिक या लाइफटाइम चुनें · सुरक्षित Google Play बिलिंग · कभी भी रद्द करें",
    quick_pro_dialog_title: "👑 Midiastore Pro सब्सक्राइब करें",
    quick_pro_dialog_subtitle: "Google Play Store द्वारा आधिकारिक भुगतान शुरू करने के लिए नीचे अपना प्लान चुनें:",
    quick_pro_open_tab_btn: "🎁 टेस्टर VIP / विवरण",
    quick_pro_restore_btn: "खरीद पुनर्स्थापित करें",
    top_pro_active_pill: "⭐ VIP",
  },
  ja: {
    home_pro_cta_badge: "👑 MIDIASTORE PRO • VIP アクセス",
    home_pro_cta_title: "Midiastore Pro の全機能を今すぐ解放",
    home_pro_cta_subtitle: "90日間未使用アプリ検出、監査済みレポート出力、VIP Pro ステータスを解放します。",
    home_pro_subscribe_button: "⭐ 今すぐ MIDIASTORE PRO を購読",
    home_pro_subscribe_button_with_price: "⭐ 今すぐ PRO を購読 · %1$s",
    home_pro_active_button: "✅ MIDIASTORE PRO 有効 · プラン管理",
    home_pro_cta_active_title: "お使いの端末で Pro VIP モードが有効です",
    home_pro_cta_footnote: "年額・月額・買い切りを選択 · Google Play による安全な課金 · いつでも解約可能",
    quick_pro_dialog_title: "👑 Midiastore Pro に登録",
    quick_pro_dialog_subtitle: "Google Play ストアでの公式お支払いプランを以下から選択してください：",
    quick_pro_open_tab_btn: "🎁 テスターVIP / 詳細",
    quick_pro_restore_btn: "購入を復元",
    top_pro_active_pill: "⭐ VIP",
  },
  zh: {
    home_pro_cta_badge: "👑 MIDIASTORE PRO • VIP 会员",
    home_pro_cta_title: "立即解锁 Midiastore Pro 全部专业功能",
    home_pro_cta_subtitle: "解锁90天未使用应用检测、审计报告导出与 VIP Pro 尊享权益。",
    home_pro_subscribe_button: "⭐ 立即订阅 MIDIASTORE PRO",
    home_pro_subscribe_button_with_price: "⭐ 立即订阅 PRO · %1$s",
    home_pro_active_button: "✅ MIDIASTORE PRO 已激活 · 管理会员",
    home_pro_cta_active_title: "您的设备已解锁 Pro VIP 尊享模式",
    home_pro_cta_footnote: "可选年付、月付或终身版 · Google Play 安全计费 · 随时可取消",
    quick_pro_dialog_title: "👑 订阅 Midiastore Pro",
    quick_pro_dialog_subtitle: "请在下方选择方案，通过 Google Play 商店完成官方订阅：",
    quick_pro_open_tab_btn: "🎁 测试员 VIP / 详情",
    quick_pro_restore_btn: "恢复购买",
    top_pro_active_pill: "⭐ VIP",
  },
};

const targets = [
  ["values", data.en],
  ["values-en", data.en],
  ["values-pt-rBR", data.pt],
  ["values-es", data.es],
  ["values-de", data.de],
  ["values-fr", data.fr],
  ["values-hi", data.hi],
  ["values-ja", data.ja],
  ["values-zh-rCN", data.zh],
];

for (const [folder, dict] of targets) {
  const filePath = path.join(RES_DIR, folder, "strings.xml");
  let xml = fs.readFileSync(filePath, "utf8");
  for (const [key, rawVal] of Object.entries(dict)) {
    const line = `    <string name="${key}">${escapeXml(rawVal)}</string>`;
    const re = new RegExp(`^\\s*<string\\s+name="${key}"(?:\\s+[^>]*)?>[\\s\\S]*?<\\/string>`, "m");
    if (re.test(xml)) {
      xml = xml.replace(re, line);
    } else {
      xml = xml.replace("</resources>", `${line}\n</resources>`);
    }
  }
  fs.writeFileSync(filePath, xml, "utf8");
}

console.log(`Synced ${Object.keys(data.en).length} Home Pro CTA & VIP polish keys across ${targets.length} locales.`);
