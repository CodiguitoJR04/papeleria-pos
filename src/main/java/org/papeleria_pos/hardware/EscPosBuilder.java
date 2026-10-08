package org.papeleria_pos.hardware;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Constructor de comandos ESC/POS.
 * Cada método devuelve la propia instancia para encadenar llamadas.
 */
public class EscPosBuilder {

    /* ---------- Comandos ESC/POS estándar ---------- */
    private static final byte[] CMD_INIT       = { 0x1B, '@' };
    private static final byte[] CMD_BOLD_ON    = { 0x1B, 'E', 1 };
    private static final byte[] CMD_BOLD_OFF   = { 0x1B, 'E', 0 };
    private static final byte[] CMD_ALIGN_L    = { 0x1B, 'a', 0 };
    private static final byte[] CMD_ALIGN_C    = { 0x1B, 'a', 1 };
    private static final byte[] CMD_ALIGN_R    = { 0x1B, 'a', 2 };
    private static final byte[] CMD_DOUBLE_ON  = { 0x1D, '!', 0x11 };
    private static final byte[] CMD_DOUBLE_OFF = { 0x1D, '!', 0x00 };
    private static final byte[] CMD_CUT_FULL   = { 0x1D, 'V', 0x00 };
    private static final byte[] CMD_DRAWER     = { 0x1B, 'p', 0x00, 0x19, (byte) 0xFA };

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final Charset charset;

    public EscPosBuilder(String charsetName) {
        Charset cs;
        try {
            cs = Charset.forName(charsetName);
        } catch (Exception e) {
            System.err.println("⚠ Charset '" + charsetName + "' no soportado. Usando ISO-8859-1.");
            cs = Charset.forName("ISO-8859-1");
        }
        this.charset = cs;
    }

    /* ---------- Comandos ---------- */
    public EscPosBuilder init()                { write(CMD_INIT);       return this; }
    public EscPosBuilder bold(boolean on)      { write(on ? CMD_BOLD_ON : CMD_BOLD_OFF); return this; }
    public EscPosBuilder alignLeft()           { write(CMD_ALIGN_L);    return this; }
    public EscPosBuilder alignCenter()         { write(CMD_ALIGN_C);    return this; }
    public EscPosBuilder alignRight()          { write(CMD_ALIGN_R);    return this; }
    public EscPosBuilder doubleSize(boolean on){ write(on ? CMD_DOUBLE_ON : CMD_DOUBLE_OFF); return this; }

    public EscPosBuilder feed(int lines) {
        if (lines < 0) lines = 0;
        write(new byte[]{ 0x1B, 'd', (byte) lines });
        return this;
    }

    public EscPosBuilder line(String s) {
        write((s == null ? "" : s).getBytes(charset));
        write("\n".getBytes(charset));
        return this;
    }

    public EscPosBuilder blank() { return line(""); }


    public EscPosBuilder separator(int width) {
        if (width < 1) width = 32;
        return line("-".repeat(width));
    }

    /** Pulso eléctrico para abrir el cajón (RJ11 en la impresora). */
    public EscPosBuilder openDrawer() { write(CMD_DRAWER); return this; }

    /** Avanza el papel y corta. */
    public EscPosBuilder cut() {
        feed(3);
        write(CMD_CUT_FULL);
        return this;
    }
    public EscPosBuilder image(byte[] rasterData) {
        if (rasterData != null && rasterData.length > 0) {
            write(rasterData);
        }
        return this;
    }


    public byte[] build() {
        return out.toByteArray();
    }

    private void write(byte[] b) {
        try { out.write(b); }
        catch (IOException ignored) { /* ByteArrayOutputStream no lanza */ }
    }

}