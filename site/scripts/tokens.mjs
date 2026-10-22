// Builds every design-token output from afnon-mihrab/tokens.json, the one source of truth:
//   app/src/main/java/tv/afnon/home/Tokens.java   (Java constants, names as the app uses them)
//   app/src/main/res/values/colors.xml            (window background etc.)
//   site/src/styles/tokens.css                    (CSS custom properties for the preview site)
// Night mode: afnon-mihrab/tokens.night.json overrides colours by the same paths (references follow), the usual
// Style Dictionary theme. Tokens.java gets both palettes, its colours as fields that Tokens.use(night) switches,
// and colors.xml the night colours that differ, as night_<name>.
// `--check` writes nothing and fails if the app's committed outputs differ (for CI).
import StyleDictionary from 'style-dictionary';
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '../..');
const check = process.argv.includes('--check');
const HEADER = 'Generated from afnon-mihrab/tokens.json by site/scripts/tokens.mjs. Do not edit: run `npm run tokens` in site/.';

const ext = (t) => t.$extensions?.['tv.afnon'] ?? {};
// Colours drop their group name (color.lapis-900 -> LAPIS_900); everything else keeps its full path.
const path = (t) => (t.path[0] === 'color' ? t.path.slice(1) : t.path);
const constName = (t) => ext(t).java ?? path(t).join('_').replace(/-/g, '_').toUpperCase() + (t.$type === 'duration' ? '_MS' : '');

/** "#AARRGGBB" from any CSS colour Style Dictionary resolved, as a Java int literal. */
const javaColor = (hex8android) => '0x' + hex8android.slice(1).toUpperCase();
const durationMs = (v) => {
  const m = /^([\d.]+)(ms|s|min)$/.exec(v);
  if (!m) throw new Error(`bad duration ${v}`);
  return Number(m[1]) * { ms: 1, s: 1000, min: 60000 }[m[2]];
};

/** Night values by Java name (colours only), filled in from tokens.night.json before the formats run. */
const night = new Map();
const nightAndroid = new Map();

function javaLine(t) {
  const v = t.$value;
  switch (t.$type) {
    case 'color':
      // Not final: Tokens.use switches the palette, and every reader must see the field, not an inlined copy.
      return `static int ${constName(t)} = ${javaColor(v)};`;
    case 'dimension':
      return `static final float ${constName(t)} = ${parseFloat(v)}f;`;
    case 'number':
      return `static final float ${constName(t)} = ${v}f;`;
    case 'duration': {
      const ms = durationMs(v);
      if (ext(t).unit === 'min') return `static final int ${constName(t)} = ${ms / 60000};`;
      return `static final long ${constName(t)} = ${ms};`;
    }
    default:
      return null; // fontFamily, cubicBezier: not used as Java constants
  }
}

StyleDictionary.registerFormat({
  name: 'afnon/java',
  format: ({ dictionary }) => {
    const seen = new Set();
    const body = dictionary.allTokens
      .map((t) => {
        const line = javaLine(t);
        if (!line) return null;
        if (seen.has(constName(t))) throw new Error(`duplicate Java name ${constName(t)}`);
        seen.add(constName(t));
        const doc = `${t.path.join('.')}${t.$description ? ': ' + t.$description : ''}`;
        return `    /** ${doc.replace(/\*\//g, '* /')} */\n    ${line}`;
      })
      .filter(Boolean)
      .join('\n');
    const changes = dictionary.allTokens
      .filter((t) => t.$type === 'color' && night.get(constName(t)) !== t.$value)
      .map((t) => `        ${constName(t)} = night ? ${javaColor(night.get(constName(t)))} : ${javaColor(t.$value)};`)
      .join('\n');
    return `package tv.afnon.home;

// ${HEADER}

/**
 * Design tokens in px at 1920×1080. Multiply lengths by {@link Ui#px}. Colours are the day palette until
 * {@link #use} switches them to night mode's (afnon-mihrab/tokens.night.json).
 */
final class Tokens {
    private Tokens() {}

${body}

    /** Whether the colours above are night mode's. */
    static boolean night;

    /**
     * The day or the night palette for every colour above. Views take their colours when they are built, so a
     * screen in front rebuilds after a switch; the others build in the new palette when they next open.
     */
    static void use(boolean night) {
        if (Tokens.night == night) return;
        Tokens.night = night;
${changes}
    }
}
`;
  },
});

