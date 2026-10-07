package org.papeleria_pos.dto;

import java.time.LocalDateTime;

public class UsuarioResumen {
    private final int id;
    private final String nombre;
    private final String username;
    private final String rol;
    private final boolean activo;
    private final LocalDateTime ultimoAcceso;
    private final int ventasHoy;

    public UsuarioResumen(int id, String nombre, String username, String rol,
                          boolean activo, LocalDateTime ultimoAcceso, int ventasHoy) {
        this.id = id;
        this.nombre = nombre;
        this.username = username;
        this.rol = rol;
        this.activo = activo;
        this.ultimoAcceso = ultimoAcceso;
        this.ventasHoy = ventasHoy;
    }

    public int getId()                  { return id; }
    public String getNombre()           { return nombre; }
    public String getUsername()         { return username; }
    public String getRol()              { return rol; }
    public boolean isActivo()           { return activo; }
    public LocalDateTime getUltimoAcceso(){ return ultimoAcceso; }
    public int getVentasHoy()           { return ventasHoy; }

    public String getEstado()           { return activo ? "Activo" : "Inactivo"; }

    /** Texto amigable para la columna "Último acceso". */
    public String getUltimoAccesoTexto() {
        if (ultimoAcceso == null) return "Nunca";
        LocalDateTime ahora = LocalDateTime.now();
        long dias = java.time.Duration.between(ultimoAcceso, ahora).toDays();
        if (dias == 0) return "Hoy";
        if (dias == 1) return "Ayer";
        if (dias < 30) return "Hace " + dias + " días";
        return ultimoAcceso.toLocalDate().toString();
    }

    /** Iniciales para el avatar. */
    public String getIniciales() {
        if (nombre == null || nombre.isEmpty()) return "?";
        String[] partes = nombre.trim().split("\\s+");
        if (partes.length == 1) return partes[0].substring(0, 1).toUpperCase();
        return (partes[0].substring(0, 1) + partes[1].substring(0, 1)).toUpperCase();
    }
}
