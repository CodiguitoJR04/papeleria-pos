package org.papeleria_pos.models;

public class DetalleVenta {
    private int idDetalle;
    private int idVenta;
    private int idProducto;
    private int cantidad;
    private double precioUnitario;
    private double importe;

    public DetalleVenta() {}

    public int getIdDetalle()               { return idDetalle; }
    public void setIdDetalle(int id)        { this.idDetalle = id; }
    public int getIdVenta()                 { return idVenta; }
    public void setIdVenta(int id)          { this.idVenta = id; }
    public int getIdProducto()              { return idProducto; }
    public void setIdProducto(int id)       { this.idProducto = id; }
    public int getCantidad()                { return cantidad; }
    public void setCantidad(int c)          { this.cantidad = c; }
    public double getPrecioUnitario()       { return precioUnitario; }
    public void setPrecioUnitario(double p){ this.precioUnitario = p; }
    public double getImporte()              { return importe; }
    public void setImporte(double i)        { this.importe = i; }
}