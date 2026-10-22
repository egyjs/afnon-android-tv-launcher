// Quranic text is never typed by hand. It is fetched from Tanzil (api.alquran.cloud, one edition for all) into
// afnon-mihrab/quran.json, and every place that shows it is generated from that file:
//   app/src/main/java/tv/afnon/home/Quran.java   (Java constants)
//   text between <!--quran ID br|space|lineN-->…<!--/quran--> markers in README.md and afnon-mihrab/design/*.html
// The site imports afnon-mihrab/quran.json directly.
//   node scripts/quran.mjs           write the outputs from quran.json (offline)
//   node scripts/quran.mjs --fetch   fetch every verse again, then write
//   node scripts/quran.mjs --check   write nothing; fail if an output differs from quran.json (for CI)
// A verse is a whole ayah or its last `words` words; `lines` breaks it for the arch (word counts per line).
import { readFileSync, writeFileSync, readdirSync } from 'node:fs';
import { dirname, resolve, relative } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '../..');
const JSON_PATH = resolve(root, 'afnon-mihrab/quran.json');
const JAVA_PATH = resolve(root, 'app/src/main/java/tv/afnon/home/Quran.java');
const EDITION = 'quran-simple-enhanced'; // Tanzil "Simple Enhanced": full harakat, plain letters; Amiri draws it
const VERSES = {
  // The arch's crown on every screen: An-Nisa 4:103, its last seven words.
  crown: { ref: '4:103', words: 7, lines: [3, 2, 2] },
  // Site only (app: false): a short, a middle and a long verse for the verse-of-the-day mock (src/mocks).
  sampleS: { ref: '94:6', app: false },
  sampleM: { ref: '13:28', app: false },
  sampleL: { ref: '3:8', app: false },
};
const check = process.argv.includes('--check');
const HEADER = 'Generated from afnon-mihrab/quran.json by site/scripts/quran.mjs. Do not edit: Quranic text is never typed by hand.';

async function fetchAll() {
  const out = { source: `https://api.alquran.cloud/v1/ayah/{ref}/${EDITION}`, edition: EDITION, verses: {} };
  for (const [id, v] of Object.entries(VERSES)) {
    const res = await fetch(`https://api.alquran.cloud/v1/ayah/${v.ref}/${EDITION}`);
    if (!res.ok) throw new Error(`${v.ref}: HTTP ${res.status}`);
    const d = (await res.json()).data;
    let words = d.text.split(' '); // exactly as served: no normalising, no reordering of marks
    if (v.words) words = words.slice(-v.words);
    const lines = [];
    let at = 0;
    for (const n of v.lines ?? [words.length]) { lines.push(words.slice(at, at + n).join(' ')); at += n; }
    if (at !== words.length) throw new Error(`${id}: lines ${v.lines} do not cover ${words.length} words`);
    out.verses[id] = { ref: v.ref, surah: d.surah.name, surahEn: d.surah.englishName, ayah: d.numberInSurah, lines };
    console.log(`${id}: ${v.ref} ${d.surah.englishName}, ${words.length} words`);
  }
  writeFileSync(JSON_PATH, JSON.stringify(out, null, 1) + '\n');
}

const javaString = (s) => '"' + s.replace(/\\/g, '\\\\').replace(/"/g, '\\"').replace(/\n/g, '\\n') + '"';

function outputs(q) {
  const files = new Map();
  const consts = Object.entries(q.verses).filter(([id]) => VERSES[id]?.app !== false).flatMap(([id, v]) => {
    const name = id.toUpperCase();
    return [
      `    /** ${v.surahEn} ${v.ref}, one arch line per \\n. */`,
      `    static final String ${name} = ${javaString(v.lines.join('\n'))};`,
      `    static final String ${name}_REF = ${javaString(v.ref)};`,
    ];
  });
  files.set(JAVA_PATH, `// ${HEADER}\npackage tv.afnon.home;\n\n/** Quranic text, exactly as fetched from Tanzil (${q.edition}). */\nfinal class Quran {\n    private Quran() {}\n\n${consts.join('\n')}\n}\n`);

  const marked = [resolve(root, 'README.md'), ...readdirSync(resolve(root, 'afnon-mihrab/design')).filter((f) => f.endsWith('.html')).map((f) => resolve(root, 'afnon-mihrab/design', f))];
  for (const path of marked) {
    const before = readFileSync(path, 'utf8');
    const after = before.replace(/<!--quran (\w+) (br|space|line\d)-->[\s\S]*?<!--\/quran-->/g, (_, id, how) => {
      const v = q.verses[id];
      if (!v) throw new Error(`${relative(root, path)}: no verse "${id}" in quran.json`);
      const text = how.startsWith('line') ? v.lines[+how.slice(4) - 1] : v.lines.join(how === 'br' ? '<br>' : ' ');
      if (text === undefined) throw new Error(`${relative(root, path)}: verse "${id}" has no ${how}`);
      return `<!--quran ${id} ${how}-->${text}<!--/quran-->`;
    });
    if (after !== before || check) files.set(path, after);
  }
  return files;
}

if (process.argv.includes('--fetch')) await fetchAll();
const q = JSON.parse(readFileSync(JSON_PATH, 'utf8'));
let stale = 0;
for (const [path, text] of outputs(q)) {
  let current = '';
  try { current = readFileSync(path, 'utf8'); } catch { /* new */ }
  if (current === text) continue;
  if (check) { console.error(`out of date: ${relative(root, path)} (run \`npm run quran\` in site/)`); stale++; }
  else { writeFileSync(path, text); console.log(`wrote ${relative(root, path)}`); }
}
if (stale) process.exit(1);
