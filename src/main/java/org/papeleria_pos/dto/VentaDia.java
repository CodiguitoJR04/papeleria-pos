package org.papeleria_pos.dto;

import java.time.LocalDate;
import java.util.Objects;

public class VentaDia {
    private final LocalDate fecha;
    private final double total;

    public VentaDia(LocalDate fecha, double total) {
        this.fecha = fecha;
        this.total = total;
    }

    public LocalDate getFecha() { return fecha; }
    public double getTotal()    { return total; }

    @Override
    public String toString() {
        return "VentaDia{fecha=" + fecha + ", total=" + total + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VentaDia)) return false;
        VentaDia v = (VentaDia) o;
        return Double.compare(v.total, total) == 0 && Objects.equals(fecha, v.fecha);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fecha, total);
    }
}
