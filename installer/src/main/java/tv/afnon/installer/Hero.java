package tv.afnon.installer;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;
import android.view.animation.LinearInterpolator;

/**
 * The arch at the top of every screen: Afnon's mihrab (the TV app's ArchView shape), with what is happening drawn
 * inside it. Search sends arch-shaped rings outward; progress fills a ring; done and problem show a mark.
 */
final class Hero extends View {
    enum Mode { TV, SEARCH, ALLOW, PAIR, PROGRESS, DONE, PROBLEM, CITY, APPS, CLEAN }

    private Mode mode = Mode.TV;
    /** 0..1, or below 0 for "working, amount unknown". */
    private float progress = -1;
    private float t;
    private final ValueAnimator clock = ValueAnimator.ofFloat(0, 1);

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ink = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path arch = new Path(), inner = new Path(), ring = new Path();
    private final RectF r = new RectF();
    private float k, ox, oy;

    Hero(Context c) {
        super(c);
        fill.setColor(Ui.LAPIS_800);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setColor(Ui.GOLD_400);
        line.setStyle(Paint.Style.STROKE);
        ink.setStyle(Paint.Style.STROKE);
        ink.setStrokeCap(Paint.Cap.ROUND);
        ink.setStrokeJoin(Paint.Join.ROUND);
        label.setTypeface(Ui.semibold());
        label.setTextAlign(Paint.Align.CENTER);
        label.setColor(Ui.PLASTER);
        clock.setDuration(2400);
        clock.setRepeatCount(ValueAnimator.INFINITE);
        clock.setInterpolator(new LinearInterpolator());
        clock.addUpdateListener(a -> {
            t = (float) a.getAnimatedValue();
            invalidate();
        });
    }

    void set(Mode m) {
        mode = m;
        progress = -1;
        invalidate();
    }

    void progress(float p) {
        progress = p;
        invalidate();
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        clock.start();
    }

    @Override protected void onDetachedFromWindow() {
        clock.cancel();
        super.onDetachedFromWindow();
    }

    @Override protected void onMeasure(int w, int h) {
        setMeasuredDimension(MeasureSpec.getSize(w), Ui.dp(getContext(), 196));
    }

    @Override protected void onSizeChanged(int w, int h, int ow, int oh) {
        // The arch is 600×900 in the TV app; here 2/3 of the view's height wide, with room for rings around it.
        float archH = h * 0.86f;
        k = archH / 900f;
        ox = (w - 600 * k) / 2f;
        oy = h - archH;
        arch.reset();
        shape(arch, 0);
        inner.reset();
        shape(inner, 18);
        stroke.setStrokeWidth(Math.max(2f, 8 * k));
        line.setStrokeWidth(Math.max(1f, 3 * k));
        line.setColor(0x73d8b46a);
        glow.setShader(new RadialGradient(w / 2f, oy + 480 * k, 560 * k, 0x33d8b46a, 0x00d8b46a,
                Shader.TileMode.CLAMP));
    }

    /** The arch outline, inset by {@code in} (TV-app units). */
    private void shape(Path p, float in) {
        p.moveTo(x(in), y(900));
        p.lineTo(x(in), y(330 + in * 0.2f));
        p.cubicTo(x(in), y(170 + in * 0.8f), x(140 + in * 0.6f), y(70 + in * 1.2f), x(300), y(in * 1.2f));
        p.cubicTo(x(460 - in * 0.6f), y(70 + in * 1.2f), x(600 - in), y(170 + in * 0.8f), x(600 - in), y(330 + in * 0.2f));
        p.lineTo(x(600 - in), y(900));
    }

    private float x(float v) { return ox + v * k; }
    private float y(float v) { return oy + v * k; }

    @Override protected void onDraw(Canvas c) {
        float cx = x(300), cy = y(560);
        c.drawCircle(cx, cy, 560 * k, glow);
        if (mode == Mode.SEARCH) rings(c);
        c.drawPath(arch, fill);
        c.drawPath(inner, line);
        c.drawPath(arch, stroke);
        switch (mode) {
            case TV: tv(c, cx, cy, false); break;
            case SEARCH: waves(c, cx, cy); break;
            case ALLOW: tv(c, cx, cy, true); break;
            case PAIR: code(c, cx, cy); break;
            case PROGRESS: ring(c, cx, cy); break;
            case DONE: check(c, cx, cy); break;
            case PROBLEM: problem(c, cx, cy); break;
            case CITY: crescent(c, cx, cy); break;
            case APPS: tiles(c, cx, cy); break;
            case CLEAN: sparkle(c, cx, cy); break;
        }
    }

    /** Arch outlines growing out of the arch and fading, three at a time. */
    private void rings(Canvas c) {
        for (int i = 0; i < 3; i++) {
            float f = (t + i / 3f) % 1f;
            float s = 1f + f * 0.55f;
            ring.reset();
            ring.addPath(arch);
            c.save();
            c.scale(s, s, x(300), y(900));
            line.setAlpha((int) (140 * (1 - f)));
            c.drawPath(ring, line);
            c.restore();
        }
        line.setAlpha(0x73);
    }

