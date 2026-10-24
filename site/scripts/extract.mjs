// Reads the app's own data into JSON for the preview site, so the site never keeps a second copy:
//   Text.java       -> src/data/strings.json   (every `ar ? … : …` string, as {ar, en} templates)
//   Prefs.CITIES    -> src/data/cities.json
//   Prefs.METHODS   -> src/data/methods.json
//   Radio.STATIONS  -> src/data/stations.json
//   res/font/*.ttf  -> src/assets/fonts/
//   docs/screenshots -> public/screenshots/   (social preview images)
// Templates use {param} for the Java method's parameters; "d": true means the app passes it through d()
// (Eastern Arabic digits when that setting is on). Methods with other logic are listed as skipped and are
// written by hand in src/lib/text.ts.
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '../..');
const src = (f) => readFileSync(resolve(root, 'app/src/main/java/tv/afnon/home', f), 'utf8');
const out = resolve(root, 'site/src/data');
mkdirSync(out, { recursive: true });
const write = (name, data) => {
  writeFileSync(resolve(out, name), JSON.stringify(data, null, 2) + '\n');
  console.log(`wrote site/src/data/${name}`);
};

const STR = String.raw`"((?:[^"\\]|\\.)*)"`;
const unescape = (s) => JSON.parse(`"${s}"`);

// ---------------------------------------------------------------- Text.java

/** `"a" + x + "b"` (params only) -> "a{x}b"; null if the expression does anything else. */
function template(expr, params) {
  let e = expr.trim();
  let d = false;
  const m = /^d\((.*)\)$/s.exec(e);
  if (m) { d = true; e = m[1].trim(); }
  const parts = [];
  const re = new RegExp(String.raw`\s*(?:${STR}|(\w+))\s*(\+|$)`, 'y');
  let pos = 0;
  while (pos < e.length) {
    re.lastIndex = pos;
    const p = re.exec(e);
    if (!p) return null;
    if (p[1] !== undefined) parts.push(unescape(p[1]));
    else if (params.includes(p[2])) parts.push(`{${p[2]}}`);
    else return null;
    pos = re.lastIndex;
    if (p[3] === '') break;
  }
  if (pos < e.length) return null;
  return { s: parts.join(''), d };
}

function splitTernary(body) {
  // body: `ar ? X : Y` with X, Y free of top-level ?: (string literals may contain ':').
  if (!body.startsWith('ar ?')) return null;
  let depth = 0, inStr = false;
  for (let i = 4; i < body.length; i++) {
    const c = body[i];
    if (inStr) { if (c === '\\') i++; else if (c === '"') inStr = false; continue; }
    if (c === '"') inStr = true;
    else if (c === '(' || c === '{') depth++;
    else if (c === ')' || c === '}') depth--;
    else if (c === '?' && depth === 0) return null;
    else if (c === ':' && depth === 0) return [body.slice(4, i).trim(), body.slice(i + 1).trim()];
  }
  return null;
}

const text = src('Text.java');
const strings = {};
const skipped = [];
const methodRe = /^ {4}(String(?:\[\])?) (\w+)\(([^)]*)\) \{\s*(?:return ([^;]*);\s*\})?/gm;
for (const m of text.matchAll(methodRe)) {
  const [, type, name, rawParams, body] = m;
  const params = rawParams.split(',').map((p) => p.trim().split(/\s+/).pop()).filter(Boolean);
  const tern = body && splitTernary(body.replace(/\s+/g, ' ').trim());
  if (!tern) { skipped.push(name); continue; }
  if (type === 'String[]') {
    const arr = (e) => [...e.matchAll(new RegExp(STR, 'g'))].map((x) => unescape(x[1]));
    if (!/^new String\[\] \{/.test(tern[0])) { skipped.push(name); continue; }
    strings[name] = { ar: arr(tern[0]), en: arr(tern[1]) };
    continue;
  }
  const ar = template(tern[0], params), en = template(tern[1], params);
  if (!ar || !en) { skipped.push(name); continue; }
  strings[name] = { ar: ar.s, en: en.s, ...(ar.d || en.d ? { d: true } : {}) };
}
write('strings.json', strings);
console.log(`  ${Object.keys(strings).length} strings; by hand in text.ts: ${skipped.join(', ')}`);

// ---------------------------------------------------------------- Prefs: cities and methods

const prefs = src('Prefs.java');
const consts = Object.fromEntries(
  [...prefs.matchAll(/static final Method (\w+) = Method\.(\w+);/g)].map((m) => [m[1], m[2]]),
);
const cityRe = new RegExp(
  String.raw`new City\(${STR}, ${STR}, ${STR}, ${STR}, ${STR}, ([-\d.]+), ([-\d.]+), (?:Method\.)?(\w+)(?:, (\w+))?\)`, 'g');
const cities = [...prefs.matchAll(cityRe)].map((m) => ({
  id: unescape(m[1]), ar: unescape(m[2]), en: unescape(m[3]), countryAr: unescape(m[4]), countryEn: unescape(m[5]),
  lat: Number(m[6]), lng: Number(m[7]), method: consts[m[8]] ?? m[8], hanafi: m[9] === 'HANAFI',
}));
if (cities.length < 10) throw new Error('Prefs.CITIES not found');
write('cities.json', cities);

// Location's first step: each region's countries (English names), in the order of Text.regions().
const regionsBlock = /String\[\]\[\] REGIONS = \{([\s\S]*?)\n {4}\};/.exec(prefs)[1];
const regions = [...regionsBlock.matchAll(/\{([^}]*)\}/g)].map((m) => [...m[1].matchAll(new RegExp(STR, 'g'))].map((x) => unescape(x[1])));
const lost = [...new Set(cities.map((c) => c.countryEn))].filter((n) => !regions.some((r) => r.includes(n)));
if (lost.length) throw new Error(`Prefs.REGIONS is missing ${lost.join(', ')}`);
write('regions.json', regions);

