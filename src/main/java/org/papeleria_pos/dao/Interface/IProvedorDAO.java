
package org.papeleria_pos.dao.Interface;

import org.papeleria_pos.models.Proveedor;
import java.util.List;

public interface IProvedorDAO {
    List<Proveedor> listarTodos();
    List<Proveedor> buscar(String filtro);
    Proveedor       buscarPorId(int id);
    int             insertar(Proveedor p);
    boolean         actualizar(Proveedor p);
    boolean         eliminar(int id);
    Proveedor buscarPorNombre(String nombre);
}