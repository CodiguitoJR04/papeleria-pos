package org.papeleria_pos.dto;

import java.util.List;

public class VentaRequest {
    private final int idCajero;
    private final int idTurno;
    private final String metodoPago;      // EFECTIVO / TARJETA
    private final double montoRecibido;
    private final List<ItemCarrito> items;

    public VentaRequest(int idCajero, int idTurno, String metodoPago,
                        double montoRecibido, List<ItemCarrito> items) {
        this.idCajero = idCajero;
        this.idTurno = idTurno;
        this.metodoPago = metodoPago;
        this.montoRecibido = montoRecibido;
        this.items = items;
    }

    public int getIdCajero()                { return idCajero; }
    public int getIdTurno()                 { return idTurno; }
    public String getMetodoPago()           { return metodoPago; }
    public double getMontoRecibido()        { return montoRecibido; }
    public List<ItemCarrito> getItems()     { return items; }
}
