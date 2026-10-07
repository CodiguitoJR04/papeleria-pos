package org.papeleria_pos.dto;

import java.time.LocalDateTime;
import java.util.Objects;

public class VentaReciente {
    private final int folio;
    private final String cajero;
    private final LocalDateTime fecha;
    private final double total;
    private final String estado;

    public VentaReciente(int folio, String cajero, LocalDateTime fecha,
                         double total, String estado) {
        this.folio  = folio;
        this.cajero = cajero;
        this.fecha  = fecha;
        this.total  = total;
        this.estado = estado;
    }

    public int getFolio()           { return folio; }
    public String getCajero()       { return cajero; }
    public LocalDateTime getFecha() { return fecha; }
    public double getTotal()        { return total; }
    public String getEstado()       { return estado; }

    @Override
    public String toString() {
        return "VentaReciente{folio=" + folio + ", cajero='" + cajero + '\'' +
                ", fecha=" + fecha + ", total=" + total + ", estado='" + estado + "'}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VentaReciente)) return false;
        VentaReciente v = (VentaReciente) o;
        return folio == v.folio
                && Double.compare(v.total, total) == 0
                && Objects.equals(cajero, v.cajero)
                && Objects.equals(fecha, v.fecha)
                && Objects.equals(estado, v.estado);
    }

    @Override
    public int hashCode() {
        return Objects.hash(folio, cajero, fecha, total, estado);
    }
}
