package org.papeleria_pos.dto;

public class CategoriaResumen {
    private final int id;
    private final String nombre;
    private final String descripcion;
    private final int numProductos;
    private final boolean activa;

    public CategoriaResumen(int id, String nombre, String descripcion,
                            int numProductos, boolean activa) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.numProductos = numProductos;
        this.activa = activa;
    }

    public int getId()              { return id; }
    public String getNombre()       { return nombre; }
    public String getDescripcion()  { return descripcion; }
    public int getNumProductos()    { return numProductos; }
    public boolean isActiva()       { return activa; }

    /** Texto para la columna Estado en la tabla. */
    public String getEstado()       { return activa ? "Activa" : "Inactiva"; }
}
