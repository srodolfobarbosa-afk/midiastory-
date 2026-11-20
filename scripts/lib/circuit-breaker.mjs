import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..", "..");
const defaultStatePath = path.join(projectRoot, ".tmp", "circuit-breaker-state.json");

/**
 * Lightweight persistent Circuit Breaker for multi-LLM providers/models.
 * States: CLOSED -> OPEN (cooldown after failures) -> HALF_OPEN (single probe after cooldown).
 */
export class ProviderCircuitBreaker {
  constructor({
    statePath = defaultStatePath,
    failureThreshold = 2,
    baseCooldownMs = 5 * 60 * 1000,
    maxCooldownMs = 20 * 60 * 1000,
  } = {}) {
    this.statePath = statePath;
    this.failureThreshold = failureThreshold;
    this.baseCooldownMs = baseCooldownMs;
    this.maxCooldownMs = maxCooldownMs;
    this.state = this._load();
  }

  _load() {
    try {
      if (fs.existsSync(this.statePath)) {
        const parsed = JSON.parse(fs.readFileSync(this.statePath, "utf8"));
        if (parsed && typeof parsed === "object") return parsed;
      }
    } catch {}
    return {};
  }

  _save() {
    try {
      fs.mkdirSync(path.dirname(this.statePath), { recursive: true });
      fs.writeFileSync(this.statePath, JSON.stringify(this.state, null, 2), "utf8");
    } catch {}
  }

  canRequest(key, nowMs = Date.now()) {
    const entry = this.state[key];
    if (!entry) return { allowed: true, state: "CLOSED" };
    if (entry.state === "OPEN") {
      if (nowMs >= entry.openUntilMs) {
        entry.state = "HALF_OPEN";
        this._save();
        return { allowed: true, state: "HALF_OPEN" };
      }
      return {
        allowed: false,
        state: "OPEN",
        retryAfterSec: Math.ceil((entry.openUntilMs - nowMs) / 1000),
        lastError: entry.lastError,
      };
    }
    return { allowed: true, state: entry.state || "CLOSED" };
  }

  recordSuccess(key) {
    if (this.state[key]) {
      delete this.state[key];
      this._save();
    }
  }

  recordFailure(key, { status = null, message = "" } = {}, nowMs = Date.now()) {
    const entry = this.state[key] || { failures: 0, state: "CLOSED", openUntilMs: 0 };
    entry.failures += 1;
    entry.lastError = String(message || status || "error").slice(0, 160);
    entry.lastStatus = status;
    entry.updatedAt = new Date(nowMs).toISOString();

    // Immediate open on 402 (no credits) or 404 (model removed); threshold-based for 429/5xx/timeouts
    const instantOpen = status === 402 || status === 404;
    if (instantOpen || entry.failures >= this.failureThreshold) {
      const multiplier = Math.min(4, Math.max(1, entry.failures - this.failureThreshold + 1));
      const cooldown = Math.min(this.maxCooldownMs, this.baseCooldownMs * multiplier);
      entry.state = "OPEN";
      entry.openUntilMs = nowMs + cooldown;
    }

    this.state[key] = entry;
    this._save();
    return entry;
  }
}
