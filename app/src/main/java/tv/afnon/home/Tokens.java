package tv.afnon.home;

// Generated from afnon-mihrab/tokens.json by site/scripts/tokens.mjs. Do not edit: run `npm run tokens` in site/.

/**
 * Design tokens in px at 1920×1080. Multiply lengths by {@link Ui#px}. Colours are the day palette until
 * {@link #use} switches them to night mode's (afnon-mihrab/tokens.night.json).
 */
final class Tokens {
    private Tokens() {}

    /** color.lapis-950: Settings sidebar, below the ground */
    static int LAPIS_950 = 0xFF0A1730;
    /** color.lapis-900: Ground / page background */
    static int LAPIS_900 = 0xFF0C1B33;
    /** color.lapis-800: Surface: arch fill, tiles, cells, panels */
    static int LAPIS_800 = 0xFF12264A;
    /** color.lapis-700: Raised: badges, stepper buttons, toggle off track */
    static int LAPIS_700 = 0xFF1A3360;
    /** color.gold-400: Accent, sacred text, focus fill, next prayer · 8.9:1 on ground */
    static int GOLD_400 = 0xFFD8B46A;
    /** color.gold-200: Approaching-prayer glow, pressed state */
    static int GOLD_200 = 0xFFF0D79C;
    /** color.plaster-100: Primary text, focus ring, current prayer · 13.9:1 on ground */
    static int PLASTER_100 = 0xFFEFE7D6;
    /** color.mist-400: Secondary text, past prayers · 7.7:1 on ground */
    static int MIST_400 = 0xFF9FB0CC;
    /** color.disabled-text */
    static int DISABLED_TEXT = 0xFF6F7F9C;
    /** color.lapis-800-60: Past prayer cells (lapis-800 at 60%) */
    static int LAPIS_800_60 = 0x9912264A;
    /** color.gold-400-60: Arch foot button border at rest */
    static int GOLD_400_60 = 0x99D8B46A;
    /** color.line-soft: Chip borders, Qibla outer ring */
    static int LINE_SOFT = 0x40EFE7D6;
    /** color.line-dash: Dashed "All apps" border */
    static int LINE_DASH = 0x4DEFE7D6;
    /** color.arch-inner: Inner arch line */
    static int ARCH_INNER = 0x73D8B46A;
    /** color.bell-off-border: Prayer Day: reminder bell when off (mist-400 at 40%) */
    static int BELL_OFF_BORDER = 0x669FB0CC;
    /** color.banner-track: Reminder banner: time-left track (gold-400 at 25%) */
    static int BANNER_TRACK = 0x40D8B46A;
    /** color.seg-track-focused: Settings: segmented control track on a focused gold row (lapis-900 at 15%) */
    static int SEG_TRACK_FOCUSED = 0x260C1B33;
    /** color.qibla-ring: Qibla inner ring (plaster-100 at 10%) */
    static int QIBLA_RING = 0x1AEFE7D6;
    /** color.qibla-tick: Qibla cardinal ticks (plaster-100 at 50%) */
    static int QIBLA_TICK = 0x80EFE7D6;
    /** color.scrim: Prayer-time-now overlay (no blur) */
    static int SCRIM = 0xF0081224;
    /** color.scrim-menu: Behind menus, pickers and choosers (lapis-900 at 92%) */
    static int SCRIM_MENU = 0xEB0C1B33;
    /** color.scrim-panel: Behind side panels (Inputs) */
    static int SCRIM_PANEL = 0xB8060D1A;
    /** color.pattern-line: Star lattice lines (gold-400 at 7%) */
    static int PATTERN_LINE = 0x12D8B46A;
    /** color.app-icon-tint: Other apps' icons (Home's favourites, All apps) are multiplied by it in night mode; white by day, when no filter is applied */
    static int APP_ICON_TINT = 0xFFFFFFFF;
    /** color.on-plaster.label: Approaching card: caps label on plaster */
    static int ON_PLASTER_LABEL = 0xFF5A4A22;
    /** color.on-plaster.secondary: Approaching card: secondary text on plaster */
    static int ON_PLASTER_SECONDARY = 0xFF3D4A63;
    /** color.on-plaster.border: Approaching card border (lapis-900 at 60%) */
    static int ON_PLASTER_BORDER = 0x990C1B33;
    /** color.on-plaster.button-border: Approaching card: outlined button border (lapis-900 at 35%) */
    static int ON_PLASTER_BUTTON_BORDER = 0x590C1B33;
    /** color.card.fill: Approaching card (Home's reminder) ground */
    static int CARD_FILL = 0xFFEFE7D6;
    /** color.card.text: Approaching card: title and outlined button label */
    static int CARD_TEXT = 0xFF0C1B33;
    /** color.current.fill: The current prayer (Home's cell, Prayer day's row) and a chosen segment */
    static int CURRENT_FILL = 0xFFEFE7D6;
    /** color.current.text: Text and icons on current.fill */
    static int CURRENT_TEXT = 0xFF0C1B33;
    /** color.screensaver.ground: Screensaver and Audio Only ground */
    static int SCREENSAVER_GROUND = 0xFF000000;
    /** color.screensaver.text */
    static int SCREENSAVER_TEXT = 0xFFA69F90;
    /** color.screensaver.text-dim */
    static int SCREENSAVER_TEXT_DIM = 0xFF7D7769;
    /** color.screensaver.next */
    static int SCREENSAVER_NEXT = 0xFFB8A578;
    /** color.screensaver.arch-line */
    static int SCREENSAVER_ARCH_LINE = 0x59D8B46A;
    /** font.size.display-xl: Countdown */
    static final float FONT_SIZE_DISPLAY_XL = 150f;
    /** font.size.display-xl-hours: Countdown when it has hours */
    static final float FONT_SIZE_DISPLAY_XL_HOURS = 120f;
    /** font.size.display-l: Clock */
    static final float FONT_SIZE_DISPLAY_L = 96f;
    /** font.size.display-m: Next prayer name */
    static final float FONT_SIZE_DISPLAY_M = 88f;
    /** font.size.display-s: Settings chooser title */
    static final float FONT_SIZE_DISPLAY_S = 44f;
    /** font.size.display-xs: Settings Asr preview time */
    static final float FONT_SIZE_DISPLAY_XS = 36f;
    /** font.size.title-l */
    static final float FONT_SIZE_TITLE_L = 64f;
    /** font.size.title */
    static final float FONT_SIZE_TITLE = 52f;
    /** font.size.heading */
    static final float FONT_SIZE_HEADING = 40f;
    /** font.size.body-l */
    static final float FONT_SIZE_BODY_L = 30f;
    /** font.size.body */
    static final float FONT_SIZE_BODY = 26f;
    /** font.size.label */
    static final float FONT_SIZE_LABEL = 22f;
    /** font.size.caption: Minimum size on screen */
    static final float FONT_SIZE_CAPTION = 20f;
    /** font.size.sacred */
    static final float FONT_SIZE_SACRED = 32f;
    /** font.lineHeight.display */
    static final float FONT_LINEHEIGHT_DISPLAY = 1f;
    /** font.lineHeight.title */
    static final float FONT_LINEHEIGHT_TITLE = 1.15f;
    /** font.lineHeight.body */
    static final float FONT_LINEHEIGHT_BODY = 1.5f;
    /** font.lineHeight.sacred */
    static final float FONT_LINEHEIGHT_SACRED = 1.55f;
    /** font.letterSpacing.caps-en: English caps labels only */
    static final float FONT_LETTERSPACING_CAPS_EN = 0.2f;
    /** font.letterSpacing.ar: Arabic is never letter-spaced */
    static final float FONT_LETTERSPACING_AR = 0f;
    /** space.1 */
    static final float SPACE_1 = 8f;
    /** space.2 */
    static final float SPACE_2 = 12f;
    /** space.3 */
    static final float SPACE_3 = 16f;
    /** space.4 */
    static final float SPACE_4 = 24f;
    /** space.5 */
    static final float SPACE_5 = 32f;
    /** space.6 */
    static final float SPACE_6 = 48f;
    /** space.7 */
    static final float SPACE_7 = 64f;
    /** space.8 */
    static final float SPACE_8 = 96f;
    /** space.safe-inline */
    static final float SPACE_SAFE_INLINE = 96f;
    /** space.safe-block */
    static final float SPACE_SAFE_BLOCK = 56f;
    /** layout.arch.width */
    static final float ARCH_W = 600f;
    /** layout.arch.height */
    static final float ARCH_H = 900f;
    /** layout.arch.inset-inline-start */
    static final float ARCH_X = 96f;
    /** layout.arch.top */
    static final float ARCH_Y = 120f;
    /** layout.arch.crown-height */
    static final float LAYOUT_ARCH_CROWN_HEIGHT = 380f;
    /** layout.courtyard.inset-inline-start */
    static final float COURT_X = 760f;
    /** layout.courtyard.width */
    static final float COURT_W = 1064f;
    /** layout.gap-favourites */
    static final float LAYOUT_GAP_FAVOURITES = 24f;
    /** layout.gap-more */
    static final float LAYOUT_GAP_MORE = 20f;
    /** layout.gap-prayer-cells */
    static final float LAYOUT_GAP_PRAYER_CELLS = 12f;
    /** layout.tile-favourite.width */
    static final float LAYOUT_TILE_FAVOURITE_WIDTH = 248f;
    /** layout.tile-favourite.height */
    static final float LAYOUT_TILE_FAVOURITE_HEIGHT = 150f;
    /** layout.tile-more.height */
    static final float LAYOUT_TILE_MORE_HEIGHT = 112f;
    /** layout.prayer-cell.height */
    static final float LAYOUT_PRAYER_CELL_HEIGHT = 146f;
    /** layout.day-row.width */
    static final float LAYOUT_DAY_ROW_WIDTH = 620f;
    /** layout.day-row.height */
    static final float LAYOUT_DAY_ROW_HEIGHT = 104f;
    /** layout.button-height */
    static final float LAYOUT_BUTTON_HEIGHT = 72f;
    /** layout.toast-width */
    static final float LAYOUT_TOAST_WIDTH = 680f;
    /** layout.banner-width */
    static final float LAYOUT_BANNER_WIDTH = 640f;
    /** layout.side-panel-width */
    static final float LAYOUT_SIDE_PANEL_WIDTH = 600f;
    /** layout.settings.sidebar-width */
    static final float LAYOUT_SETTINGS_SIDEBAR_WIDTH = 560f;
    /** layout.settings.row-width */
    static final float LAYOUT_SETTINGS_ROW_WIDTH = 1184f;
    /** radius.badge */
    static final float RADIUS_BADGE = 8f;
    /** radius.cell */
    static final float RADIUS_CELL = 14f;
    /** radius.row */
    static final float RADIUS_ROW = 16f;
    /** radius.tile */
    static final float RADIUS_TILE = 18f;
    /** radius.panel */
    static final float RADIUS_PANEL = 24f;
    /** radius.pill */
    static final float RADIUS_PILL = 999f;
    /** focus.fill: A focused tile, row or button */
    static int FOCUS_FILL = 0xFFD8B46A;
    /** focus.text: Text and icons on focus.fill */
    static int FOCUS_TEXT = 0xFF0C1B33;
    /** focus.ring-color: The ring round a focused tile */
    static int FOCUS_RING_COLOR = 0xFFEFE7D6;
    /** focus.ring-width */
    static final float FOCUS_RING_WIDTH = 4f;
    /** focus.ring-offset */
    static final float FOCUS_RING_OFFSET = 6f;
    /** focus.scale */
    static final float FOCUS_SCALE = 1.06f;
    /** focus.scale-row */
    static final float FOCUS_SCALE_ROW = 1.04f;
    /** focus.scale-settings-row: Settings rows, which are wide */
    static final float FOCUS_SCALE_SETTINGS_ROW = 1.02f;
    /** focus.on-light.fill */
    static int FOCUS_ON_LIGHT_FILL = 0xFF0C1B33;
    /** focus.on-light.text */
    static int FOCUS_ON_LIGHT_TEXT = 0xFFEFE7D6;
    /** focus.on-light.ring-color */
    static int FOCUS_ON_LIGHT_RING_COLOR = 0xFFD8B46A;
    /** focus.on-light.ring-offset */
    static final float FOCUS_ON_LIGHT_RING_OFFSET = 4f;
    /** focus.pressed.fill */
    static int FOCUS_PRESSED_FILL = 0xFFF0D79C;
    /** focus.pressed.scale */
    static final float FOCUS_PRESSED_SCALE = 1.02f;
    /** focus.pressed.ring-offset */
    static final float FOCUS_PRESSED_RING_OFFSET = 4f;
    /** border.arch-rest.color */
    static int BORDER_ARCH_REST_COLOR = 0xFFD8B46A;
    /** border.arch-rest.width */
    static final float BORDER_ARCH_REST_WIDTH = 3f;
    /** border.arch-focused.color */
    static int BORDER_ARCH_FOCUSED_COLOR = 0xFFEFE7D6;
    /** border.arch-focused.width */
    static final float BORDER_ARCH_FOCUSED_WIDTH = 5f;
    /** border.arch-approaching.color */
    static int BORDER_ARCH_APPROACHING_COLOR = 0xFFF0D79C;
    /** border.arch-approaching.width */
    static final float BORDER_ARCH_APPROACHING_WIDTH = 6f;
    /** border.next-prayer.color */
    static int BORDER_NEXT_PRAYER_COLOR = 0xFFD8B46A;
    /** border.next-prayer.width */
    static final float BORDER_NEXT_PRAYER_WIDTH = 3f;
    /** border.chip.color */
    static int BORDER_CHIP_COLOR = 0x40EFE7D6;
    /** border.chip.width */
    static final float BORDER_CHIP_WIDTH = 2f;
    /** motion.focus */
    static final long DUR_FOCUS_MS = 120;
    /** motion.panel-slide */
    static final long MOTION_PANEL_SLIDE_MS = 240;
    /** motion.overlay-fade: linear */
    static final long MOTION_OVERLAY_FADE_MS = 200;
    /** motion.screensaver-fade: As AfnonDream.FADE_MS */
    static final long MOTION_SCREENSAVER_FADE_MS = 800;
    /** motion.screensaver-drift-interval */
    static final long MOTION_SCREENSAVER_DRIFT_INTERVAL_MS = 60000;
    /** motion.banner-lifetime */
    static final long MOTION_BANNER_LIFETIME_MS = 12000;
    /** motion.saved-lifetime: Settings: how long ✓ Saved shows after a change */
    static final long MOTION_SAVED_LIFETIME_MS = 2000;
    /** motion.call-auto-dismiss: The call screen closes by itself: the default of Settings → Reminders → Close the call screen */
    static final long MOTION_CALL_AUTO_DISMISS_MS = 600000;
    /** motion.call-silent-dismiss: The call screen set to close when the adhan ends, with no adhan playing */
    static final long MOTION_CALL_SILENT_DISMISS_MS = 60000;
    /** timing.approach: Before a prayer, the arch switches to its approaching outline */
    static final int APPROACH_MIN = 15;

