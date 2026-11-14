import fs from "node:fs";
import path from "node:path";
import { execFileSync } from "node:child_process";
import { fileURLToPath } from "node:url";
import { writeEpubPackage } from "./lib/epub-builder.mjs";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");
const ebookEnDir = path.join(projectRoot, "docs", "ebook-en");
const coverPs1 = path.join(__dirname, "render-book-cover.ps1");
const coverPngPath = path.join(ebookEnDir, "cover-play-books-en-1600x2560.png");
const epubOutPath = path.join(ebookEnDir, "Android_Storage_Mastery_Midiastore.epub");

const CTA_EN = "https://midiastory-df6a0.web.app/en/?utm_source=play_books_en&utm_medium=ebook_cta&utm_campaign=android_storage_mastery";

const META = {
  id: "urn:uuid:d41b7a20-9c4f-4e11-a8d2-3b6e910f55c2",
  title: "Android Storage Mastery: Reclaim Real Gigabytes Without Losing Memories",
  subtitle: "A Practical 100% On-Device Playbook for Exact SHA-256 Deduplication, EXIF Privacy, Scoped Storage & Offline Productivity",
  author: "Midiastore Engineering (Rodolfo Barbosa)",
  publisher: "Midiastore Editorial",
  lang: "en",
  modified: "2026-11-14T08:50:00Z",
};

const CHAPTERS = [
  {
    id: "ch1",
    file: "ch1.xhtml",
    title: "1. Why Your Android Phone Runs Out of Space Even When You Delete Photos",
    body: `
      <span class="chapter-tag">Chapter 01 • The Hidden Architecture</span>
      <h1>Why Android Storage Fills Up Quietly</h1>
      <p>Most users delete ten selfies, look at Settings, and discover almost nothing changed. On modern Android phones (Android 10 through Android 16), real storage pressure almost never comes from tiny text screenshots—it comes from silent media duplication, WhatsApp/messaging forwards, unindexed 10+ MB video clips, and the system Trash buffer.</p>
      <div class="callout">
        <strong>Core Fact: Trash Is Not Freed Storage</strong>
        On Android 11+ (API 30+), moving a photo or video to the system Trash keeps the full binary payload on disk for 30 days. Storage bytes are only genuinely reclaimed after permanent deletion is confirmed.
      </div>
      <h2>The 4 Real Culprits Measured by MediaStore</h2>
      <ul>
        <li><strong>Exact Duplicate Files:</strong> The same video or photo saved twice from WhatsApp groups, downloads, and cloud re-exports.</li>
        <li><strong>Oversized Clips (&gt;10 MB):</strong> Forgotten 4K screen recordings, forwarded event videos, and long voice/video notes.</li>
        <li><strong>30+ Day Stale Clutter:</strong> Temporary memes and receipts accumulating every month inside Downloads and messaging folders.</li>
        <li><strong>Metadata &amp; EXIF Exposure:</strong> Original camera files often embed GPS coordinates and camera serial details when shared unstripped.</li>
      </ul>`,
  },
  {
    id: "ch2",
    file: "ch2.xhtml",
    title: "2. Byte-Exact SHA-256 vs. Dangerous 'Smart Guess' Cleaners",
    body: `
      <span class="chapter-tag">Chapter 02 • Cryptographic Precision</span>
      <h1>Why File Size Alone Is Never Enough</h1>
      <p>Many legacy cleaner apps compare only filenames or approximate thumbnails, risking deletion of irreplaceable originals or nearly identical family shots you intentionally kept. By contrast, streaming <strong>SHA-256 cryptographic hashing</strong> inspects the exact byte stream of candidate files sharing the same byte size and guarantees bit-for-bit identity before tagging a duplicate.</p>
      <table>
        <thead><tr><th>Comparison Method</th><th>False Positives?</th><th>Safety Guarantee</th></tr></thead>
        <tbody>
          <tr><td>Filename matching</td><td>High</td><td>Unsafe for real backups</td></tr>
          <tr><td>File size only</td><td>Medium</td><td>Different files can share identical sizes</td></tr>
          <tr><td><strong>Streaming SHA-256</strong></td><td>Zero (practical certainty)</td><td>Safe — 1 original always kept protected</td></tr>
        </tbody>
      </table>
      <p>In addition, bounded on-device scanners keep memory safe on mid-range phones while ensuring you can pause or cancel a scan anytime without altering a single byte on disk.</p>`,
  },
  {
    id: "ch3",
    file: "ch3.xhtml",
    title: "3. Scoped Storage, Official Consent & Privacy (Zero MANAGE_EXTERNAL_STORAGE)",
    body: `
      <span class="chapter-tag">Chapter 03 • Android Security & Play Compliance</span>
      <h1>Why Ethical Cleaners Never Ask for Blanket Full-Disk Access</h1>
      <p>Starting with Android 10 and enforced strictly by Google Play policies, clean utility software operates exclusively inside official <strong>MediaStore</strong> and system consent dialogs (<code>MediaStore.createDeleteRequest</code> / <code>createTrashRequest</code> on API 30+, and item consent flows on API 29).</p>
      <div class="callout">
        <strong>Your Checklist Before Installing Any Cleaner</strong>
        <ul>
          <li>Does it avoid invasive "All Files Access" (<code>MANAGE_EXTERNAL_STORAGE</code>) when only handling Photos, Video, and Audio?</li>
          <li>Does it never promise fake "RAM Boost", "CPU Cooling", or wiping third-party app caches blocked by Android sandboxing?</li>
          <li>Does every file stay 100% offline on your device during scanning?</li>
        </ul>
      </div>`,
  },
  {
    id: "ch4",
    file: "ch4.xhtml",
    title: "4. WhatsApp Bloat, Local Media Studio & Offline Survival Toolkit",
    body: `
      <span class="chapter-tag">Chapter 04 • Daily Routine & Offline Utility</span>
      <h1>Turning Cleanup Into an Everyday Habit</h1>
      <p>Sustainable phone hygiene takes three minutes a week rather than panicking right before a concert or flight:</p>
      <ol>
        <li>Sort largest WhatsApp media first and remove forwarded videos you no longer need.</li>
        <li>Inspect your <strong>Protected Original</strong> badge before confirming any SHA-256 duplicate cleanup.</li>
        <li>Use built-in offline tools—audio recorder, instant PDF/QR creator, offline World Clock, and zero-internet mini-games—without installing five bloated ad-heavy apps.</li>
      </ol>
      <div class="callout">
        <strong>Companion App &amp; Multilingual Web Portal</strong>
        Explore the privacy-first Android app and browser utilities at:<br />
        <a href="${CTA_EN}">${CTA_EN}</a>
      </div>`,
  },
  {
    id: "ch5",
    file: "ch5.xhtml",
    title: "5. Complete 10-Point Phone Health Audit & Publishing Checklist",
    body: `
      <span class="chapter-tag">Chapter 05 • Action Plan</span>
      <h1>Your 10-Minute Storage Recovery Blueprint</h1>
      <ul>
        <li>Verify free GB vs. Total Storage before and after permanent Trash empty.</li>
        <li>Run SHA-256 duplicate review with preview thumbnails enabled.</li>
        <li>Audit +30-day downloads and large video clips above 10 MB.</li>
        <li>Keep all media analysis local and offline—zero cloud upload required.</li>
        <li>Review MediaStore folder permissions anytime in Android Settings.</li>
      </ul>
      <p>Thank you for reading <em>Android Storage Mastery</em> by Midiastore Editorial. Visit <a href="${CTA_EN}">midiastory-df6a0.web.app/en/</a> for guides in 13 languages.</p>`,
  },
];

