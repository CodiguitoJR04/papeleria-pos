package org.papeleria_pos.dto;

import java.util.Objects;

public class AlertaStock {
    private final int idProducto;
    private final String nombre;
    private final int stockActual;
    private final int stockMinimo;

    public AlertaStock(int idProducto, String nombre, int stockActual, int stockMinimo) {
        this.idProducto  = idProducto;
        this.nombre      = nombre;
        this.stockActual = stockActual;
        this.stockMinimo = stockMinimo;
    }

    public int getIdProducto() { return idProducto; }
    public String getNombre()  { return nombre; }
    public int getStockActual(){ return stockActual; }
    public int getStockMinimo(){ return stockMinimo; }

    /** Nivel de alerta para pintar el badge: "out", "low" u "ok". */
    public String nivel() {
        if (stockActual == 0) return "out";
        if (stockActual <= stockMinimo) return "low";
        return "ok";
    }

    @Override
    public String toString() {
        return "AlertaStock{id=" + idProducto + ", nombre='" + nombre + '\'' +
                ", actual=" + stockActual + ", minimo=" + stockMinimo + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AlertaStock)) return false;
        AlertaStock a = (AlertaStock) o;
        return idProducto == a.idProducto
                && stockActual == a.stockActual
                && stockMinimo == a.stockMinimo
                && Objects.equals(nombre, a.nombre);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idProducto, nombre, stockActual, stockMinimo);
    }
}
