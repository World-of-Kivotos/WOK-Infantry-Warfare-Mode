// Build the single-file helmet preview page.
//   node build.mjs                                   -> helmet-preview.html with every helmet
//   node build.mjs --only 6b47 --out preview-6b47.html -> page with just that helmet (for isolated review)
// helmets/<key>.meta.json describes every shipped helmet; helmets/<key>.js (optional) registers ARMORS.<key>.
import fs from 'node:fs';
const here = new URL('./', import.meta.url);
const r = f => fs.readFileSync(new URL(f, here), 'utf8').replace(/^﻿/, '');
const arg = k => { const i = process.argv.indexOf(k); return i > 0 ? process.argv[i + 1] : null; };
const only = arg('--only'), out = arg('--out') || 'helmet-preview.html';

const esc = s => String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/"/g, '&quot;');
const metas = fs.readdirSync(new URL('./helmets/', here)).filter(f => f.endsWith('.meta.json'))
  .map(f => JSON.parse(r('./helmets/' + f)))
  .filter(m => !only || m.key === only)
  .sort((a, b) => a.order - b.order || a.key.localeCompare(b.key));
if (!metas.length) throw new Error('no helmet metas' + (only ? ' for ' + only : ''));
const hasDesign = m => fs.existsSync(new URL(`./helmets/${m.key}.js`, here));
// every design is included even on a single-helmet page: a design may build on another one (ARMORS['6b47'](mode))
const allDesigns = fs.readdirSync(new URL('./helmets/', here)).filter(f => /^[^_].*\.js$/.test(f)).sort();
const src = [r('./helmets/_kit.js'), ...allDesigns.map(f => r('./helmets/' + f))].join('\n');

const buttons = metas.map(m => `      <button type="button" id="b-${m.key}" data-armor="${m.key}"${hasDesign(m) ? ' data-new' : ''}>${esc(m.name)}<span class="tier">${esc(m.tier)}</span></button>`).join('\n');
const refs = metas.map((m, i) => `    <div class="note" data-for="${m.key}"${i ? ' hidden' : ''}>
      <h3>${esc(m.title)}</h3>
      <p class="hint">${m.refs.length ? '原图：' + m.refs.map(x => `<a href="${esc(x.url)}" target="_blank" rel="noopener">${esc(x.label)}</a>`).join(' · ') + '（tarkov.dev 的游戏内渲染图）' : '没有找到塔科夫原物'}</p>
      <ul>
${m.notes.map(n => `        <li>${n}</li>`).join('\n')}
      </ul>
    </div>`).join('\n');

const viewer = r('./viewer.js');
const nl = viewer.indexOf('\n');
const imp = viewer.slice(0, nl), rest = viewer.slice(nl + 1);
const script = [
  imp,
  r('./designs.js').replace(/^export /gm, ''),
  src,
  `const PAGE_KEYS = ${JSON.stringify(metas.map(m => m.key))};`,
  r('./current_geo.js').replace(/^export /gm, ''),
  rest,
].join('\n');
const html = r('./template.html').replace('<!--ARMOR_BUTTONS-->', () => buttons).replace('<!--ARMOR_REFS-->', () => refs).replace('/*SCRIPT*/', () => script);
fs.writeFileSync(new URL('./' + out, here), html);
console.log('ok', out, metas.map(m => m.key + (hasDesign(m) ? '*' : '')).join(','), script.length);
