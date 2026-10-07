package org.papeleria_pos.models;

import java.time.LocalDateTime;

public class Pago {
    private int idPago;
    private int idVenta;
    private String tipoPago;
    private double monto;
    private double montoRecibido;
    private double cambio;
    private LocalDateTime fechaPago;

    public Pago() {}

    public int getIdPago()                  { return idPago; }
    public void setIdPago(int id)           { this.idPago = id; }
    public int getIdVenta()                 { return idVenta; }
    public void setIdVenta(int id)          { this.idVenta = id; }
    public String getTipoPago()             { return tipoPago; }
    public void setTipoPago(String t)       { this.tipoPago = t; }
    public double getMonto()                { return monto; }
    public void setMonto(double m)          { this.monto = m; }
    public double getMontoRecibido()        { return montoRecibido; }
    public void setMontoRecibido(double m)  { this.montoRecibido = m; }
    public double getCambio()               { return cambio; }
    public void setCambio(double c)         { this.cambio = c; }
    public LocalDateTime getFechaPago()     { return fechaPago; }
    public void setFechaPago(LocalDateTime f) { this.fechaPago = f; }
}