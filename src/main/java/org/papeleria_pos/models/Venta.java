package org.papeleria_pos.models;

import java.time.LocalDateTime;

public class Venta {
    private int idVenta;
    private int folio;
    private LocalDateTime fecha;
    private int idUsuario;
    private String usuario;
    private String metodoPago;
    private double subtotal;
    private double iva;
    private double total;
    private String estado;

    public Venta() {}

    public int getIdVenta()                   { return idVenta; }
    public void setIdVenta(int id)            { this.idVenta = id; }
    public int getFolio()                     { return folio; }
    public void setFolio(int folio)           { this.folio = folio; }
    public LocalDateTime getFecha()           { return fecha; }
    public void setFecha(LocalDateTime f)     { this.fecha = f; }
    public int getIdUsuario()                 { return idUsuario; }
    public void setIdUsuario(int id)          { this.idUsuario = id; }
    public String getUsuario()                { return usuario; }
    public void setUsuario(String u)          { this.usuario = u; }
    public String getMetodoPago()             { return metodoPago; }
    public void setMetodoPago(String m)       { this.metodoPago = m; }
    public double getSubtotal()               { return subtotal; }
    public void setSubtotal(double s)         { this.subtotal = s; }
    public double getIva()                    { return iva; }
    public void setIva(double iva)            { this.iva = iva; }
    public double getTotal()                  { return total; }
    public void setTotal(double total)        { this.total = total; }
    public String getEstado()                 { return estado; }
    public void setEstado(String estado)      { this.estado = estado; }
}