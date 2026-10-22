// Dev-only: the elements the mocks add to a real screen (src/mocks/mocks.ts lists the mocks). Runs on /mocks/ only.
const stage = document.querySelector<HTMLElement>('.tv');
const flags = (stage?.dataset.mock ?? '').split(' ').filter(Boolean);
const ar = document.documentElement.lang === 'ar';
const has = (f: string) => flags.includes(f);
const svg = (body: string, size = 30) =>
  `<svg width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${body}</svg>`;
const WIFI = '<path d="M2 8.5a15 15 0 0 1 20 0M5 12a10 10 0 0 1 14 0M8.5 15.5a5 5 0 0 1 7 0"/><circle cx="12" cy="19" r="1.2" fill="currentColor"/>';

function popup(cls: string, labels: string[], actions: (() => void)[], onClose: () => void) {
  const box = document.createElement('div');
  box.className = 'mock-pop ' + cls;
  box.setAttribute('role', 'menu');
  const rows = labels.map((label, i) => {
    const b = document.createElement('button');
    b.className = 'ft mock-pop-row';
    b.textContent = label;
    b.id = `${cls}-${i}`;
    b.dataset.hints = JSON.stringify(['OK  ' + label, ar ? 'رجوع  إغلاق' : 'Back  close']);
    b.addEventListener('click', () => actions[i]());
    return b;
  });
  rows.forEach((r, i) => {
    r.dataset.navUp = '#' + rows[Math.max(0, i - 1)].id;
    r.dataset.navDown = '#' + rows[Math.min(rows.length - 1, i + 1)].id;
    r.dataset.navLeft = '#' + r.id;
    r.dataset.navRight = '#' + r.id;
    box.append(r);
  });
  const back = document.createElement('button');
  back.className = 'visually-hidden';
  back.dataset.back = '';
  back.tabIndex = -1;
  back.addEventListener('click', onClose);
  box.append(back);
  return { box, rows };
}

const toast = (s: string) => {
  const out = stage!.querySelector<HTMLElement>('[data-toast-out]');
  if (!out) return;
  out.textContent = s;
  out.hidden = false;
  window.setTimeout(() => (out.hidden = true), 2600);
};
const moveStart = (from: string, to: string) => {
  stage!.querySelector(from)?.removeAttribute('data-focus-start');
  stage!.querySelector(to)?.setAttribute('data-focus-start', '');
};

