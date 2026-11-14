import fs from 'fs';

const xml = fs.readFileSync('ui_debug2.xml', 'utf8');
const lines = xml.split('<node ');
for (const line of lines) {
  if (line.includes('button') || line.includes('Button') || line.includes('Mover') || line.includes('Excluir')) {
    console.log(line.substring(0, 300));
  }
}
