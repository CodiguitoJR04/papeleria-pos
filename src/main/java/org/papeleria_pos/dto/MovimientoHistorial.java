package org.papeleria_pos.dto;

import java.time.LocalDateTime;

public class MovimientoHistorial {
    private final LocalDateTime fecha;
    private final String producto;
    private final String tipo;
    private final int cantidad;
    private final String motivo;
    private final String usuario;
    private final String referencia;

    public MovimientoHistorial(LocalDateTime fecha, String producto, String tipo,
                               int cantidad, String motivo, String usuario, String referencia) {
        this.fecha = fecha;
        this.producto = producto;
        this.tipo = tipo;
        this.cantidad = cantidad;
        this.motivo = motivo;
        this.usuario = usuario;
        this.referencia = referencia;
    }

    public LocalDateTime getFecha()  { return fecha; }
    public String getProducto()      { return producto; }
    public String getTipo()          { return tipo; }
    public int getCantidad()         { return cantidad; }
    public String getMotivo()        { return motivo; }
    public String getUsuario()       { return usuario; }
    public String getReferencia()    { return referencia; }
}
