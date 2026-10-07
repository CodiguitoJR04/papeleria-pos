package org.papeleria_pos.models;

import java.time.LocalDateTime;

public class Turno {
    private int idTurno;
    private int idCajero;
    private int caja;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private double montoInicial;
    private double montoFinal;
    private double totalVentas;
    private String estado;

    public Turno() {}

    public int getIdTurno()                   { return idTurno; }
    public void setIdTurno(int id)            { this.idTurno = id; }
    public int getIdCajero()                  { return idCajero; }
    public void setIdCajero(int id)           { this.idCajero = id; }
    public int getCaja()                      { return caja; }
    public void setCaja(int caja)             { this.caja = caja; }
    public LocalDateTime getFechaInicio()     { return fechaInicio; }
    public void setFechaInicio(LocalDateTime f) { this.fechaInicio = f; }
    public LocalDateTime getFechaFin()        { return fechaFin; }
    public void setFechaFin(LocalDateTime f)  { this.fechaFin = f; }
    public double getMontoInicial()           { return montoInicial; }
    public void setMontoInicial(double m)     { this.montoInicial = m; }
    public double getMontoFinal()             { return montoFinal; }
    public void setMontoFinal(double m)       { this.montoFinal = m; }
    public double getTotalVentas()            { return totalVentas; }
    public void setTotalVentas(double t)      { this.totalVentas = t; }
    public String getEstado()                 { return estado; }
    public void setEstado(String e)           { this.estado = e; }
}