const methodsBlock = /Method\[\] METHODS = \{([^}]*)\}/.exec(prefs)[1];
write('methods.json', [...methodsBlock.matchAll(/Method\.(\w+)/g)].map((m) => m[1]));

// ---------------------------------------------------------------- Radio.STATIONS

const radio = src('Radio.java');
const vars = Object.fromEntries([...radio.matchAll(new RegExp(String.raw`(\w+) = ${STR}`, 'g'))].map((m) => [m[1], unescape(m[2])]));
/** A Java string expression from string literals and the constants above, e.g. QURAN_AR + " 2". */
const value = (e) => e.split('+').map((p) => {
  p = p.trim();
  const lit = new RegExp(`^${STR}$`).exec(p);
  if (lit) return unescape(lit[1]);
  if (p in vars) return vars[p];
  throw new Error(`Radio: can't read ${p}`);
}).join('');
const block = /Station\[\] STATIONS = \{([\s\S]*?)\n\s*\};/.exec(radio)[1];
/** Each `new Station(...)` argument list, split on top-level commas (outside strings and brackets). */
function calls(code, head) {
  const res = [];
  for (let at = code.indexOf(head); at !== -1; at = code.indexOf(head, at + 1)) {
    const args = []; let cur = '', depth = 0, inStr = false;
    for (let i = at + head.length; i < code.length; i++) {
      const c = code[i];
      if (inStr) { cur += c; if (c === '\\') cur += code[++i]; else if (c === '"') inStr = false; continue; }
      if (c === '"') inStr = true;
      if (c === '(') depth++;
      if (c === ')' && depth-- === 0) { args.push(cur.trim()); break; }
      if (c === ',' && depth === 0) { args.push(cur.trim()); cur = ''; } else cur += c;
    }
    res.push(args);
  }
  return res;
}
const stations = calls(block, 'new Station(').map((args) => {
  const [id, ar, en, whereAr, whereEn, url] = args.slice(0, 6).map(value);
  return { id, ar, en, whereAr, whereEn, url };
});
if (!stations.length) throw new Error('Radio.STATIONS not found');
write('stations.json', stations);

// ---------------------------------------------------------------- fonts (the app's own files)

import { readdirSync, copyFileSync } from 'node:fs';
const fontsIn = resolve(root, 'app/src/main/res/font');
const fontsOut = resolve(root, 'site/src/assets/fonts');
mkdirSync(fontsOut, { recursive: true });
for (const f of readdirSync(fontsIn)) copyFileSync(resolve(fontsIn, f), resolve(fontsOut, f));
console.log('copied app fonts to site/src/assets/fonts');

// ---------------------------------------------------------------- README screenshots, for social previews

const shotsIn = resolve(root, 'docs/screenshots');
const shotsOut = resolve(root, 'site/public/screenshots');
mkdirSync(shotsOut, { recursive: true });
for (const f of readdirSync(shotsIn)) copyFileSync(resolve(shotsIn, f), resolve(shotsOut, f));
console.log('copied docs/screenshots to site/public/screenshots');

// ---------------------------------------------------------------- Adhans.VOICES (Settings: adhan voice lists)

const adhans = src('Adhans.java');
const voicesBlock = /String\[\]\[\] VOICES = \{([\s\S]*?)\n\s*\};/.exec(adhans)[1];
const voices = [...voicesBlock.matchAll(new RegExp(String.raw`\{${STR}, ${STR}, ${STR}\}`, 'g'))]
  .map((m) => ({ id: unescape(m[1]), ar: unescape(m[2]), en: unescape(m[3]) }));
if (!voices.length) throw new Error('Adhans.VOICES not found');
write('adhans.json', voices);

// ---------------------------------------------------------------- Radio.STATIONS again, with the reciters
// (Station.reciter(id, ar, en) channels and each station's reciter flag; Settings groups the list by it)

const reciterFactory = /static Station reciter\([^)]*\) \{\s*return new Station\(id, ar, en, ([\s\S]*?), true\);/.exec(radio);
const [rWhereAr, rWhereEn, rUrl] = calls(`new Station(id, ar, en, ${reciterFactory[1]}, true)`, 'new Station(').flat().slice(3, 6);
const literal = (e) => value(e.replace(/\s*\+\s*id\s*$/, ''));
const allStations = [];
for (const m of block.matchAll(/new Station\(|Station\.reciter\(/g)) {
  const isReciter = m[0].startsWith('Station.reciter');
  const args = calls(block.slice(m.index), m[0])[0];
  if (isReciter) {
    const [id, ar, en] = args.map(value);
    allStations.push({ id, ar, en, whereAr: value(rWhereAr), whereEn: value(rWhereEn), url: literal(rUrl) + id, reciter: true });
  } else {
    const [id, ar, en, whereAr, whereEn, url] = args.slice(0, 6).map(value);
    allStations.push({ id, ar, en, whereAr, whereEn, url, reciter: args[6] === 'true' });
  }
}
write('stations.json', allStations);