if (stage && flags.length) {
  // ------------------------------------------------------------ network pill states
  const net = stage.querySelector<HTMLElement>('.pills .pill:not(.radio-pill)');
  if (net && has('wifi-off')) {
    net.classList.add('mock-offline');
    net.innerHTML = svg(WIFI + '<path d="M3 3l18 18"/>') + `<span>${ar ? 'غير متصل' : 'Offline'}</span>`;
  }
  if (net && has('wifi-wired')) {
    net.innerHTML = svg('<rect x="7" y="3" width="10" height="8" rx="1.5"/><path d="M9 11v3M12 11v3M15 11v3M12 14v7M8 21h8"/>') + `<span>${ar ? 'سلكي' : 'Wired'}</span>`;
  }

  if (net && has('wifi-vpn')) {
    net.innerHTML = svg(WIFI) + '<span dir="ltr">Wi-Fi</span>'
      + `<span class="mock-vpn">${svg('<path d="M12 3l7 3v5c0 4.5-3 8-7 10-4-2-7-5.5-7-10V6z"/><path d="M9 12l2 2 4-4"/>', 22)}<span dir="ltr">VPN</span></span>`;
  }

  // ------------------------------------------------------------ radio card: Screensaver instead of Screen off
  if (has('radio-saver')) {
    const old = stage.querySelector<HTMLElement>('[data-screen-off]')!;
    const btn = old.cloneNode(false) as HTMLElement; // a clone drops the Screen-off click handler
    btn.removeAttribute('data-screen-off');
    const label = ar ? 'شاشة التوقف' : 'Screensaver';
    btn.innerHTML = `${label} ` + svg('<path d="M5 21V10c0-3 3-5 7-8 4 3 7 5 7 8v11z"/><path d="M9 21v-6h6v6"/>', 24);
    btn.dataset.hints = JSON.stringify(['OK  ' + label, ar ? 'رجوع  إغلاق' : 'Back  close']);
    btn.addEventListener('click', () => (location.href = location.pathname.replace(/radio-saver\/?$/, 'verse-screensaver/')));
    old.replaceWith(btn);
    stage.querySelector<HTMLElement>('[data-open-radio]')!.click();
    moveStart('.arch-btn', '[data-radio-toggle]');
  }

  // ------------------------------------------------------------ inputs pill and list
  if (has('inputs')) {
    const pills = stage.querySelector<HTMLElement>('.pills')!;
    const pill = document.createElement('button');
    pill.className = 'ft pill mock-inputs';
    // Home's styles are scoped to its own elements (data-astro-cid-*): give the new pill the same marker.
    for (const a of [...pills.querySelector('.pill')!.attributes]) if (a.name.startsWith('data-astro-cid')) pill.setAttribute(a.name, '');
    pill.innerHTML = svg('<rect x="3" y="5" width="18" height="12" rx="2"/><path d="M8 21h8M12 17v4M10 9l3 2-3 2"/>') + `<span>${ar ? 'المصادر' : 'Inputs'}</span>`;
    pill.dataset.hints = JSON.stringify([ar ? 'OK  المصادر' : 'OK  Inputs']);
    pills.append(pill);
    const names = ar
      ? ['HDMI 1 · بلايستيشن', 'HDMI 2 · ريسيفر', 'HDMI 3', 'البث التلفزيوني']
      : ['HDMI 1 · PlayStation', 'HDMI 2 · Receiver', 'HDMI 3', 'Live TV'];
    let pop: ReturnType<typeof popup> | null = null;
    const close = () => { pop?.box.remove(); pop = null; pill.focus({ preventScroll: true }); };
    const open = () => {
      pop = popup('mock-inputs-pop', names, names.map((n) => () => { close(); toast((ar ? 'على التلفاز يفتح ' : 'On the TV this opens ') + n); }), close);
      stage.append(pop.box);
      if (stage.contains(document.activeElement)) pop.rows[0].focus({ preventScroll: true });
      return pop;
    };
    pill.addEventListener('click', open);
    const first = open();
    moveStart('.arch-btn', '#' + first.rows[0].id);
  }

  // ------------------------------------------------------------ hold OK on a favourite
  if (has('hold-menu')) {
    const favs = [...stage.querySelectorAll<HTMLElement>('.fav')];
    favs.forEach((f, i) => (f.id = 'mock-fav-' + i));
    let pop: ReturnType<typeof popup> | null = null;
    const close = (focus?: HTMLElement) => { pop?.box.remove(); pop = null; focus?.focus({ preventScroll: true }); };
    const swap = (a: HTMLElement, b: HTMLElement) => {
      const [h, t] = [a.innerHTML, a.dataset.toast];
      a.innerHTML = b.innerHTML; a.dataset.toast = b.dataset.toast;
      b.innerHTML = h; b.dataset.toast = t;
    };
    const open = (i: number, focus: boolean) => {
      close();
      const f = favs[i];
      const labels = ar
        ? ['نقل للأمام', 'نقل للخلف', 'استبدال بتطبيق آخر…', 'كل التطبيقات']
        : ['Move earlier', 'Move later', 'Replace with another app…', 'All apps'];
      pop = popup('mock-fav-pop', labels, [
        () => { if (i > 0) { swap(f, favs[i - 1]); close(favs[i - 1]); } else close(f); },
        () => { if (i < favs.length - 1) { swap(f, favs[i + 1]); close(favs[i + 1]); } else close(f); },
        () => { close(f); toast(ar ? 'تفتح «كل التطبيقات» لاختيار البديل' : 'Opens All apps to pick the replacement'); },
        () => { close(f); stage.querySelector<HTMLAnchorElement>('.more.all')?.click(); },
      ], () => close(f));
      pop.box.style.insetInlineStart = getComputedStyle(f).insetInlineStart;
      stage.append(pop.box);
      if (focus) pop.rows[0].focus({ preventScroll: true });
      return pop;
    };
    favs.forEach((f, i) => {
      f.addEventListener('keydown', (e) => { if (e.key === 'Enter' && e.repeat) { e.preventDefault(); open(i, true); } });
      f.addEventListener('contextmenu', (e) => { e.preventDefault(); open(i, true); });
    });
    const first = open(1, false);
    moveStart('.arch-btn', '#' + first.rows[0].id);
  }

  // ------------------------------------------------------------ colour glow behind the favourites
  if (has('glow')) {
    const COLORS = ['#e62117', '#17a673', '#d8b46a', '#3b82f6']; // sampled from each app's icon on the TV
    const layers = [0, 1].map(() => {
      const g = document.createElement('div');
      g.className = 'mock-glow';
      stage.querySelector('.fav-label')!.before(g);
      return g;
    });
    let front = 0;
    const favs = [...stage.querySelectorAll<HTMLElement>('.fav')];
    stage.addEventListener('focusin', (e) => {
      const i = favs.indexOf(e.target as HTMLElement);
      if (i < 0) { layers.forEach((l) => (l.style.opacity = '0')); return; }
      const back = 1 - front;
      layers[back].style.background = `radial-gradient(closest-side, ${COLORS[i]}66, ${COLORS[i]}22 55%, transparent)`;
      layers[back].style.insetInlineStart = `calc(${getComputedStyle(favs[i]).insetInlineStart} - 260px)`;
      layers[back].style.opacity = '1';
      layers[front].style.opacity = '0';
      front = back;
    });
  }

  // ------------------------------------------------------------ the verse rides with the screensaver's drift
  const verse = stage.querySelector<HTMLElement>('.mock-verse-saver');
  const group = stage.querySelector<HTMLElement>('[data-group]');
  if (verse && group) group.append(verse);
}

// ?focus=<selector> focuses an element on load (screenshots of focus states).
const want = new URLSearchParams(location.search).get('focus');
if (stage && want) window.setTimeout(() => stage.querySelector<HTMLElement>(want)?.focus({ preventScroll: true }), 300);
