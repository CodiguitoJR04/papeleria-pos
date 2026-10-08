package org.papeleria_pos.dto;

import java.time.LocalDateTime;

public class MovimientoResumen {
    private int            idMovimiento;
    private LocalDateTime  fecha;
    private String         producto;
    private String         tipo;          // SALIDA_VENTA, ENTRADA, MERMA, AJUSTE
    private int            cantidad;
    private String         motivo;
    private String         usuario;
    private String         referencia;    // 1042, L-2026-09-001, AJ-001, —

    public MovimientoResumen() {}

    public MovimientoResumen(int idMovimiento, LocalDateTime fecha, String producto,
                             String tipo, int cantidad, String motivo,
                             String usuario, String referencia) {
        this.idMovimiento = idMovimiento;
        this.fecha        = fecha;
        this.producto     = producto;
        this.tipo         = tipo;
        this.cantidad     = cantidad;
        this.motivo       = motivo;
        this.usuario      = usuario;
        this.referencia   = referencia;
    }

    public int           getIdMovimiento()               { return idMovimiento; }
    public void          setIdMovimiento(int v)          { this.idMovimiento = v; }
    public LocalDateTime getFecha()                      { return fecha; }
    public void          setFecha(LocalDateTime v)       { this.fecha = v; }
    public String        getProducto()                   { return producto; }
    public void          setProducto(String v)           { this.producto = v; }
    public String        getTipo()                       { return tipo; }
    public void          setTipo(String v)               { this.tipo = v; }
    public int           getCantidad()                   { return cantidad; }
    public void          setCantidad(int v)              { this.cantidad = v; }
    public String        getMotivo()                     { return motivo; }
    public void          setMotivo(String v)             { this.motivo = v; }
    public String        getUsuario()                    { return usuario; }
    public void          setUsuario(String v)            { this.usuario = v; }
    public String        getReferencia()                 { return referencia; }
    public void          setReferencia(String v)         { this.referencia = v; }

    /** true si resta stock (venta/merma). */
    public boolean esSalida() {
        return "SALIDA_VENTA".equals(tipo) || "MERMA".equals(tipo);
    }
}