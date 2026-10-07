package org.papeleria_pos.dto;

import java.time.LocalDateTime;

public class TurnoAbierto {
    private final int idTurno;
    private final int idCajero;
    private final String cajero;
    private final int caja;
    private final LocalDateTime fechaInicio;
    private final double montoInicial;

    public TurnoAbierto(int idTurno, int idCajero, String cajero, int caja,
                        LocalDateTime fechaInicio, double montoInicial) {
        this.idTurno = idTurno;
        this.idCajero = idCajero;
        this.cajero = cajero;
        this.caja = caja;
        this.fechaInicio = fechaInicio;
        this.montoInicial = montoInicial;
    }

    public int getIdTurno()               { return idTurno; }
    public int getIdCajero()              { return idCajero; }
    public String getCajero()             { return cajero; }
    public int getCaja()                  { return caja; }
    public LocalDateTime getFechaInicio() { return fechaInicio; }
    public double getMontoInicial()       { return montoInicial; }
}