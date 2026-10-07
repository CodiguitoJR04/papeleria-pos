package org.papeleria_pos.dao.Interface;

import org.papeleria_pos.dto.CategoriaResumen;

import java.util.List;

public interface ICategoriaDAO {
    List<CategoriaResumen> listarTodas();
    List<CategoriaResumen> buscar(String filtro);
    boolean insertar(String nombre, String descripcion, boolean activa);
    boolean actualizar(int id, String nombre, String descripcion, boolean activa);
    boolean eliminar(int id);
    int contarActivas();
}