function main() {
  fs.mkdirSync(ebookEnDir, { recursive: true });
  execFileSync("powershell", ["-NoProfile", "-ExecutionPolicy", "Bypass", "-File", coverPs1], {
    stdio: "inherit",
    env: {
      ...process.env,
      MIDI_BOOK_COVER_OUT: coverPngPath,
      MIDI_COVER_BADGE: "GLOBAL EDITION • MIDIASTORE SERIES",
      MIDI_COVER_T1: "ANDROID",
      MIDI_COVER_T2: "STORAGE",
      MIDI_COVER_T3: "MASTERY",
      MIDI_COVER_SUB: "Reclaim Real Gigabytes Byte-by-Byte: SHA-256 Deduplication, EXIF Privacy & Offline Productivity.",
      MIDI_COVER_HDR: "3-PILLAR ON-DEVICE METHODOLOGY",
      MIDI_COVER_P1_L: "01  BYTE-EXACT SHA-256 DEDUPLICATION",
      MIDI_COVER_P1_V: "Zero risk to originals",
      MIDI_COVER_P2_L: "02  EXIF / GPS PRIVACY & SCOPED STORAGE",
      MIDI_COVER_P2_V: "100% local on-device",
      MIDI_COVER_P3_L: "03  WHATSAPP, >10 MB CLIPS & MEDIA STUDIO",
      MIDI_COVER_P3_V: "Offline PDF + QR + M4A",
      MIDI_COVER_FOOT: "Companion guide for Midiastore & Android 10–16 privacy standards",
      MIDI_COVER_ED: "ENGLISH EDITION • WORLDWIDE",
    },
  });

  const coverBytes = fs.readFileSync(coverPngPath);
  const bytes = writeEpubPackage({
    meta: META,
    coverBytes,
    chapters: CHAPTERS,
    outPath: epubOutPath,
    labels: { cover: "Cover", toc: "Table of Contents" },
  });

  const guideEn = `# Google Play Books Upload Ready — English Global Edition

- **Book EPUB 3.0:** \`docs/ebook-en/Android_Storage_Mastery_Midiastore.epub\` (${(bytes / 1024).toFixed(1)} KB)
- **Cover 1600x2560 PNG:** \`docs/ebook-en/cover-play-books-en-1600x2560.png\`
- **Suggested Global Retail Price:** **$4.99 USD** (auto-converted across US, UK, CA, AU, EU, IN, JP)
- **Partner Center URL:** https://play.google.com/books/publish (owner login & tax/bank profile required)
- **SEO Categories:** Computers / Operating Systems / Android · Computers / Security / Privacy
`;
  fs.writeFileSync(path.join(ebookEnDir, "PUBLISH_GUIDE_EN.md"), guideEn, "utf8");
  console.log(`✅ EN EPUB ready: ${epubOutPath} (${(bytes / 1024).toFixed(1)} KB)`);
}

main();
