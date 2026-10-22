// Browser side: prefs saved in this browser, and a once-a-second loop that re-renders the screen model into
// the page's [data-k] (innerHTML) and [data-a] (attributes) elements.
import { DEFAULT_PREFS, type Prefs } from '../lib/data';
import { makeCtx, type Ctx, type Model } from '../lib/ctx';
import { shiftFor } from '../lib/prayer';
import type { Lang } from '../lib/text';
import { patchModel } from '../mocks/mocks';

const KEY = 'afnon-prefs';

export function loadPrefs(): Prefs {
  try {
    return { ...DEFAULT_PREFS, ...JSON.parse(localStorage.getItem(KEY) ?? '{}') };
  } catch {
    return { ...DEFAULT_PREFS };
  }
}

export function savePrefs(p: Prefs) {
  try { localStorage.setItem(KEY, JSON.stringify(p)); } catch { /* private mode: this visit only */ }
}

export function bind(root: ParentNode, m: Model) {
  for (const el of root.querySelectorAll<HTMLElement>('[data-k]')) {
    const v = m.html[el.dataset.k!];
    if (v !== undefined && el.innerHTML !== v) el.innerHTML = v;
  }
  for (const el of root.querySelectorAll<HTMLElement>('[data-a]')) {
    const attrs = m.attr?.[el.dataset.a!];
    if (attrs) for (const [k, v] of Object.entries(attrs)) if (el.getAttribute(k) !== v) el.setAttribute(k, v);
  }
}

/**
 * Runs `model` every second for this page's language and test mode (`data-mode` on the stage), the clock
 * shifted as the app's test intents do. Returns a function that re-renders at once (after a prefs change).
 */
export function live(model: (c: Ctx) => Model, onTick?: (c: Ctx) => void) {
  const stage = document.querySelector<HTMLElement>('.tv')!;
  const lang = document.documentElement.lang as Lang;
  const mode = stage.dataset.mode || undefined;
  let prefs = loadPrefs();
  let shift = mode ? shiftFor(mode, prefs) : 0;
  // Dev-only UI mocks (/mocks/): flags on the stage, and the mock's changes to the screen model.
  const mock = import.meta.env.DEV ? (stage.dataset.mock ?? '').split(' ').filter(Boolean) : [];
  const render = () => {
    const c = makeCtx(lang, prefs, new Date(Date.now() + shift), mode, mock);
    bind(stage, import.meta.env.DEV && mock.length ? patchModel(stage.dataset.screen!, c, model(c)) : model(c));
    onTick?.(c);
  };
  render();
  setInterval(render, 1000);
  return (p?: Prefs) => {
    if (p) { prefs = p; savePrefs(p); shift = mode ? shiftFor(mode, prefs) : 0; }
    render();
  };
}
