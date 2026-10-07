package org.papeleria_pos.models;

public class Proveedor {
    private int idProveedor;
    private String nombre;
    private String contacto;
    private String telefono;
    private String email;
    private boolean activo = true;

    public Proveedor() {}

    public int getIdProveedor()                { return idProveedor; }
    public void setIdProveedor(int id)         { this.idProveedor = id; }
    public String getNombre()                  { return nombre; }
    public void setNombre(String n)            { this.nombre = n; }
    public String getContacto()                { return contacto; }
    public void setContacto(String c)          { this.contacto = c; }
    public String getTelefono()                { return telefono; }
    public void setTelefono(String t)          { this.telefono = t; }
    public String getEmail()                   { return email; }
    public void setEmail(String e)             { this.email = e; }
    public boolean isActivo()                  { return activo; }
    public void setActivo(boolean a)           { this.activo = a; }
}