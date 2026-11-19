const fs = require('fs');
const path = require('path');
const vm = require('vm');
const root = path.join(__dirname, 'app', 'src', 'main', 'assets', 'stages');
for (const file of fs.readdirSync(root).filter(f=>f.endsWith('.html')).sort()) {
  const text = fs.readFileSync(path.join(root, file), 'utf8');
  const scripts = [...text.matchAll(/<script([^>]*)>([\s\S]*?)<\/script>/gi)].map(m=>({attrs:m[1],src:m[2]}));
  if (!scripts.length) { console.log(file + ': no scripts'); continue; }
  let ok = true;
  for (let i = 0; i < scripts.length; i++) {
    const {attrs, src} = scripts[i];
    if (/type\s*=\s*['"]module['"]/i.test(attrs)) continue;
    try {
      new vm.Script(src, { filename: file + ':script' + i });
    } catch (e) {
      ok = false;
      console.log(file + ': script ' + i + ' ERROR');
      console.log(e.stack || e.message);
    }
  }
  if (ok) console.log(file + ': OK');
}
