(function () {
  const SUPABASE_URL = "https://usmqtnlsaavxqguwrtix.supabase.co";
  const ANON_KEY =
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVzbXF0bmxzYWF2eHFndXdydGl4Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTQwNzcwNTQsImV4cCI6MjEwOTY1MzA1NH0.N7XIpn3lGJM7V8clfU8uKe-7v-N3qKPkIgv3sCKv2VU";

  function getVisitorId() {
    try {
      const key = "ms_anon_vid";
      let vid = localStorage.getItem(key);
      if (!vid) {
        vid =
          typeof crypto !== "undefined" && crypto.randomUUID
            ? crypto.randomUUID()
            : "v_" + Math.random().toString(36).slice(2) + Date.now().toString(36);
        localStorage.setItem(key, vid);
      }
      return String(vid).slice(0, 64);
    } catch (_) {
      return null;
    }
  }

  function getUtm() {
    const params = new URLSearchParams(window.location.search || "");
    const clean = (v, max) => (v ? String(v).trim().slice(0, max) : null);
    let refHost = null;
    try {
      if (document.referrer) {
        refHost = clean(new URL(document.referrer).hostname, 120);
      }
    } catch (_) {}
    return {
      utm_source: clean(params.get("utm_source"), 80),
      utm_medium: clean(params.get("utm_medium"), 80),
      utm_campaign: clean(params.get("utm_campaign"), 100),
      referrer_host: refHost,
    };
  }

  function sendEvent(eventType, sourceOverride) {
    const utm = getUtm();
    const payload = {
      page_path: String(window.location.pathname || "/").slice(0, 200),
      locale: String(document.documentElement.lang || "pt-BR").slice(0, 16),
      event_type: eventType,
      utm_source: sourceOverride || utm.utm_source,
      utm_medium: utm.utm_medium,
      utm_campaign: utm.utm_campaign,
      referrer_host: utm.referrer_host,
      visitor_id: getVisitorId(),
    };

    try {
      const endpoint = `${SUPABASE_URL}/rest/v1/web_page_events`;
      const bodyStr = JSON.stringify(payload);
      fetch(endpoint, {
        method: "POST",
        keepalive: true,
        headers: {
          apikey: ANON_KEY,
          Authorization: `Bearer ${ANON_KEY}`,
          "Content-Type": "application/json",
          Prefer: "return=minimal",
        },
        body: bodyStr,
      }).catch(() => {});
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
    refreshPrelaunchStats,
    sharePage,
  };

  window.addEventListener("DOMContentLoaded", () => {
    sendEvent("page_view");
    refreshPrelaunchStats();

    document.querySelectorAll("[data-track-cta], a[href*='play.google.com/apps/testing']").forEach((el) => {
      el.addEventListener("click", () => {
        const ctaName = el.getAttribute("data-track-cta") || "play_optin_link";
        const href = el.getAttribute("href") || "";
        const isPlayOptin =
          ctaName.includes("optin") || href.indexOf("play.google.com/apps/testing") !== -1;
        sendEvent(isPlayOptin ? "optin_click" : "cta_click", ctaName);
        setTimeout(refreshPrelaunchStats, 450);
      });
    });
  });
})();