    /** Whether the colours above are night mode's. */
    static boolean night;

    /**
     * The day or the night palette for every colour above. Views take their colours when they are built, so a
     * screen in front rebuilds after a switch; the others build in the new palette when they next open.
     */
    static void use(boolean night) {
        if (Tokens.night == night) return;
        Tokens.night = night;
        LAPIS_950 = night ? 0xFF090806 : 0xFF0A1730;
        LAPIS_900 = night ? 0xFF0B0A08 : 0xFF0C1B33;
        LAPIS_800 = night ? 0xFF17140F : 0xFF12264A;
        LAPIS_700 = night ? 0xFF221D16 : 0xFF1A3360;
        GOLD_400 = night ? 0xFF84703F : 0xFFD8B46A;
        GOLD_200 = night ? 0xFF93835C : 0xFFF0D79C;
        PLASTER_100 = night ? 0xFF8F877A : 0xFFEFE7D6;
        MIST_400 = night ? 0xFF6A655C : 0xFF9FB0CC;
        DISABLED_TEXT = night ? 0xFF57524A : 0xFF6F7F9C;
        LAPIS_800_60 = night ? 0x9917140F : 0x9912264A;
        GOLD_400_60 = night ? 0x9984703F : 0x99D8B46A;
        LINE_SOFT = night ? 0x388F877A : 0x40EFE7D6;
        LINE_DASH = night ? 0x458F877A : 0x4DEFE7D6;
        ARCH_INNER = night ? 0x7384703F : 0x73D8B46A;
        BELL_OFF_BORDER = night ? 0x666A655C : 0x669FB0CC;
        BANNER_TRACK = night ? 0x4084703F : 0x40D8B46A;
        SEG_TRACK_FOCUSED = night ? 0x4D0B0A08 : 0x260C1B33;
        QIBLA_RING = night ? 0x1A8F877A : 0x1AEFE7D6;
        QIBLA_TICK = night ? 0x738F877A : 0x80EFE7D6;
        SCRIM = night ? 0xF0070605 : 0xF0081224;
        SCRIM_MENU = night ? 0xF00B0A08 : 0xEB0C1B33;
        SCRIM_PANEL = night ? 0xB8050403 : 0xB8060D1A;
        PATTERN_LINE = night ? 0x0F84703F : 0x12D8B46A;
        APP_ICON_TINT = night ? 0xFFA0927A : 0xFFFFFFFF;
        ON_PLASTER_LABEL = night ? 0xFF84703F : 0xFF5A4A22;
        ON_PLASTER_SECONDARY = night ? 0xFF6A655C : 0xFF3D4A63;
        ON_PLASTER_BORDER = night ? 0x8084703F : 0x990C1B33;
        ON_PLASTER_BUTTON_BORDER = night ? 0x598F877A : 0x590C1B33;
        CARD_FILL = night ? 0xFF221D16 : 0xFFEFE7D6;
        CARD_TEXT = night ? 0xFFA99C80 : 0xFF0C1B33;
        CURRENT_FILL = night ? 0xFF2A241B : 0xFFEFE7D6;
        CURRENT_TEXT = night ? 0xFFB0A27F : 0xFF0C1B33;
        SCREENSAVER_TEXT = night ? 0xFF6A655C : 0xFFA69F90;
        SCREENSAVER_TEXT_DIM = night ? 0xFF4F4B44 : 0xFF7D7769;
        SCREENSAVER_NEXT = night ? 0xFF75684A : 0xFFB8A578;
        SCREENSAVER_ARCH_LINE = night ? 0x4784703F : 0x59D8B46A;
        FOCUS_FILL = night ? 0xFF3A3022 : 0xFFD8B46A;
        FOCUS_TEXT = night ? 0xFFCDB68A : 0xFF0C1B33;
        FOCUS_RING_COLOR = night ? 0xFFA8987A : 0xFFEFE7D6;
        FOCUS_ON_LIGHT_FILL = night ? 0xFF0B0A08 : 0xFF0C1B33;
        FOCUS_ON_LIGHT_TEXT = night ? 0xFF8F877A : 0xFFEFE7D6;
        FOCUS_ON_LIGHT_RING_COLOR = night ? 0xFF84703F : 0xFFD8B46A;
        FOCUS_PRESSED_FILL = night ? 0xFF93835C : 0xFFF0D79C;
        BORDER_ARCH_REST_COLOR = night ? 0xFF84703F : 0xFFD8B46A;
        BORDER_ARCH_FOCUSED_COLOR = night ? 0xFF8F877A : 0xFFEFE7D6;
        BORDER_ARCH_APPROACHING_COLOR = night ? 0xFF93835C : 0xFFF0D79C;
        BORDER_NEXT_PRAYER_COLOR = night ? 0xFF84703F : 0xFFD8B46A;
        BORDER_CHIP_COLOR = night ? 0x388F877A : 0x40EFE7D6;
    }
}
