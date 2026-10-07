package org.papeleria_pos.models;

public class Producto {
    private int idProducto;
    private String sku;
    private String nombre;
    private int idCategoria;
    private String nombreCategoria;   // 🔑 NUEVO
    private double precioCompra;
    private double precioVenta;
    private int stockActual;
    private int stockMinimo;
    private boolean activo = true;
    private int    idProveedor;

    public Producto() {}

    public int getIdProducto()                      { return idProducto; }
    public void setIdProducto(int idProducto)       { this.idProducto = idProducto; }

    public String getSku()                          { return sku; }
    public void setSku(String sku)                  { this.sku = sku; }

    public String getNombre()                       { return nombre; }
    public void setNombre(String nombre)            { this.nombre = nombre; }

    public int getIdCategoria()                     { return idCategoria; }
    public void setIdCategoria(int idCategoria)     { this.idCategoria = idCategoria; }

    public String getNombreCategoria()              { return nombreCategoria; }   // 🔑 NUEVO
    public void setNombreCategoria(String n)        { this.nombreCategoria = n; } // 🔑 NUEVO

    public double getPrecioCompra()                 { return precioCompra; }
    public void setPrecioCompra(double precioCompra){ this.precioCompra = precioCompra; }

    public double getPrecioVenta()                  { return precioVenta; }
    public void setPrecioVenta(double precioVenta)  { this.precioVenta = precioVenta; }

    public int getStockActual()                     { return stockActual; }
    public void setStockActual(int stockActual)     { this.stockActual = stockActual; }

    public int getStockMinimo()                     { return stockMinimo; }
    public void setStockMinimo(int stockMinimo)     { this.stockMinimo = stockMinimo; }

    public boolean isActivo()                       { return activo; }
    public void setActivo(boolean activo)           { this.activo = activo; }

    public int getIdProveedor()                      { return idProveedor; }
    public void setIdProveedor(int v)                { this.idProveedor = v; }
}