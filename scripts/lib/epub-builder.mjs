import fs from "node:fs";
import zlib from "node:zlib";

export function escapeXml(str) {
  return str
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&apos;");
}

export function crc32(buf) {
  let c = 0xffffffff;
  for (let i = 0; i < buf.length; i++) {
    c ^= buf[i];
    for (let k = 0; k < 8; k++) {
      c = (c >>> 1) ^ (0xedb88320 & -(c & 1));
    }
  }
  return (c ^ 0xffffffff) >>> 0;
}

export function buildEpubZip(entries) {
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
    localHdr.writeUInt16LE(0x0800, 6);
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

export const EPUB_BOOK_CSS = `
body { font-family: Georgia, "Times New Roman", serif; line-height: 1.68; margin: 4% 6%; font-size: 1.02em; }
h1, h2, h3 { font-family: system-ui, -apple-system, "Segoe UI", sans-serif; line-height: 1.25; margin-top: 1.5em; margin-bottom: 0.6em; }
h1 { font-size: 1.65em; border-bottom: 2px solid #2b8a78; padding-bottom: 0.35em; }
h2 { font-size: 1.25em; color: #1d6b5d; }
p { margin: 0.75em 0; text-align: justify; }
ul, ol { margin: 0.8em 0 1em 1.4em; }
li { margin: 0.45em 0; }
.callout { border-left: 4px solid #2b8a78; background-color: rgba(43, 138, 120, 0.08); padding: 0.9em 1.1em; margin: 1.2em 0; border-radius: 6px; }
.callout strong { display: block; margin-bottom: 0.35em; font-family: system-ui, -apple-system, sans-serif; }
.chapter-tag { font-family: system-ui, -apple-system, sans-serif; font-size: 0.8em; text-transform: uppercase; letter-spacing: 0.1em; color: #2b8a78; font-weight: bold; }
table { width: 100%; border-collapse: collapse; margin: 1.2em 0; font-size: 0.92em; }
th, td { border: 1px solid #8aa0b8; padding: 0.55em 0.7em; text-align: left; }
th { background-color: rgba(43, 138, 120, 0.15); font-family: system-ui, -apple-system, sans-serif; }
.cover-page { text-align: center; margin: 0; padding: 0; }
.cover-page img { max-width: 100%; max-height: 98vh; }
`;

export function writeEpubPackage({ meta, coverBytes, chapters, outPath, labels }) {
  const wrap = (title, bodyHtml) => `<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops" xml:lang="${meta.lang}" lang="${meta.lang}">
<head>
  <meta charset="utf-8" />
  <title>${escapeXml(title)}</title>
  <link rel="stylesheet" type="text/css" href="styles/book.css" />
</head>
<body>
${bodyHtml}
</body>
</html>`;

  const containerXml = `<?xml version="1.0" encoding="UTF-8"?>
<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
  <rootfiles>
    <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml" />
  </rootfiles>
</container>`;

  const coverXhtml = wrap(
    labels.cover,
    `<div class="cover-page"><img src="images/cover.png" alt="${escapeXml(meta.title)}" /></div>`
  );

  const navXhtml = `<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops" xml:lang="${meta.lang}" lang="${meta.lang}">
<head>
  <meta charset="utf-8" />
  <title>${escapeXml(labels.toc)}</title>
  <link rel="stylesheet" type="text/css" href="styles/book.css" />
</head>
<body>
  <nav epub:type="toc" id="toc">
    <h1>${escapeXml(labels.toc)}</h1>
    <ol>
      <li><a href="cover.xhtml">${escapeXml(labels.cover)}</a></li>
${chapters.map((c) => `      <li><a href="${c.file}">${escapeXml(c.title)}</a></li>`).join("\n")}
    </ol>
  </nav>
</body>
</html>`;

  const tocNcx = `<?xml version="1.0" encoding="UTF-8"?>
<ncx xmlns="http://www.daisy.org/z3986/2005/ncx/" version="2005-1">
  <head>
    <meta name="dtb:uid" content="${meta.id}" />
    <meta name="dtb:depth" content="1" />
    <meta name="dtb:totalPageCount" content="0" />
    <meta name="dtb:maxPageNumber" content="0" />
  </head>
  <docTitle><text>${escapeXml(meta.title)}</text></docTitle>
  <navMap>
    <navPoint id="nav-cover" playOrder="1">
      <navLabel><text>${escapeXml(labels.cover)}</text></navLabel>
      <content src="cover.xhtml" />
    </navPoint>
${chapters
  .map(
    (c, idx) => `    <navPoint id="nav-${c.id}" playOrder="${idx + 2}">
      <navLabel><text>${escapeXml(c.title)}</text></navLabel>
      <content src="${c.file}" />
    </navPoint>`
  )
  .join("\n")}
  </navMap>
</ncx>`;

  const contentOpf = `<?xml version="1.0" encoding="UTF-8"?>
<package xmlns="http://www.idpf.org/opf" version="3.0" unique-identifier="bookid" xml:lang="${meta.lang}">
  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
    <dc:identifier id="bookid">${meta.id}</dc:identifier>
    <dc:title>${escapeXml(meta.title)}</dc:title>
    <dc:creator>${escapeXml(meta.author)}</dc:creator>
    <dc:publisher>${escapeXml(meta.publisher)}</dc:publisher>
    <dc:language>${meta.lang}</dc:language>
    <dc:description>${escapeXml(meta.subtitle)}</dc:description>
    <meta property="dcterms:modified">${meta.modified}</meta>
    <meta name="cover" content="cover-image" />
  </metadata>
  <manifest>
    <item id="css" href="styles/book.css" media-type="text/css" />
    <item id="cover-image" href="images/cover.png" media-type="image/png" properties="cover-image" />
    <item id="cover" href="cover.xhtml" media-type="application/xhtml+xml" />
    <item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav" />
    <item id="ncx" href="toc.ncx" media-type="application/x-dtbncx+xml" />
${chapters.map((c) => `    <item id="${c.id}" href="${c.file}" media-type="application/xhtml+xml" />`).join("\n")}
  </manifest>
  <spine toc="ncx">
    <itemref idref="cover" />
    <itemref idref="nav" />
${chapters.map((c) => `    <itemref idref="${c.id}" />`).join("\n")}
  </spine>
</package>`;

  const entries = [
    { name: "mimetype", data: "application/epub+zip", store: true },
    { name: "META-INF/container.xml", data: containerXml },
    { name: "OEBPS/content.opf", data: contentOpf },
    { name: "OEBPS/nav.xhtml", data: navXhtml },
    { name: "OEBPS/toc.ncx", data: tocNcx },
    { name: "OEBPS/styles/book.css", data: EPUB_BOOK_CSS },
    { name: "OEBPS/images/cover.png", data: coverBytes, store: true },
    { name: "OEBPS/cover.xhtml", data: coverXhtml },
    ...chapters.map((c) => ({
      name: `OEBPS/${c.file}`,
      data: wrap(c.title, c.body),
    })),
  ];

  const epubBuf = buildEpubZip(entries);
  fs.writeFileSync(outPath, epubBuf);
  return epubBuf.length;
}
