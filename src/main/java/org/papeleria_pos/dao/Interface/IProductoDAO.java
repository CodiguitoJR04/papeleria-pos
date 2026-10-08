package org.papeleria_pos.dao.Interface;

import org.papeleria_pos.models.Producto;
import java.util.List;

public interface IProductoDAO {

    /* ---------- Lectura ---------- */
    List<Producto> listarTodos();
    List<Producto> buscar(String filtro);
    List<Producto> buscar(String filtro, Integer idCategoria, Boolean soloStockBajo);
    List<Producto> buscarCoincidencias(String query, int limite);
    int contarStockBajo();
    List<Producto> obtenerStockBajo(int limite);
    List<Producto> obtenerAlertasStock(int limite);

    /* ---------- Escritura ---------- */
    /** @return id generado, o -1 si falló. */
    int insertar(Producto p);

    /** @return true si actualizó al menos 1 fila. */
    boolean actualizar(Producto p);

    /** @return true si eliminó/desactivó al menos 1 fila. */
    boolean eliminar(int idProducto);

    /** @return id_categoria o null si no existe. */
    Integer obtenerIdCategoriaPorNombre(String nombre);
    /** Lista los productos activos asignados a un proveedor. */
    List<Producto> listarPorProveedor(int idProveedor, boolean soloStockBajo);
    List<Producto> buscarPorProveedor(int idProveedor, Boolean soloStockBajo);
}