package tv.afnon.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/**
 * Settings' switch, in its rows and in a list of switches (Turn the TV on). On: gold track, lapis knob at the end.
 * Off: lapis-700 track, mist knob at the start. On a gold row the on track turns lapis so it still shows.
 */
final class Switch extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean on, focused;

    Switch(Context c, boolean on, boolean focused) {
        super(c);
        this.on = on;
        this.focused = focused;
    }

    /** A switch in a list (Turn the TV on) follows its item's value and focus without being rebuilt. */
    void set(boolean on, boolean focused) {
        if (on == this.on && focused == this.focused) return;
        this.on = on;
        this.focused = focused;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight(), r = h / 2;
        p.setColor(on ? (focused ? Tokens.LAPIS_900 : Tokens.GOLD_400) : Tokens.LAPIS_700);
        c.drawRoundRect(0, 0, w, h, r, r, p);
        float knob = h * 18 / 48, inset = h * 6 / 48;
        boolean atEnd = on;
        boolean rtl = getLayoutDirection() == LAYOUT_DIRECTION_RTL;
        float cx = (atEnd != rtl) ? w - inset - knob : inset + knob;
        p.setColor(on ? (focused ? Tokens.GOLD_400 : Tokens.LAPIS_900) : Tokens.MIST_400);
        c.drawCircle(cx, r, knob, p);
    }
}
