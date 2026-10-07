package org.papeleria_pos.session;

public class Sesion {
    private static Sesion instancia;

    public static Sesion get() {
        if (instancia == null) instancia = new Sesion();
        return instancia;
    }

    private int idUsuario;
    private String nombre, username, rol;

    private Sesion() {}

    public void iniciar(int idUsuario, String nombre, String username, String rol) {
        this.idUsuario = idUsuario;
        this.nombre = nombre;
        this.username = username;
        this.rol = rol;
    }

    public void cerrar() {
        idUsuario = 0; nombre = username = rol = null;
    }

    public int getIdUsuario()   { return idUsuario; }
    public String getNombre()   { return nombre; }
    public String getUsername() { return username; }
    public String getRol()      { return rol; }

    public boolean esAdmin()    { return "Administrador".equals(rol); }
    public boolean esCajero()   { return "Cajero".equals(rol); }
}