StyleDictionary.registerFormat({
  name: 'afnon/android-colors',
  format: ({ dictionary }) => {
    const name = (t) => path(t).join('_').replace(/-/g, '_');
    const colors = dictionary.allTokens.filter((t) => t.path[0] === 'color');
    const rows = colors.map((t) => `    <color name="${name(t)}">${t.$value}</color>`).join('\n');
    const nightRows = colors
      .filter((t) => nightAndroid.get(name(t)) !== t.$value)
      .map((t) => `    <color name="night_${name(t)}">${nightAndroid.get(name(t))}</color>`)
      .join('\n');
    return `<?xml version="1.0" encoding="utf-8"?>\n<!-- ${HEADER} -->\n<resources>\n${rows}\n`
      + `    <!-- Night mode (tokens.night.json): the colours that differ. -->\n${nightRows}\n</resources>\n`;
  },
});

const DAY = resolve(root, 'afnon-mihrab/tokens.json');
const NIGHT = resolve(root, 'afnon-mihrab/tokens.night.json');

// Every night token must override a day one: a misspelt path would otherwise add a colour nothing reads.
const leaves = (o, at = []) => Object.entries(o).flatMap(([k, v]) =>
  k.startsWith('$') || typeof v !== 'object' ? [] : '$value' in v ? [[...at, k].join('.')] : leaves(v, [...at, k]));
const dayPaths = new Set(leaves(JSON.parse(readFileSync(DAY, 'utf8'))));
const unknown = leaves(JSON.parse(readFileSync(NIGHT, 'utf8'))).filter((p) => !dayPaths.has(p));
if (unknown.length) {
  console.error('tokens.night.json has paths that tokens.json lacks:\n  ' + unknown.join('\n  '));
  process.exit(1);
}

// The night theme: tokens.json underneath, tokens.night.json on top; references resolve against the merge.
const sdNight = new StyleDictionary({
  include: [DAY],
  source: [NIGHT],
  log: { verbosity: 'silent', warnings: 'error' },
  platforms: {
    java: { transforms: ['attribute/cti', 'name/constant', 'color/hex8android'] },
    android: { transforms: ['attribute/cti', 'name/snake', 'color/hex8android'] },
  },
});
for (const t of (await sdNight.getPlatformTokens('java')).allTokens) if (t.$type === 'color') night.set(constName(t), t.$value);
for (const t of (await sdNight.getPlatformTokens('android')).allTokens) {
  if (t.path[0] === 'color') nightAndroid.set(path(t).join('_').replace(/-/g, '_'), t.$value);
}

const sd = new StyleDictionary({
  source: [DAY],
  log: { verbosity: 'silent', warnings: 'error' },
  platforms: {
    css: {
      transforms: ['attribute/cti', 'name/kebab', 'color/css', 'fontFamily/css', 'cubicBezier/css'],
      buildPath: resolve(root, 'site/src/styles') + '/',
      files: [{ destination: 'tokens.css', format: 'css/variables', options: { outputReferences: true, fileHeader: () => [HEADER] } }],
    },
    java: {
      transforms: ['attribute/cti', 'name/constant', 'color/hex8android'],
      buildPath: resolve(root, 'app/src/main/java/tv/afnon/home') + '/',
      files: [{ destination: 'Tokens.java', format: 'afnon/java' }],
    },
    android: {
      transforms: ['attribute/cti', 'name/snake', 'color/hex8android'],
      buildPath: resolve(root, 'app/src/main/res/values') + '/',
      files: [{ destination: 'colors.xml', format: 'afnon/android-colors' }],
    },
  },
});

let stale = [];
// The CSS is gitignored and rebuilt with the site, so --check covers only the app's committed files.
for (const platform of check ? ['java', 'android'] : ['css', 'java', 'android']) {
  for (const { output, destination } of await sd.formatPlatform(platform)) {
    let old = null;
    try { old = readFileSync(destination, 'utf8'); } catch {}
    if (old === output) continue;
    if (check) { stale.push(destination); continue; }
    mkdirSync(dirname(destination), { recursive: true });
    writeFileSync(destination, output);
    console.log('wrote', destination.slice(root.length + 1));
  }
}
if (stale.length) {
  console.error('Out of date with tokens.json (run `npm run tokens` in site/):\n  ' + stale.join('\n  '));
  process.exit(1);
}
