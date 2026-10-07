package org.papeleria_pos.models;

import java.time.LocalDateTime;

public class MovimientoInventario {
    private int idMovimiento;
    private int idProducto;
    private int idAdministrador;
    private String tipo;
    private int cantidad;
    private LocalDateTime fecha;
    private String motivo;

    public MovimientoInventario() {}

    public int getIdMovimiento()               { return idMovimiento; }
    public void setIdMovimiento(int id)        { this.idMovimiento = id; }
    public int getIdProducto()                 { return idProducto; }
    public void setIdProducto(int id)          { this.idProducto = id; }
    public int getIdAdministrador()            { return idAdministrador; }
    public void setIdAdministrador(int id)     { this.idAdministrador = id; }
    public String getTipo()                    { return tipo; }
    public void setTipo(String t)              { this.tipo = t; }
    public int getCantidad()                   { return cantidad; }
    public void setCantidad(int c)             { this.cantidad = c; }
    public LocalDateTime getFecha()            { return fecha; }
    public void setFecha(LocalDateTime f)      { this.fecha = f; }
    public String getMotivo()                  { return motivo; }
    public void setMotivo(String m)            { this.motivo = m; }
}