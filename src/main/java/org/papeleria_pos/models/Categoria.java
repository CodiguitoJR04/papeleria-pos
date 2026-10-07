package org.papeleria_pos.models;

public class Categoria {
    private int idCategoria;
    private String nombre;
    private String descripcion;
    private boolean activo = true;

    public Categoria() {}

    public int getIdCategoria()                   { return idCategoria; }
    public void setIdCategoria(int id)            { this.idCategoria = id; }
    public String getNombre()                     { return nombre; }
    public void setNombre(String nombre)          { this.nombre = nombre; }
    public String getDescripcion()                { return descripcion; }
    public void setDescripcion(String d)          { this.descripcion = d; }
    public boolean isActivo()                     { return activo; }
    public void setActivo(boolean activo)         { this.activo = activo; }
}