    private void tv(Canvas c, float cx, float cy, boolean dialog) {
        float w = 340 * k, h = 210 * k;
        ink.setColor(Ui.PLASTER);
        ink.setStrokeWidth(10 * k);
        r.set(cx - w / 2, cy - h / 2 - 30 * k, cx + w / 2, cy + h / 2 - 30 * k);
        c.drawRoundRect(r, 18 * k, 18 * k, ink);
        c.drawLine(cx - 60 * k, r.bottom + 50 * k, cx + 60 * k, r.bottom + 50 * k, ink);
        c.drawLine(cx, r.bottom, cx, r.bottom + 50 * k, ink);
        if (!dialog) {
            // A small arch on the TV: Afnon, waiting.
            ink.setColor(Ui.GOLD_400);
            ink.setStrokeWidth(8 * k);
            Path p = new Path();
            float aw = 70 * k, top = r.top + 40 * k, bottom = r.bottom - 30 * k;
            p.moveTo(cx - aw / 2, bottom);
            p.lineTo(cx - aw / 2, top + 50 * k);
            p.quadTo(cx - aw / 2, top + 10 * k, cx, top);
            p.quadTo(cx + aw / 2, top + 10 * k, cx + aw / 2, top + 50 * k);
            p.lineTo(cx + aw / 2, bottom);
            c.drawPath(p, ink);
            return;
        }
        // "Allow debugging?" with its Allow button breathing.
        Paint box = new Paint(Paint.ANTI_ALIAS_FLAG);
        box.setColor(Ui.LAPIS_700);
        RectF d = new RectF(cx - 120 * k, r.top + 35 * k, cx + 120 * k, r.bottom - 30 * k);
        c.drawRoundRect(d, 12 * k, 12 * k, box);
        box.setColor(Ui.MIST);
        c.drawRoundRect(new RectF(d.left + 24 * k, d.top + 24 * k, d.right - 60 * k, d.top + 38 * k), 7 * k, 7 * k, box);
        c.drawRoundRect(new RectF(d.left + 24 * k, d.top + 52 * k, d.right - 100 * k, d.top + 64 * k), 6 * k, 6 * k, box);
        float pulse = (float) (0.5 + 0.5 * Math.sin(t * Math.PI * 4));
        box.setColor(Ui.GOLD_400);
        box.setAlpha((int) (170 + 85 * pulse));
        c.drawRoundRect(new RectF(d.right - 110 * k, d.bottom - 46 * k, d.right - 22 * k, d.bottom - 18 * k),
                14 * k, 14 * k, box);
    }

    /** Wi-Fi arcs filling in one after another. */
    private void waves(Canvas c, float cx, float cy) {
        ink.setStrokeWidth(14 * k);
        float base = cy + 70 * k;
        for (int i = 0; i < 3; i++) {
            float rad = (70 + i * 65) * k;
            float on = (t * 4f) % 4f - i;
            ink.setColor(on > 0 && on < 2.2f ? Ui.GOLD_400 : 0x59efe7d6);
            r.set(cx - rad, base - rad, cx + rad, base + rad);
            c.drawArc(r, 225, 90, false, ink);
        }
        Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
        dot.setColor(Ui.GOLD_400);
        c.drawCircle(cx, base, 16 * k, dot);
    }

    /** Six code boxes, lighting up left to right. */
    private void code(Canvas c, float cx, float cy) {
        float s = 64 * k, gap = 18 * k, total = 6 * s + 5 * gap + 26 * k;
        float left = cx - total / 2;
        Paint box = new Paint(Paint.ANTI_ALIAS_FLAG);
        box.setStyle(Paint.Style.STROKE);
        box.setStrokeWidth(6 * k);
        int lit = (int) (t * 9) % 9;
        for (int i = 0; i < 6; i++) {
            float l = left + i * (s + gap) + (i >= 3 ? 26 * k : 0);
            r.set(l, cy - s / 2 - 20 * k, l + s, cy + s / 2 - 20 * k);
            box.setColor(i < lit ? Ui.GOLD_400 : 0x59efe7d6);
            c.drawRoundRect(r, 12 * k, 12 * k, box);
            if (i < lit) {
                Paint d = new Paint(Paint.ANTI_ALIAS_FLAG);
                d.setColor(Ui.GOLD_400);
                c.drawCircle(r.centerX(), r.centerY(), 9 * k, d);
            }
        }
    }

    private void ring(Canvas c, float cx, float cy) {
        float rad = 150 * k;
        r.set(cx - rad, cy - rad, cx + rad, cy + rad);
        ink.setStrokeWidth(18 * k);
        ink.setColor(Ui.LAPIS_700);
        c.drawArc(r, 0, 360, false, ink);
        ink.setColor(Ui.GOLD_400);
        if (progress < 0) {
            c.drawArc(r, t * 360f - 90, 80, false, ink);
        } else {
            c.drawArc(r, -90, 360 * progress, false, ink);
            label.setTextSize(72 * k);
            label.setColor(Ui.PLASTER);
            c.drawText(Math.round(progress * 100) + "%", cx, cy + 26 * k, label);
        }
    }

