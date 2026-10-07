package org.papeleria_pos.dao.Interface;

import org.papeleria_pos.dto.UsuarioResumen;

import java.util.List;

public interface IUsuarioDAO {
    List<UsuarioResumen> listarTodos();
    List<UsuarioResumen> buscar(String filtro, String rol, Boolean activo);
    boolean insertar(String nombre, String username, String password, String rol, boolean activo);
    boolean actualizar(int id, String nombre, String username, String password, String rol, boolean activo);
    boolean eliminar(int id);
    int contarActivos();

    /** Devuelve el rol si las credenciales son válidas, o null si no. */
    String autenticar(String username, String password);
}
