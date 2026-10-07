package org.papeleria_pos.hardware;

import org.papeleria_pos.dto.ItemCarrito;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Construye el ticket como bytes ESC/POS a partir de una venta.
 * Toda la plantilla (header, columnas, totales, footer) vive aquí.
 */
public class TicketFormatter {

    private static final NumberFormat MXN =
            NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-MX"));

    private static final DateTimeFormatter FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final PrinterConfig cfg = PrinterConfig.get();

    /* ============================================================
       Ticket de venta
       ============================================================ */
    public byte[] construir(String folio,
                            String cajero,
                            String turno,
                            List<ItemCarrito> items,
                            double subtotal,
                            double iva,
                            double descuento,
                            double total,
                            String metodoPago,
                            double recibido,
                            double cambio,
                            boolean abrirCajon) {

        int w = cfg.getAncho();
        EscPosBuilder b = new EscPosBuilder(cfg.getCharset());
        b.init();

        /* ---------- Encabezado ---------- */
        b.alignCenter().bold(true).doubleSize(true)
                .line(cfg.getNegocioNombre())
                .doubleSize(false).bold(false);

        if (!cfg.getNegocioDireccion().isEmpty()) b.line(cfg.getNegocioDireccion());
        if (!cfg.getNegocioTelefono().isEmpty())  b.line("Tel: " + cfg.getNegocioTelefono());
        if (!cfg.getNegocioRfc().isEmpty())       b.line("RFC: " + cfg.getNegocioRfc());
        b.blank();

        /* ---------- Datos del ticket ---------- */
        b.alignLeft();
        b.line("Folio: " + folio);
        b.line("Fecha: " + LocalDateTime.now().format(FECHA));
        b.line("Cajero: " + cajero);
        b.line("Turno: " + turno);
        b.separator(w);

        /* ---------- Columnas ---------- */
        int colProd = Math.max(10, w - 4 - 9 - 9 - 3);   // 22 en 48 col
        int colCant = 4;
        int colPrec = 9;
        int colImp  = 9;

        b.bold(true);
        b.line(String.format("%-" + colProd + "s %" + colCant + "s %" + colPrec + "s %" + colImp + "s",
                "PRODUCTO", "CANT", "P.UNIT", "IMPORTE"));
        b.bold(false);
        b.separator(w);

        /* ---------- Items ---------- */
        for (ItemCarrito it : items) {
            String nombre = recortar(it.getProducto().getNombre(), colProd);
            String cant   = String.valueOf(it.getCantidad());
            String prec   = String.format("%.2f", it.getProducto().getPrecioVenta());
            String imp    = String.format("%.2f", it.getImporte());

            b.line(String.format("%-" + colProd + "s %" + colCant + "s %" + colPrec + "s %" + colImp + "s",
                    nombre, cant, prec, imp));
        }

        b.separator(w);

        /* ---------- Totales ---------- */
        b.line(alinear("Subtotal:",   MXN.format(subtotal), w));
        if (descuento > 0)
            b.line(alinear("Descuento:", "-" + MXN.format(descuento), w));
        b.line(alinear("IVA 16%:",    MXN.format(iva), w));
        b.bold(true).line(alinear("TOTAL:", MXN.format(total), w)).bold(false);
        b.blank();

        b.line(alinear("Método:", metodoPago, w));
        if ("EFECTIVO".equalsIgnoreCase(metodoPago)) {
            b.line(alinear("Recibido:", MXN.format(recibido), w));
            b.line(alinear("Cambio:",   MXN.format(cambio), w));
        }

        /* ---------- Pie ---------- */
        b.blank();
        b.alignCenter()
                .line(cfg.getTicketFooter())
                .line("¡Vuelva pronto!")
                .alignLeft();
        b.blank().blank();

        /* ---------- Cajón + corte ---------- */
        if (abrirCajon) b.openDrawer();
        if (cfg.isCortar()) b.cut();

        return b.build();
    }

    /* ============================================================
       Ticket de prueba (para botón "Imprimir prueba")
       ============================================================ */
    public byte[] construirPrueba() {
        int w = cfg.getAncho();
        EscPosBuilder b = new EscPosBuilder(cfg.getCharset());
        b.init()
                .alignCenter().bold(true).line("TICKET DE PRUEBA").bold(false)
                .line(cfg.getNegocioNombre())
                .separator(w)
                .alignLeft()
                .line("Ancho papel:  " + w + " caracteres")
                .line("Charset:      " + cfg.getCharset())
                .line("Cortar:       " + cfg.isCortar())
                .line("Abrir cajón:  " + cfg.isAbrirCajon())
                .blank()
                .alignCenter()
                .line("Si ves este ticket, la impresora")
                .line("está correctamente configurada.")
                .alignLeft()
                .blank().blank();

        if (cfg.isAbrirCajon()) b.openDrawer();
        if (cfg.isCortar())     b.cut();
        return b.build();
    }

    /* ============================================================
       Helpers de alineación
       ============================================================ */
    private String alinear(String izq, String der, int w) {
        int espacios = w - izq.length() - der.length();
        if (espacios < 1) espacios = 1;
        return izq + " ".repeat(espacios) + der;
    }

    private String recortar(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}