    private void check(Canvas c, float cx, float cy) {
        float rad = 150 * k;
        float breathe = (float) (0.5 + 0.5 * Math.sin(t * Math.PI * 2));
        Paint disc = new Paint(Paint.ANTI_ALIAS_FLAG);
        disc.setColor(Ui.GOLD_400);
        disc.setAlpha((int) (40 + 30 * breathe));
        c.drawCircle(cx, cy, rad * 1.18f, disc);
        disc.setAlpha(255);
        c.drawCircle(cx, cy, rad, disc);
        ink.setColor(Ui.LAPIS_900);
        ink.setStrokeWidth(22 * k);
        Path p = new Path();
        p.moveTo(cx - 70 * k, cy + 4 * k);
        p.lineTo(cx - 18 * k, cy + 56 * k);
        p.lineTo(cx + 78 * k, cy - 52 * k);
        c.drawPath(p, ink);
    }

    /** A crescent and a star: the prayer day, for the city screen. */
    private void crescent(Canvas c, float cx, float cy) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Ui.GOLD_400);
        Path moon = new Path();
        moon.addCircle(cx - 20 * k, cy, 120 * k, Path.Direction.CW);
        Path bite = new Path();
        bite.addCircle(cx + 30 * k, cy - 30 * k, 104 * k, Path.Direction.CW);
        moon.op(bite, Path.Op.DIFFERENCE);
        c.drawPath(moon, p);
        float twinkle = (float) (0.6 + 0.4 * Math.sin(t * Math.PI * 2));
        p.setColor(Ui.PLASTER);
        p.setAlpha((int) (255 * twinkle));
        c.drawPath(star(cx + 95 * k, cy - 60 * k, 34 * k), p);
    }

    private static Path star(float x, float y, float r) {
        Path p = new Path();
        for (int i = 0; i < 10; i++) {
            double a = Math.PI / 2 + i * Math.PI / 5;
            float rr = i % 2 == 0 ? r : r * 0.42f;
            float px = x + (float) (rr * Math.cos(a)), py = y - (float) (rr * Math.sin(a));
            if (i == 0) p.moveTo(px, py); else p.lineTo(px, py);
        }
        p.close();
        return p;
    }

    /** Six app tiles, one lit at a time: the favourites. */
    private void tiles(Canvas c, float cx, float cy) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        float s = 96 * k, gap = 26 * k;
        int lit = (int) (t * 6) % 6;
        float left = cx - s - gap / 2, top = cy - (3 * s + 2 * gap) / 2 - 20 * k;
        for (int i = 0; i < 6; i++) {
            float l = left + (i % 2) * (s + gap), y = top + (i / 2) * (s + gap);
            r.set(l, y, l + s, y + s);
            p.setColor(i == lit ? Ui.GOLD_400 : Ui.LAPIS_700);
            c.drawRoundRect(r, 20 * k, 20 * k, p);
        }
    }

    /** Sparkles: the cleanup. */
    private void sparkle(Canvas c, float cx, float cy) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        float[][] stars = {{0, -10, 90}, {110, -110, 40}, {-105, 90, 34}, {95, 110, 26}};
        for (int i = 0; i < stars.length; i++) {
            float phase = (float) (0.55 + 0.45 * Math.sin((t + i * 0.25f) * Math.PI * 2));
            p.setColor(i == 0 ? Ui.GOLD_400 : Ui.PLASTER);
            p.setAlpha((int) (255 * (i == 0 ? 1f : phase)));
            float rr = stars[i][2] * k * (i == 0 ? 1f : 0.8f + 0.2f * phase);
            c.drawPath(sparkle(cx + stars[i][0] * k, cy + stars[i][1] * k, rr), p);
        }
    }

    private static Path sparkle(float x, float y, float r) {
        Path p = new Path();
        p.moveTo(x, y - r);
        p.quadTo(x, y, x + r, y);
        p.quadTo(x, y, x, y + r);
        p.quadTo(x, y, x - r, y);
        p.quadTo(x, y, x, y - r);
        p.close();
        return p;
    }

    private void problem(Canvas c, float cx, float cy) {
        float rad = 150 * k;
        Paint disc = new Paint(Paint.ANTI_ALIAS_FLAG);
        disc.setColor(Ui.CORAL_SOFT);
        c.drawCircle(cx, cy, rad, disc);
        ink.setColor(Ui.CORAL);
        ink.setStrokeWidth(10 * k);
        c.drawCircle(cx, cy, rad, ink);
        ink.setStrokeWidth(24 * k);
        c.drawLine(cx, cy - 70 * k, cx, cy + 20 * k, ink);
        c.drawPoint(cx, cy + 70 * k, ink);
    }
}
