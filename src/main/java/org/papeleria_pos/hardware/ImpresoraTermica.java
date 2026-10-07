package org.papeleria_pos.hardware;

import org.papeleria_pos.dto.ItemCarrito;

import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import java.util.List;

/**
 * Fachada universal para impresoras térmicas.
 * Envía bytes crudos ESC/POS al spooler de Windows.
 */
public class ImpresoraTermica {

    private static final TicketFormatter FORMATTER = new TicketFormatter();

    /* ============================================================
       API pública
       ============================================================ */

    /** Imprime un ticket de prueba con la configuración actual. */
    public static void imprimirPrueba() throws PrintException {
        enviarCrudo(FORMATTER.construirPrueba());
    }

    /** Imprime el ticket de una venta y (si aplica) abre el cajón. */
    public static void imprimirTicket(String folio,
                                      String cajero,
                                      String turno,
                                      List<ItemCarrito> items,
                                      double subtotal,
                                      double iva,
                                      double descuento,
                                      double total,
                                      String metodoPago,
                                      double recibido,
                                      double cambio) throws PrintException {

        PrinterConfig cfg = PrinterConfig.get();

        // Decidir si abrir cajón
        boolean abrirCajon = cfg.isAbrirCajon();
        if (abrirCajon && cfg.isAbrirCajonSoloEfectivo()
                && !"EFECTIVO".equalsIgnoreCase(metodoPago)) {
            abrirCajon = false;
        }

        byte[] ticket = FORMATTER.construir(
                folio, cajero, turno, items,
                subtotal, iva, descuento, total,
                metodoPago, recibido, cambio, abrirCajon);

        // Enviar N copias
        for (int i = 0; i < cfg.getCopias(); i++) {
            enviarCrudo(ticket);
        }
    }

    /** Lista todas las impresoras disponibles (para pantalla de config). */
    public static String[] listarImpresoras() {
        PrintService[] servicios = PrintServiceLookup.lookupPrintServices(null, null);
        String[] nombres = new String[servicios.length];
        for (int i = 0; i < servicios.length; i++) {
            nombres[i] = servicios[i].getName();
        }
        return nombres;
    }

    /** ¿Hay alguna impresora disponible? */
    public static boolean estaDisponible() {
        try {
            return resolverImpresora() != null;
        } catch (Exception e) {
            return false;
        }
    }

    /* ============================================================
       Internos
       ============================================================ */

    private static void enviarCrudo(byte[] data) throws PrintException {
        PrintService servicio = resolverImpresora();
        if (servicio == null) {
            throw new PrintException("No hay impresora disponible.");
        }

        DocPrintJob job = servicio.createPrintJob();
        Doc doc = new SimpleDoc(data, DocFlavor.BYTE_ARRAY.AUTOSENSE, null);

        PrintRequestAttributeSet attrs = new HashPrintRequestAttributeSet();
        job.print(doc, attrs);
    }

    private static PrintService resolverImpresora() {
        String nombre = PrinterConfig.get().getNombreImpresora();

        PrintService[] servicios = PrintServiceLookup.lookupPrintServices(
                DocFlavor.BYTE_ARRAY.AUTOSENSE, null);

        // Si no hay ninguna, intentamos igual con la predeterminada
        if (servicios.length == 0) {
            return PrintServiceLookup.lookupDefaultPrintService();
        }

        // DEFAULT → predeterminada del sistema
        if (nombre == null || nombre.isBlank() || "DEFAULT".equalsIgnoreCase(nombre)) {
            PrintService def = PrintServiceLookup.lookupDefaultPrintService();
            if (def != null) return def;
            return servicios[0]; // fallback: la primera disponible
        }

        // Búsqueda por nombre (case-insensitive)
        for (PrintService s : servicios) {
            if (s.getName().equalsIgnoreCase(nombre)) return s;
        }

        // Si no encuentra la configurada, usa la predeterminada
        System.err.println("⚠ Impresora '" + nombre + "' no encontrada. Usando la predeterminada.");
        return PrintServiceLookup.lookupDefaultPrintService();
    }
}