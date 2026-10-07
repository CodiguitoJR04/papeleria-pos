package org.papeleria_pos.dto;

import java.util.Objects;

public class TopProducto {
    private final int idProducto;
    private final String nombre;
    private final int unidadesVendidas;
    private final double importeTotal;

    public TopProducto(int idProducto, String nombre,
                       int unidadesVendidas, double importeTotal) {
        this.idProducto       = idProducto;
        this.nombre           = nombre;
        this.unidadesVendidas = unidadesVendidas;
        this.importeTotal     = importeTotal;
    }

    public int getIdProducto()        { return idProducto; }
    public String getNombre()         { return nombre; }
    public int getUnidadesVendidas()  { return unidadesVendidas; }
    public double getImporteTotal()   { return importeTotal; }

    @Override
    public String toString() {
        return "TopProducto{id=" + idProducto + ", nombre='" + nombre + '\'' +
                ", unidades=" + unidadesVendidas + ", importe=" + importeTotal + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TopProducto)) return false;
        TopProducto t = (TopProducto) o;
        return idProducto == t.idProducto
                && unidadesVendidas == t.unidadesVendidas
                && Double.compare(t.importeTotal, importeTotal) == 0
                && Objects.equals(nombre, t.nombre);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idProducto, nombre, unidadesVendidas, importeTotal);
    }
}
