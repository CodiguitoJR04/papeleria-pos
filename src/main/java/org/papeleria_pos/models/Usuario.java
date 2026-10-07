package org.papeleria_pos.models;

public class Usuario {
    int id_user;
    String nombre;
    String username;
    String password;
    String rol;

    //Metodos que realiza el usuario
    public boolean login(){
        return true;
    }
    public void Logout(){

    }

    //Hacemos los metodos Get and Setter

    public Usuario(int id_user, String nombre,String username,String password, String rol){
        id_user=this.id_user;
        nombre=this.nombre;
        username=this.username;
        password=this.password;
        rol=this.rol;
    }

   /* public Usuario (){

    }*/

    public Usuario(int id_user, String nombre){

    }

    public int getId_user() {
        return id_user;
    }

    public void setId_user(int id_user) {
        this.id_user = id_user;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

}
