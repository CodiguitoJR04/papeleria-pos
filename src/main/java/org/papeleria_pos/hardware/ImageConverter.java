package org.papeleria_pos.hardware;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Convierte una imagen (BMP/PNG/JPG) al formato raster ESC/POS (GS v 0).
 * La imagen se convierte a 1-bit: píxeles oscuros = negro, claros = blanco.
 */
public class ImageConverter {

    /** Umbral de luminancia: menor = más oscuro. 0-255. */
    private static final int UMBRAL = 128;

    /**
     * Lee una imagen y la convierte a los bytes ESC/POS listos para imprimir.
     * Devuelve un array vacío si la imagen no se puede leer.
     */
    public static byte[] aEscPosRaster(InputStream in) throws IOException {
        BufferedImage img = ImageIO.read(in);
        if (img == null) {
            System.err.println("⚠ No se pudo leer la imagen del logo.");
            return new byte[0];
        }
        return convertir(img);
    }

    /** Convierte una BufferedImage a bytes de comando ESC/POS. */
    public static byte[] convertir(BufferedImage img) throws IOException {
        int ancho  = img.getWidth();
        int alto   = img.getHeight();
        int bytesPorFila = (ancho + 7) / 8;   // 8 píxeles por byte

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // ---- Comando GS v 0 m xL xH yL yH ----
        out.write(0x1D);                          // GS
        out.write(0x76);                          // v
        out.write(0x30);                          // 0
        out.write(0x00);                          // m = 0 (tamaño normal)
        out.write(bytesPorFila & 0xFF);           // xL
        out.write((bytesPorFila >> 8) & 0xFF);    // xH
        out.write(alto & 0xFF);                   // yL
        out.write((alto >> 8) & 0xFF);            // yH

        // ---- Datos del bitmap (1 bit por píxel) ----
        for (int y = 0; y < alto; y++) {
            for (int xb = 0; xb < bytesPorFila; xb++) {
                int byteActual = 0;
                for (int bit = 0; bit < 8; bit++) {
                    int px = xb * 8 + bit;
                    if (px < ancho) {
                        int rgb = img.getRGB(px, y);
                        int r = (rgb >> 16) & 0xFF;
                        int g = (rgb >> 8) & 0xFF;
                        int b = rgb & 0xFF;
                        int lum = (r + g + b) / 3;

                        // Si el pixel es oscuro → bit 1 (punto negro)
                        if (lum < UMBRAL) {
                            byteActual |= (0x80 >> bit);
                        }
                    }
                }
                out.write(byteActual);
            }
        }

        return out.toByteArray();
    }
}