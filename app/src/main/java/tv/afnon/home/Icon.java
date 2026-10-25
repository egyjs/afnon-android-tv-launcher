package tv.afnon.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/** Stroke icons on a 24px grid in the current colour, so they follow focus (README: Iconography). */
final class Icon extends View {
    static final int WIFI = 0, GEAR = 1, COMPASS = 2, BELL = 3, BACK = 4, STAR = 5, MIHRAB = 6, RADIO = 7, PLAY = 8, STOP = 9, MOON = 10, INPUT = 11;

    private int kind;
    private final float strokeOn24;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private final android.graphics.Path path = new android.graphics.Path();

    Icon(Context c, int kind, float strokeOn24) {
        super(c);
        this.kind = kind;
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        this.strokeOn24 = strokeOn24;
    }

    void setKind(int k) {
        if (kind == k) return;
        kind = k;
        invalidate();
    }

    void setColor(int color) {
        p.setColor(color);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        float k = getWidth() / 24f;
        p.setStrokeWidth(strokeOn24 * k);
        p.setStyle(Paint.Style.STROKE);
        if (kind == WIFI) {
            // M2 8.5a15 15 0 0 1 20 0 · M5 12a10 10 0 0 1 14 0 · M8.5 15.5a5 5 0 0 1 7 0 · dot at 12,19
            arc(c, k, 12, 19.68f, 15f);
            arc(c, k, 12, 19.14f, 10f);
            arc(c, k, 12, 19.07f, 5f);
            p.setStyle(Paint.Style.FILL);
            c.drawCircle(12 * k, 19 * k, 1.2f * k, p);
        } else if (kind == GEAR) {
            c.drawCircle(12 * k, 12 * k, 3.2f * k, p);
            c.drawCircle(12 * k, 12 * k, 7.5f * k, p);
            float[][] spokes = {{12, 1.8f, 12, 4.5f}, {12, 19.5f, 12, 22.2f}, {1.8f, 12, 4.5f, 12}, {19.5f, 12, 22.2f, 12},
                    {4.8f, 4.8f, 6.7f, 6.7f}, {17.3f, 17.3f, 19.2f, 19.2f}, {4.8f, 19.2f, 6.7f, 17.3f}, {17.3f, 6.7f, 19.2f, 4.8f}};
            for (float[] s : spokes) c.drawLine(s[0] * k, s[1] * k, s[2] * k, s[3] * k, p);
        } else if (kind == BELL) {
            // M6 16V11a6 6 0 0 1 12 0v5l1.5 2h-15z · M10 21h4
            path.reset();
            path.moveTo(6 * k, 16 * k);
            path.lineTo(6 * k, 11 * k);
            r.set(6 * k, 5 * k, 18 * k, 17 * k);
            path.arcTo(r, 180, 180);
            path.lineTo(18 * k, 16 * k);
            path.lineTo(19.5f * k, 18 * k);
            path.lineTo(4.5f * k, 18 * k);
            path.close();
            c.drawPath(path, p);
            c.drawLine(10 * k, 21 * k, 14 * k, 21 * k, p);
        } else if (kind == STAR) {
            // M12 3l2.6 5.6 6 .7-4.5 4.1 1.2 6L12 16.4 6.7 19.4l1.2-6L3.4 9.3l6-.7z, filled.
            float[] pts = {12, 3, 14.6f, 8.6f, 20.6f, 9.3f, 16.1f, 13.4f, 17.3f, 19.4f, 12, 16.4f, 6.7f, 19.4f, 7.9f, 13.4f, 3.4f, 9.3f, 9.4f, 8.6f};
            path.reset();
            path.moveTo(pts[0] * k, pts[1] * k);
            for (int i = 2; i < pts.length; i += 2) path.lineTo(pts[i] * k, pts[i + 1] * k);
            path.close();
            p.setStyle(Paint.Style.FILL);
            c.drawPath(path, p);
        } else if (kind == MIHRAB) {
            // M5 21V10c0-3 3-5 7-8 4 3 7 5 7 8v11z
            path.reset();
            path.moveTo(5 * k, 21 * k);
            path.lineTo(5 * k, 10 * k);
            path.cubicTo(5 * k, 7 * k, 8 * k, 5 * k, 12 * k, 2 * k);
            path.cubicTo(16 * k, 5 * k, 19 * k, 7 * k, 19 * k, 10 * k);
            path.lineTo(19 * k, 21 * k);
            path.close();
            c.drawPath(path, p);
        } else if (kind == RADIO) {
            // A broadcast: a dot with two waves on each side.
            p.setStyle(Paint.Style.FILL);
            c.drawCircle(12 * k, 12 * k, 1.8f * k, p);
            p.setStyle(Paint.Style.STROKE);
            for (float rad : new float[] {5f, 9f}) {
                r.set((12 - rad) * k, (12 - rad) * k, (12 + rad) * k, (12 + rad) * k);
                c.drawArc(r, -40, 80, false, p);
                c.drawArc(r, 140, 80, false, p);
            }
        } else if (kind == PLAY) {
            // A filled triangle pointing to the reading direction's end, as media players do in either language.
            path.reset();
            path.moveTo(8 * k, 5 * k);
            path.lineTo(19 * k, 12 * k);
            path.lineTo(8 * k, 19 * k);
            path.close();
            p.setStyle(Paint.Style.FILL);
            c.drawPath(path, p);
        } else if (kind == STOP) {
            p.setStyle(Paint.Style.FILL);
            r.set(6.5f * k, 6.5f * k, 17.5f * k, 17.5f * k);
            c.drawRoundRect(r, 2 * k, 2 * k, p);
        } else if (kind == MOON) {
            // A crescent: screen off, the night look.
            path.reset();
            r.set(4 * k, 4 * k, 20 * k, 20 * k);
            path.addCircle(12 * k, 12 * k, 8 * k, android.graphics.Path.Direction.CW);
            android.graphics.Path cut = new android.graphics.Path();
            cut.addCircle(16 * k, 9 * k, 7 * k, android.graphics.Path.Direction.CW);
            path.op(cut, android.graphics.Path.Op.DIFFERENCE);
            p.setStyle(Paint.Style.FILL);
            c.drawPath(path, p);
        } else if (kind == INPUT) {
            // A screen on its stand with an arrow coming down into it: a source to switch to.
            r.set(3 * k, 4 * k, 21 * k, 17 * k);
            c.drawRoundRect(r, 2 * k, 2 * k, p);
            c.drawLine(8 * k, 21 * k, 16 * k, 21 * k, p);
            c.drawLine(12 * k, 7 * k, 12 * k, 13.5f * k, p);
            c.drawLine(9 * k, 10.5f * k, 12 * k, 13.5f * k, p);
            c.drawLine(15 * k, 10.5f * k, 12 * k, 13.5f * k, p);
        } else if (kind == BACK) {
            // M15 5l-7 7 7 7: points to the start side, so it mirrors in Arabic.
            boolean flip = getLayoutDirection() == LAYOUT_DIRECTION_RTL;
            float a = flip ? 24 - 15 : 15, b = flip ? 24 - 8 : 8;
            c.drawLine(a * k, 5 * k, b * k, 12 * k, p);
            c.drawLine(b * k, 12 * k, a * k, 19 * k, p);
        } else {
            c.drawCircle(12 * k, 12 * k, 9 * k, p);
            c.drawLine(12 * k, 12 * k, 15.5f * k, 5.5f * k, p);
            c.drawLine(12 * k, 12 * k, 8.5f * k, 18.5f * k, p);
        }
    }

    /** A Wi-Fi wave: the upper part of a circle centred below the dot. */
    private void arc(Canvas c, float k, float cx, float cy, float radius) {
        r.set((cx - radius) * k, (cy - radius) * k, (cx + radius) * k, (cy + radius) * k);
        c.drawArc(r, 225, 90, false, p);
    }
}
