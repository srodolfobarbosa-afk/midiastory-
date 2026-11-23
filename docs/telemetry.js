(function () {
  const SUPABASE_URL = "https://usmqtnlsaavxqguwrtix.supabase.co";
  const ANON_KEY =
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVzbXF0bmxzYWF2eHFndXdydGl4Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTQwNzcwNTQsImV4cCI6MjEwOTY1MzA1NH0.N7XIpn3lGJM7V8clfU8uKe-7v-N3qKPkIgv3sCKv2VU";
  const EDGE_INGEST_URL = `${SUPABASE_URL}/functions/v1/ingest-product-event`;
  const APP_VERSION = "web-2.8.2";

  function getVisitorId() {
    try {
      const key = "ms_anon_vid";
      let vid = localStorage.getItem(key);
      if (!vid) {
        const raw =
          typeof crypto !== "undefined" && crypto.randomUUID
            ? crypto.randomUUID().replace(/-/g, "")
            : Array.from({ length: 32 }, () => Math.floor(Math.random() * 16).toString(16)).join("");
        vid = raw.slice(0, 32);
        localStorage.setItem(key, vid);
      }
      if (!/^[a-f0-9]{16,64}$/i.test(vid)) {
        vid = Array.from({ length: 32 }, () => Math.floor(Math.random() * 16).toString(16)).join("");
        localStorage.setItem(key, vid);
      }
      return String(vid).slice(0, 64);
    } catch (_) {
      return "a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5";
    }
  }

  function detectSocialChannel(params, refHost) {
    const raw = [
      params.get("utm_source"),
      params.get("utm_medium"),
      params.get("utm_campaign"),
      params.get("utm_content"),
      params.get("src"),
      params.get("ref"),
      refHost,
    ]
      .filter(Boolean)
      .join(" ")
      .toLowerCase();

    const tags = [];
    if (raw.includes("instagram") || raw.includes("ig")) tags.push("instagram");
    if (raw.includes("tiktok") || raw.includes("ttclid")) tags.push("tiktok");
    if (raw.includes("reels")) tags.push("reels");
    if (raw.includes("story") || raw.includes("stories")) tags.push("story");
    if (raw.includes("shorts") || raw.includes("youtube") || raw.includes("youtu.be")) tags.push("shorts");
    if (raw.includes("bio")) tags.push("bio");
    return tags.length > 0 ? tags.join("_").slice(0, 48) : null;
  }

  function getAttribution() {
    const params = new URLSearchParams(window.location.search || "");
    const clean = (v, max) => (v ? String(v).trim().slice(0, max) : null);
    let refHost = null;
    try {
      if (document.referrer) {
        refHost = clean(new URL(document.referrer).hostname, 120);
      }
    } catch (_) {}

    const fresh = {
      utm_source: clean(params.get("utm_source") || params.get("src"), 80),
      utm_medium: clean(params.get("utm_medium"), 80),
      utm_campaign: clean(params.get("utm_campaign"), 100),
      utm_term: clean(params.get("utm_term"), 100),
      utm_content: clean(params.get("utm_content"), 100),
      gclid: clean(params.get("gclid"), 120),
      fbclid: clean(params.get("fbclid"), 120),
      ttclid: clean(params.get("ttclid"), 120),
      referrer_host: refHost,
      social_channel: detectSocialChannel(params, refHost),
    };

    // Infer source/medium automatically from click IDs or social referrers
    if (!fresh.utm_source) {
      if (fresh.gclid) {
        fresh.utm_source = "google_ads";
        fresh.utm_medium = fresh.utm_medium || "cpc";
      } else if (fresh.fbclid) {
        fresh.utm_source = "meta_ads";
        fresh.utm_medium = fresh.utm_medium || "paid_social";
      } else if (fresh.ttclid) {
        fresh.utm_source = "tiktok";
        fresh.utm_medium = fresh.utm_medium || "paid_social";
      } else if (refHost && refHost.includes("instagram.com")) {
        fresh.utm_source = "instagram";
        fresh.utm_medium = fresh.utm_medium || "bio";
      } else if (refHost && refHost.includes("tiktok.com")) {
        fresh.utm_source = "tiktok";
        fresh.utm_medium = fresh.utm_medium || "bio";
      } else if (refHost && (refHost.includes("youtube.com") || refHost.includes("youtu.be"))) {
        fresh.utm_source = "shorts";
        fresh.utm_medium = fresh.utm_medium || "video";
      }
    }

    try {
      const storageKey = "ms_ad_attribution_v1";
      if (
        fresh.utm_source ||
        fresh.utm_medium ||
        fresh.utm_campaign ||
        fresh.gclid ||
        fresh.fbclid ||
        fresh.ttclid ||
        fresh.social_channel
      ) {
        sessionStorage.setItem(storageKey, JSON.stringify(fresh));
        return fresh;
      }
      const saved = sessionStorage.getItem(storageKey);
      if (saved) {
        return { ...JSON.parse(saved), referrer_host: fresh.referrer_host };
      }
    } catch (_) {}

    return fresh;
  }

  function buildPlayReferrer(attr) {
    const refParts = [];
    const source = attr.utm_source || "midiastore_web";
    const medium = attr.utm_medium || "landing_page";
    refParts.push(`utm_source=${encodeURIComponent(source)}`);
    refParts.push(`utm_medium=${encodeURIComponent(medium)}`);
    if (attr.utm_campaign) refParts.push(`utm_campaign=${encodeURIComponent(attr.utm_campaign)}`);
    if (attr.utm_term) refParts.push(`utm_term=${encodeURIComponent(attr.utm_term)}`);
    if (attr.utm_content) refParts.push(`utm_content=${encodeURIComponent(attr.utm_content)}`);
    if (attr.gclid) refParts.push(`gclid=${encodeURIComponent(attr.gclid)}`);
    if (attr.fbclid) refParts.push(`fbclid=${encodeURIComponent(attr.fbclid)}`);
    if (attr.ttclid) refParts.push(`ttclid=${encodeURIComponent(attr.ttclid)}`);
    return refParts.join("&");
  }

  function decoratePlayStoreLinks() {
    const attr = getAttribution();
    const playReferrer = buildPlayReferrer(attr);

    document.querySelectorAll("a[href*='play.google.com/store/apps/details']").forEach((a) => {
      try {
        const url = new URL(a.getAttribute("href"));
        url.searchParams.set("referrer", playReferrer);
        a.setAttribute("href", url.toString());
      } catch (_) {}
    });
  }

  function dispatchProductEvent(bodyObj) {
    const bodyStr = JSON.stringify(bodyObj);
    try {
      if (typeof navigator !== "undefined" && typeof navigator.sendBeacon === "function") {
        const blob = new Blob([bodyStr], { type: "text/plain;charset=UTF-8" });
        const queued = navigator.sendBeacon(EDGE_INGEST_URL, blob);
        if (queued) return true;
      }
    } catch (_) {}

    try {
      fetch(EDGE_INGEST_URL, {
        method: "POST",
        keepalive: true,
        headers: {
          "Content-Type": "text/plain;charset=UTF-8",
        },
        body: bodyStr,
      }).catch(() => {});
      return true;
    } catch (_) {
      return false;
    }
  }

  function sendEvent(eventType, sourceOverride) {
    const attr = getAttribution();
    const visitorId = getVisitorId();
    const locale = String(document.documentElement.lang || "pt-BR").slice(0, 16);
    const pagePath = String(window.location.pathname || "/").slice(0, 200);

    const payload = {
      page_path: pagePath,
      locale,
      event_type: eventType,
      utm_source: sourceOverride || attr.utm_source,
      utm_medium: attr.utm_medium,
      utm_campaign: attr.utm_campaign,
      referrer_host: attr.referrer_host,
      visitor_id: visitorId,
    };

    try {
      fetch(`${SUPABASE_URL}/rest/v1/web_page_events`, {
        method: "POST",
        keepalive: true,
        headers: {
          apikey: ANON_KEY,
          Authorization: `Bearer ${ANON_KEY}`,
          "Content-Type": "application/json",
          Prefer: "return=minimal",
        },
        body: JSON.stringify(payload),
      }).catch(() => {});

      const mappedProductEvent =
        eventType === "page_view"
          ? "page_view"
          : eventType === "optin_click" || eventType === "cta_click" || eventType === "play_store_click"
            ? "play_store_click"
            : null;

      if (mappedProductEvent) {
        dispatchProductEvent({
          installHash: visitorId,
          eventName: mappedProductEvent,
          appVersion: APP_VERSION,
          locale,
          properties: {
            path: pagePath,
            cta: sourceOverride || null,
            utm_source: attr.utm_source || "direct",
            utm_medium: attr.utm_medium || "organic",
            utm_campaign: attr.utm_campaign || null,
            utm_content: attr.utm_content || null,
            social_channel: attr.social_channel || null,
            referrer_host: attr.referrer_host || null,
            gclid: attr.gclid ? "1" : null,
            fbclid: attr.fbclid ? "1" : null,
            ttclid: attr.ttclid ? "1" : null,
          },
        });
      }
    } catch (_) {}
  }

  async function refreshPrelaunchStats() {
    try {
      const res = await fetch(`${SUPABASE_URL}/rest/v1/rpc/get_prelaunch_stats`, {
        method: "POST",
        headers: {
          apikey: ANON_KEY,
          Authorization: `Bearer ${ANON_KEY}`,
          "Content-Type": "application/json",
        },
        body: "{}",
      });
      if (!res.ok) return null;
      const stats = await res.json();
      const apply = (selector, val) => {
        document.querySelectorAll(selector).forEach((el) => {
          el.textContent = Number(val || 0).toLocaleString(document.documentElement.lang || "pt-BR");
        });
      };
      const syncedTesters =
        stats.active_testers_display !== undefined && stats.active_testers_display !== null
          ? stats.active_testers_display
          : stats.waitlist_count;
      apply('[data-stat="total_visits"]', stats.total_visits);
      apply('[data-stat="unique_visitors"]', stats.unique_visitors);
      apply('[data-stat="visits_24h"]', stats.visits_24h);
      apply('[data-stat="waitlist_count"]', syncedTesters);
      apply('[data-stat="active_testers_display"]', syncedTesters);
      apply('[data-stat="optin_clicks"]', stats.optin_clicks);
      apply('[data-stat="share_clicks"]', stats.share_clicks);
      return stats;
    } catch (_) {
      return null;
    }
  }

  function sharePage(title, text) {
    const urlObj = new URL(window.location.href);
    urlObj.searchParams.set("utm_source", "waitlist_share");
    urlObj.searchParams.set("utm_medium", "referral");
    urlObj.searchParams.set("utm_campaign", "prelaunch");
    const shareUrl = urlObj.toString();
    sendEvent("share_click", "waitlist_share");
    if (navigator.share) {
      navigator.share({ title, text, url: shareUrl }).catch(() => {});
      return true;
    }
    if (navigator.clipboard && navigator.clipboard.writeText) {
      navigator.clipboard.writeText(shareUrl).catch(() => {});
    }
    return false;
  }

  window.MidiastoreTelemetry = {
    trackEvent: sendEvent,
    dispatchProductEvent,
    refreshPrelaunchStats,
    sharePage,
    getAttribution,
    decoratePlayStoreLinks,
  };

  window.addEventListener("DOMContentLoaded", () => {
    decoratePlayStoreLinks();
    sendEvent("page_view");
    refreshPrelaunchStats();

    document
      .querySelectorAll(
        "[data-track-cta], a[href*='play.google.com/apps/testing'], a[href*='play.google.com/store/apps/details']",
      )
      .forEach((el) => {
        if (el.dataset.msBound === "1") return;
        el.dataset.msBound = "1";
        el.addEventListener("click", () => {
          const ctaName = el.getAttribute("data-track-cta") || "play_store_link";
          const href = el.getAttribute("href") || "";
          const isPlayLink =
            ctaName.includes("optin") ||
            ctaName.includes("play") ||
            href.indexOf("play.google.com/apps/testing") !== -1 ||
            href.indexOf("play.google.com/store/apps/details") !== -1;
          sendEvent(isPlayLink ? "play_store_click" : "cta_click", ctaName);
          setTimeout(refreshPrelaunchStats, 450);
        });
      });
  });
})();
