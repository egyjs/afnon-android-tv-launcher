package tv.afnon.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;
import android.widget.FrameLayout;

/**
 * A focusable surface: app tiles, pill buttons and the arch's foot button.
 *
 * <p>The view is larger than the design rect by the focus ring's reach (6px gap + 4px ring) on every side, so the
 * ring is drawn inside its own bounds. Place it with {@link #REACH} subtracted from x and y and added twice to w
 * and h. Focused: focus.fill (gold-400; a dim amber at night), focus.ring-color ring, scale 1.06, all in 120ms
 * (tokens.json focus.*). Content on it takes focus.text.
 */
class FocusTile extends FrameLayout {
    static final float REACH = Tokens.FOCUS_RING_OFFSET + Tokens.FOCUS_RING_WIDTH;

    interface Look {
        /** Recolour the tile's content for the focused or resting state. */
        void apply(boolean focused);
    }

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF box = new RectF();
    private final RectF ringBox = new RectF();
    private final float radius;
    private int restFill;
    private int restBorder;
    private float restBorderWidth;
    private Look look;
    private float focusScale = Tokens.FOCUS_SCALE;

    FocusTile(Context c, float radiusDesignPx) {
        super(c);
        radius = Ui.px(radiusDesignPx);
        int reach = Ui.ipx(REACH);
        setPadding(reach, reach, reach, reach);
        setWillNotDraw(false);
        setFocusable(true);
        setClickable(true);
        setClipChildren(false);
        border.setStyle(Paint.Style.STROKE);
        ring.setStyle(Paint.Style.STROKE);
        ring.setColor(Tokens.FOCUS_RING_COLOR);
        ring.setStrokeWidth(Ui.px(Tokens.FOCUS_RING_WIDTH));
        setRest(Tokens.LAPIS_800, 0, 0);
    }

    /** Resting fill (0 for none) and border (width 0 for none). */
    FocusTile setRest(int fillColor, int borderColor, float borderWidthDesignPx) {
        restFill = fillColor;
        restBorder = borderColor;
        restBorderWidth = Ui.px(borderWidthDesignPx);
        invalidate();
        return this;
    }

    /** The "All apps" tile: a dashed border instead of a fill. */
    FocusTile setDashed() {
        border.setPathEffect(new DashPathEffect(new float[] {Ui.px(8), Ui.px(6)}, 0));
        return this;
    }

    /** Rows scale less than tiles (tokens.json focus.scale-row, 1.04). */
    FocusTile setFocusScale(float s) {
        focusScale = s;
        return this;
    }

    FocusTile setLook(Look l) {
        look = l;
        l.apply(isFocused());
        return this;
    }

    @Override
    protected void onFocusChanged(boolean gained, int direction, android.graphics.Rect previous) {
        super.onFocusChanged(gained, direction, previous);
        float s = gained ? focusScale : 1f;
        animate().scaleX(s).scaleY(s).setDuration(Tokens.DUR_FOCUS_MS).setInterpolator(Ui.EASE).start();
        if (look != null) look.apply(gained);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        float reach = Ui.px(REACH);
        box.set(reach, reach, getWidth() - reach, getHeight() - reach);
        boolean focused = isFocused();
        int f = focused ? Tokens.FOCUS_FILL : restFill;
        if (f != 0) {
            fill.setColor(f);
            c.drawRoundRect(box, radius, radius, fill);
        }
        if (!focused && restBorderWidth > 0) {
            border.setColor(restBorder);
            border.setStrokeWidth(restBorderWidth);
            float h = restBorderWidth / 2;
            box.inset(h, h);
            c.drawRoundRect(box, radius - h, radius - h, border);
            box.inset(-h, -h);
        }
        if (focused) {
            float w = Ui.px(Tokens.FOCUS_RING_WIDTH);
            float out = Ui.px(Tokens.FOCUS_RING_OFFSET) + w / 2;
            ringBox.set(box.left - out, box.top - out, box.right + out, box.bottom + out);
            c.drawRoundRect(ringBox, radius + out, radius + out, ring);
        }
    }
}
