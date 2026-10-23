package org.papeleria_pos.dto;

import java.time.LocalDate;

public class FacturaResumen {
    private final int idFactura;
    private final String serie;
    private final int folio;
    private final LocalDate fecha;
    private final String rfc;
    private final String razonSocial;
    private final double total;
    private final String estado;

    // 🔑 CAMBIAR ESTO A String
    private final String folioVenta;

    public FacturaResumen(int idFactura, String serie, int folio, LocalDate fecha,
                          String rfc, String razonSocial, double total,
                          String estado, String folioVenta) { // 🔑 CAMBIAR A String
        this.idFactura = idFactura;
        this.serie = serie;
        this.folio = folio;
        this.fecha = fecha;
        this.rfc = rfc;
        this.razonSocial = razonSocial;
        this.total = total;
        this.estado = estado;
        this.folioVenta = folioVenta;
    }

    public int getIdFactura()        { return idFactura; }
    public String getSerie()         { return serie; }
    public int getFolio()            { return folio; }
    public LocalDate getFecha()      { return fecha; }
    public String getRfc()           { return rfc; }
    public String getRazonSocial()   { return razonSocial; }
    public double getTotal()         { return total; }
    public String getEstado()        { return estado; }

    // 🔑 CAMBIAR A String
    public String getFolioVenta()    { return folioVenta; }

    public String getSerieFolio()    { return serie + "-" + folio; }
}