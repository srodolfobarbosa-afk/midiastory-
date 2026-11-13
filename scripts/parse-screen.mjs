import fs from "fs";

const file = process.argv[2] || "test_window_dump.xml";
if (fs.existsSync(file)) {
  const xml = fs.readFileSync(file, "utf8");
  const nodes = [...xml.matchAll(/text="([^"]+)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"/g)].map(
    (m) => ({
      text: m[1],
      x: Math.round((parseInt(m[2]) + parseInt(m[4])) / 2),
      y: Math.round((parseInt(m[3]) + parseInt(m[5])) / 2),
      bounds: `[${m[2]},${m[3]}][${m[4]},${m[5]}]`,
    })
  );
  console.log("Nós interativos:", JSON.stringify(nodes, null, 2));
}
