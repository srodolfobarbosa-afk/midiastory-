import fs from 'fs';

const xml = fs.readFileSync('ui_debug2.xml', 'utf8');

const regex = /<node[^>]*text="([^"]*)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"/g;
let m;
while ((m = regex.exec(xml)) !== null) {
  if (m[1].includes('2026-10-06') || m[1].includes('Marcar Todos') || m[1].includes('Selecionados')) {
    console.log(m[1], 'bounds:', m[2], m[3], m[4], m[5]);
  }
}

const checkRegex = /<node[^>]*class="android\.widget\.CheckBox"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"/g;
while ((m = checkRegex.exec(xml)) !== null) {
  console.log('CheckBox bounds:', m[1], m[2], m[3], m[4]);
}
