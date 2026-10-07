package org.papeleria_pos.dto;
import org.papeleria_pos.models.Producto;


public class ItemCarrito {
    private final Producto producto;
    private int cantidad;

    public ItemCarrito(Producto producto, int cantidad) {
        this.producto = producto;
        this.cantidad = cantidad;
    }

    public Producto getProducto() { return producto; }
    public int getCantidad()      { return cantidad; }
    public void setCantidad(int c){ this.cantidad = c; }

    public double getImporte()    { return producto.getPrecioVenta() * cantidad; }
    public double getCosto()      { return producto.getPrecioCompra() * cantidad; }
}
