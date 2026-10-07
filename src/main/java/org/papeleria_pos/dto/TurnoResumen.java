package org.papeleria_pos.dto;

import java.time.LocalDateTime;

public class TurnoResumen {
    private final int id;
    private final int caja;
    private final String cajero;
    private final LocalDateTime apertura;
    private final LocalDateTime cierre;
    private final int numVentas;
    private final double totalVentas;
    private final double diferencia;
    private final String estado;

    public TurnoResumen(int id, int caja, String cajero,
                        LocalDateTime apertura, LocalDateTime cierre,
                        int numVentas, double totalVentas,
                        double diferencia, String estado) {
        this.id = id;
        this.caja = caja;
        this.cajero = cajero;
        this.apertura = apertura;
        this.cierre = cierre;
        this.numVentas = numVentas;
        this.totalVentas = totalVentas;
        this.diferencia = diferencia;
        this.estado = estado;
    }

    public int getId()                  { return id; }
    public int getCaja()                { return caja; }
    public String getCajero()           { return cajero; }
    public LocalDateTime getApertura()  { return apertura; }
    public LocalDateTime getCierre()    { return cierre; }
    public int getNumVentas()           { return numVentas; }
    public double getTotalVentas()      { return totalVentas; }
    public double getDiferencia()       { return diferencia; }
    public String getEstado()           { return estado; }
}
