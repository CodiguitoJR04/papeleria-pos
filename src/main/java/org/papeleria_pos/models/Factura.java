package org.papeleria_pos.models;

import java.time.LocalDateTime;

public class Factura {
    private int idFactura;
    private int idVenta;
    private int idCliente;
    private String serie;
    private int folio;
    private String uuid;
    private String rfcReceptor;
    private String razonSocial;
    private String regimenFiscal;
    private String usoCfdi;
    private double subtotal;
    private double iva;
    private double total;
    private String metodoPago;   // PUE | PPD
    private LocalDateTime fechaEmision;
    private String estado;       // GENERADA | TIMBRADA | CANCELADA
    private String xml;
    private byte[] pdf;

    public Factura() {}

    public int getIdFactura()                  { return idFactura; }
    public void setIdFactura(int v)            { this.idFactura = v; }

    public int getIdVenta()                    { return idVenta; }
    public void setIdVenta(int v)              { this.idVenta = v; }

    public int getIdCliente()                  { return idCliente; }
    public void setIdCliente(int v)            { this.idCliente = v; }

    public String getSerie()                   { return serie; }
    public void setSerie(String v)             { this.serie = v; }

    public int getFolio()                      { return folio; }
    public void setFolio(int v)                { this.folio = v; }

    public String getUuid()                    { return uuid; }
    public void setUuid(String v)              { this.uuid = v; }

    public String getRfcReceptor()             { return rfcReceptor; }
    public void setRfcReceptor(String v)       { this.rfcReceptor = v; }

    public String getRazonSocial()             { return razonSocial; }
    public void setRazonSocial(String v)       { this.razonSocial = v; }

    public String getRegimenFiscal()           { return regimenFiscal; }
    public void setRegimenFiscal(String v)     { this.regimenFiscal = v; }

    public String getUsoCfdi()                 { return usoCfdi; }
    public void setUsoCfdi(String v)           { this.usoCfdi = v; }

    public double getSubtotal()                { return subtotal; }
    public void setSubtotal(double v)          { this.subtotal = v; }

    public double getIva()                     { return iva; }
    public void setIva(double v)               { this.iva = v; }

    public double getTotal()                   { return total; }
    public void setTotal(double v)             { this.total = v; }

    public String getMetodoPago()              { return metodoPago; }
    public void setMetodoPago(String v)        { this.metodoPago = v; }

    public LocalDateTime getFechaEmision()     { return fechaEmision; }
    public void setFechaEmision(LocalDateTime v) { this.fechaEmision = v; }

    public String getEstado()                  { return estado; }
    public void setEstado(String v)            { this.estado = v; }

    public String getXml()                     { return xml; }
    public void setXml(String v)               { this.xml = v; }

    public byte[] getPdf()                     { return pdf; }
    public void setPdf(byte[] v)               { this.pdf = v; }

    /** Texto formateado para mostrar en la UI. */
    public String getSerieFolio()              { return serie + "-" + folio; }
}