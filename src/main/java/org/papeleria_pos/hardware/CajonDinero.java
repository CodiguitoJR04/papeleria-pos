package org.papeleria_pos.hardware;

import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;

public class CajonDinero {

    /** Comando ESC/POS estándar para abrir el cajón (pin 2) */
    private static final byte[] OPEN_DRAWER_PIN2 = {
            0x1B, 0x70, 0x00, 0x19, (byte) 0xFA
    };

    /**
     * Abre el cajón de dinero enviando el comando a la impresora por defecto.
     * @return true si el comando se envió sin error
     */
    public static boolean abrir() {
        try {
            PrintService impresora = PrintServiceLookup.lookupDefaultPrintService();
            if (impresora == null) {
                System.err.println("⚠ No hay impresora predeterminada. El cajón no se puede abrir.");
                return false;
            }

            DocPrintJob job = impresora.createPrintJob();
            Doc doc = new SimpleDoc(OPEN_DRAWER_PIN2,
                    DocFlavor.BYTE_ARRAY.AUTOSENSE, null);

            PrintRequestAttributeSet attrs = new HashPrintRequestAttributeSet();
            job.print(doc, attrs);
            return true;

        } catch (Exception e) {
            System.err.println("❌ Error abriendo cajón: " + e.getMessage());
            return false;
        }
    }

    /**
     * Variante para una impresora específica por nombre (búsqueda por substring).
     */
    public static boolean abrir(String nombreImpresora) {
        try {
            PrintService[] servicios = PrintServiceLookup.lookupPrintServices(null, null);
            for (PrintService ps : servicios) {
                if (ps.getName().toLowerCase().contains(nombreImpresora.toLowerCase())) {
                    DocPrintJob job = ps.createPrintJob();
                    Doc doc = new SimpleDoc(OPEN_DRAWER_PIN2,
                            DocFlavor.BYTE_ARRAY.AUTOSENSE, null);
                    job.print(doc, new HashPrintRequestAttributeSet());
                    return true;
                }
            }
            System.err.println("⚠ No se encontró impresora: " + nombreImpresora);
            return false;
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            return false;
        }
    }
}