package org.papeleria_pos.models;

public class DashboardResumen {

    private final double ventasHoy;
    private final double ticketPromedioHoy;
    private final int productosStockBajo;
    private final int turnosAbiertos;
    private final double ventasAyer;
    private final double ticketAyer;

    public DashboardResumen(double ventasHoy, double ticketPromedioHoy, int productosStockBajo,
                            int turnosAbiertos, double ventasAyer, double ticketAyer) {
        this.ventasHoy = ventasHoy;
        this.ticketPromedioHoy = ticketPromedioHoy;
        this.productosStockBajo = productosStockBajo;
        this.turnosAbiertos = turnosAbiertos;
        this.ventasAyer = ventasAyer;
        this.ticketAyer = ticketAyer;
    }

    // Getters
    public double getVentasHoy() {
        return ventasHoy;
    }

    public double getTicketPromedioHoy() {
        return ticketPromedioHoy;
    }

    public int getProductosStockBajo() {
        return productosStockBajo;
    }

    public int getTurnosAbiertos() {
        return turnosAbiertos;
    }

    public double getVentasAyer() {
        return ventasAyer;
    }

    public double getTicketAyer() {
        return ticketAyer;
    }

    // Métodos de cálculo de porcentaje
    public double deltaVentasPct() {
        if (ventasAyer == 0) return ventasHoy > 0 ? 100.0 : 0.0;
        return ((ventasHoy - ventasAyer) / ventasAyer) * 100.0;
    }

    public double deltaTicketPct() {
        if (ticketAyer == 0) return ticketPromedioHoy > 0 ? 100.0 : 0.0;
        return ((ticketPromedioHoy - ticketAyer) / ticketAyer) * 100.0;
    }
}