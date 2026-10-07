package org.papeleria_pos.hardware;

import java.io.InputStream;
import java.util.Properties;

/**
 * Lee la configuración de /impresora.properties.
 * Si algo falta, usa valores por defecto que funcionan con casi cualquier impresora.
 */
public class PrinterConfig {

    private static PrinterConfig instance;
    private final Properties props = new Properties();

    private PrinterConfig() {
        try (InputStream in = PrinterConfig.class.getResourceAsStream("/impresora.properties")) {
            if (in != null) {
                props.load(in);
                System.out.println("✔ impresora.properties cargado.");
            } else {
                System.out.println("⚠ impresora.properties no encontrado. Usando valores por defecto.");
            }
        } catch (Exception e) {
            System.err.println("⚠ Error leyendo impresora.properties: " + e.getMessage());
        }
    }

    public static synchronized PrinterConfig get() {
        if (instance == null) instance = new PrinterConfig();
        return instance;
    }

    /* ---------- Impresora ---------- */
    public String  getNombreImpresora()        { return props.getProperty("impresora.nombre", "DEFAULT").trim(); }
    public int     getAncho()                  { return parseInt(props.getProperty("impresora.ancho", "48"), 48); }
    public String  getCharset()                { return props.getProperty("impresora.charset", "CP850").trim(); }
    public boolean isCortar()                  { return parseBool(props.getProperty("impresora.cortar", "true")); }
    public boolean isAbrirCajon()              { return parseBool(props.getProperty("impresora.abrirCajon", "true")); }
    public boolean isAbrirCajonSoloEfectivo()  { return parseBool(props.getProperty("impresora.abrirCajonSoloEfectivo", "true")); }
    public int     getCopias()                 { return parseInt(props.getProperty("impresora.copias", "1"), 1); }

    /* ---------- Negocio ---------- */
    public String  getNegocioNombre()    { return props.getProperty("negocio.nombre", "PAPELERÍA POS").trim(); }
    public String  getNegocioDireccion() { return props.getProperty("negocio.direccion", "").trim(); }
    public String  getNegocioTelefono()  { return props.getProperty("negocio.telefono", "").trim(); }
    public String  getNegocioRfc()       { return props.getProperty("negocio.rfc", "").trim(); }
    public String  getTicketFooter()     { return props.getProperty("ticket.footer", "¡Gracias por su compra!").trim(); }

    /* ---------- Helpers ---------- */
    private static int parseInt(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }

    private static boolean parseBool(String s) {
        return "true".equalsIgnoreCase(s == null ? "" : s.trim());
    }
}