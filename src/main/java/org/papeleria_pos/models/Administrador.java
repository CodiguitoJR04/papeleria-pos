package org.papeleria_pos.models;

public class Administrador extends Usuario {

    public Administrador(int id_user, String nombre, String username, String password, String rol){
        super(id_user,nombre,username,password,rol);
    }

    public void gestionarInventario(){

    }

    public void verReporte(String periodo){

    }

    public void gestionarUsuarios(){

    }

    public void autorizarAjuste(){

    }

}
