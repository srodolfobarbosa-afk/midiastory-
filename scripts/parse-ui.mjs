import fs from 'node:fs';

const filename = process.argv[2] || 'ui.xml';
const xml = fs.readFileSync(filename, 'utf8');
const regex = /<node[^>]*text="([^"]*)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"[^>]*>/g;
let match;
while ((match = regex.exec(xml)) !== null) {
  if (match[1]) {
    const x = Math.round((parseInt(match[2]) + parseInt(match[4])) / 2);
    const y = Math.round((parseInt(match[3]) + parseInt(match[5])) / 2);
    console.log(`"${match[1]}" -> Center: (${x}, ${y}) | Bounds: [${match[2]},${match[3]}][${match[4]},${match[5]}]`);
  }
}

