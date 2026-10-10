package com.wok.infantry.client.screen;

import java.util.Objects;

/**
 * Paint of the tablet device itself, per faction livery (preview {@code 18-device-livery.js}
 * {@code SKINS}, P3). Unlike the {@link TacticalPalette} it never changes with the page: the
 * hardware LED over the current page key keeps its colour even where a page scope recolours the
 * selection.
 *
 * <p>Components map to the preview keys in order: {@code CASE} is {@link #caseColor} ({@code case}
 * is a Java keyword), {@code LED_POWER} is {@link #powerLed}; every other key is the camel-case
 * name ({@code CASE_HI} = {@link #caseHi}, {@code SIGNAL_OFF} = {@link #signalOff}, ...).
 *
 * @param caseColor  device body
 * @param caseHi     lit rim of the body (top / left)
 * @param caseLo     shaded rim of the body and the glass recess lip
 * @param line       outermost outline of the body and the key caps
 * @param recessHi   lit bottom / right lip of the glass recess and the rim groove
 * @param rubber     corner bumpers and side keys
 * @param rubberHi   lit edge of the top bumpers
 * @param rubberLo   shaded edge of the bottom bumpers and the bumper ridges
 * @param rubberEdge outline of the bumpers and side keys
 * @param key        bezel key face
 * @param keyHi      upper lip of a raised key (and its hover face)
 * @param keyLo      lower lip of a raised key, upper lip of a pressed one
 * @param keyDown    face of the pressed key (current page)
 * @param keyText    key label
 * @param keySub     secondary key label (the action beside {@code Esc} / {@code R})
 * @param stripe     faction stripe on the bezel and in the status bar
 * @param silk       "WOK-T7" silkscreen on the top bezel
 * @param label      text printed on the bezel
 * @param status     status bar fill
 * @param statusLine status bar bottom rule
 * @param ident      identity text in the status bar
 * @param pill       feedback pill fill in the status bar
 * @param signalOff  unlit signal bars
 * @param led        link-OK LED and the lit LED over the current page key
 * @param powerLed   power LED on the top bezel
 */
public record DeviceSkin(int caseColor, int caseHi, int caseLo, int line, int recessHi,
                         int rubber, int rubberHi, int rubberLo, int rubberEdge,
                         int key, int keyHi, int keyLo, int keyDown, int keyText, int keySub,
                         int stripe, int silk, int label, int status, int statusLine, int ident,
                         int pill, int signalOff, int led, int powerLed) {
    /** Academy (blue side): navy case, navy rubber, light-blue stripe and LED. */
    public static final DeviceSkin ACADEMY = new DeviceSkin(
            0xFF204A82, 0xFF3768A2, 0xFF143058, 0xFF040A16, 0xFF305D92,
            0xFF15253F, 0xFF283F63, 0xFF0D172A, 0xFF03070F,
            0xFF2A5186, 0xFF4673AC, 0xFF173257, 0xFF1A3963, 0xFFEAF1F8, 0xFFAAC3E0,
            0xFF8CC4F5, 0xFFA8C2E2, 0xFFBBD0E8, 0xFF091731, 0xFF22427A, 0xFF9ACBF6,
            0xFF132B52, 0xFF2A4573, 0xFF8AC4F5, 0xFF7CCB8F);
    /**
     * Caesar (red side): true-red case, dark red rubber, a warm-white LED (a red link LED would
     * read as "link lost").
     */
    public static final DeviceSkin CAESAR = new DeviceSkin(
            0xFF8A1E26, 0xFFAD3440, 0xFF5A1219, 0xFF1A0507, 0xFFA02B35,
            0xFF3A0E13, 0xFF5C1A21, 0xFF26080C, 0xFF0E0305,
            0xFF962530, 0xFFB73B47, 0xFF5E141B, 0xFF6C1820, 0xFFFCF1EF, 0xFFF2D9D6,
            0xFFF6D8D4, 0xFFEBCDC9, 0xFFF6DCD9, 0xFF2C0A0D, 0xFF6A1820, 0xFFFFD6CF,
            0xFF4C1117, 0xFF642229, 0xFFFFE4E8, 0xFF7CCB8F);
    /**
     * Neutral (no faction): pale steel case with dark ink, a lit teal LED and its own deeper green
     * power LED (the A success green does not read as lit on the pale case).
     */
    public static final DeviceSkin NEUTRAL = new DeviceSkin(
            0xFFC6CCCA, 0xFFE3E7E5, 0xFF9AA19E, 0xFF2E3432, 0xFFD8DDDB,
            0xFFB4B7B0, 0xFFD0D3CC, 0xFF8D9089, 0xFF3E423D,
            0xFFE4E8E6, 0xFFF7F9F8, 0xFFB3BAB7, 0xFFCDD3D0, 0xFF222A29, 0xFF56605E,
            0xFF59625F, 0xFF4E5855, 0xFF2D3634, 0xFFCDD4D6, 0xFFA7B0B2, 0xFF3F4A4D,
            0xFFBAC3C5, 0xFFA9B2B4, 0xFF12A3B4, 0xFF2E9E52);

    /** Device paint of {@code livery}. */
    public static DeviceSkin forLivery(TacticalLivery.Livery livery) {
        return switch (Objects.requireNonNull(livery, "livery")) {
            case ACADEMY -> ACADEMY;
            case CAESAR -> CAESAR;
            case NEUTRAL -> NEUTRAL;
        };
    }
}
