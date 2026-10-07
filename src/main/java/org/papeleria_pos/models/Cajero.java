package org.papeleria_pos.models;

public class Cajero extends Usuario {
    String turno;

    public Cajero(String turno, int id_user, String nombre, String username, String password, String rol) {
        super(id_user, nombre, username, password, rol);
        turno = this.turno;
    }

    public String getTurno() {
        return turno;
    }

    public void setTurno(String turno) {
        this.turno = turno;
    }

    public void iniciarTurno(){

    }

    public void cerrarTurno(){

    }

}
