package org.papeleria_pos.dao;

import org.papeleria_pos.config.DatabaseConnection;
import org.papeleria_pos.dao.Interface.IProductoDAO;
import org.papeleria_pos.models.Producto;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductoDAOImpl implements IProductoDAO {

    /* ============================================================
       Listar todos
       ============================================================ */
    @Override
    public List<Producto> listarTodos() {
        // Pasamos null en tipoProducto para traer Papelería y Skincare
        return buscar(null, null, null, null);
    }

    /* ============================================================
       Búsqueda simple
       ============================================================ */
    @Override
    public List<Producto> buscarCoincidencias(String query, int limite) {
        if (query == null || query.trim().isEmpty()) return List.of();

        String sql =
                "SELECT id_producto, sku, nombre, id_categoria, nombre_categoria, " +
                        "       precio_compra, precio_venta, stock_actual, stock_minimo, activo, " +
                        "       id_proveedor, tipo_producto " +
                        "  FROM producto " +
                        " WHERE activo = 1 AND ( " +
                        "       LOWER(nombre) LIKE ? " +
                        "    OR sku LIKE ? " +
                        "    OR LOWER(nombre_categoria) LIKE ? " +
                        "    OR CAST(precio_venta AS CHAR) LIKE ? " +
                        " ) " +
                        " ORDER BY nombre LIMIT ?";

        String like = "%" + query.trim().toLowerCase() + "%";
        List<Producto> lista = new ArrayList<>();
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setString(1, like);
            ps.setString(2, "%" + query.trim() + "%");
            ps.setString(3, like);
            ps.setString(4, "%" + query.trim() + "%");
            ps.setInt(5, limite);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando: " + e.getMessage(), e);
        }
        return lista;
    }

    @Override
    public List<Producto> buscar(String filtro) {
        return buscar(filtro, null, null, null);
    }

    /* ============================================================
       Búsqueda completa con filtros (Usada por el Inventario)
       ============================================================ */
    @Override
    public List<Producto> buscar(String filtro, Integer idCategoria, Boolean soloStockBajo) {
        // Este método es usado por el POS, así que no filtramos por tipo (trae todo)
        return buscar(filtro, idCategoria, soloStockBajo, null);
    }

    @Override
    public List<Producto> buscar(String filtro, Integer idCategoria, Boolean soloStockBajo, String tipoProducto) {
        StringBuilder sql = new StringBuilder(
                "SELECT id_producto, sku, nombre, id_categoria, nombre_categoria, " +
                        "       precio_compra, precio_venta, stock_actual, stock_minimo, activo, " +
                        "       id_proveedor, tipo_producto " +
                        "  FROM producto WHERE activo = 1 ");

        List<Object> params = new ArrayList<>();

        if (filtro != null && !filtro.isEmpty()) {
            sql.append(" AND (LOWER(nombre) LIKE ? OR sku LIKE ?) ");
            String f = "%" + filtro.toLowerCase() + "%";
            params.add(f);
            params.add("%" + filtro + "%");
        }
        if (idCategoria != null) {
            sql.append(" AND id_categoria = ? ");
            params.add(idCategoria);
        }
        if (soloStockBajo != null && soloStockBajo) {
            sql.append(" AND stock_actual <= stock_minimo ");
        }
        // Filtro por tipo de producto (Papelería / Skincare)
        if (tipoProducto != null && !tipoProducto.isEmpty()) {
            sql.append(" AND tipo_producto = ? ");
            params.add(tipoProducto);
        }

        sql.append(" ORDER BY nombre");

        List<Producto> lista = new ArrayList<>();
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error listando productos: " + e.getMessage(), e);
        }
        return lista;
    }

    /* ============================================================
       Contadores y alertas
       ============================================================ */
    @Override
    public int contarStockBajo() {
        String sql = "SELECT COUNT(*) FROM producto WHERE activo = 1 AND stock_actual <= stock_minimo";
        try (Statement st = DatabaseConnection.get().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error en contarStockBajo: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Producto> obtenerStockBajo(int limite) {
        return obtenerAlertasStock(limite);
    }

    @Override
    public List<Producto> obtenerAlertasStock(int limite) {
        String sql =
                "SELECT id_producto, sku, nombre, id_categoria, nombre_categoria, " +
                        "       precio_compra, precio_venta, stock_actual, stock_minimo, activo, " +
                        "       id_proveedor, tipo_producto " +
                        "  FROM producto " +
                        " WHERE activo = 1 AND stock_actual <= stock_minimo " +
                        " ORDER BY (stock_actual * 1.0 / NULLIF(stock_minimo, 0)) ASC " +
                        " LIMIT ?";

        List<Producto> lista = new ArrayList<>();
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setInt(1, limite);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error en obtenerAlertasStock: " + e.getMessage(), e);
        }
        return lista;
    }

    /* ============================================================
       Insertar
       ============================================================ */
    @Override
    public int insertar(Producto p) {
        String sql =
                "INSERT INTO productos " +
                        "   (codigo_barras, nombre, id_categoria, id_proveedor, " +
                        "    precio_mayoreo, precio_menudeo, stock_actual, stock_minimo, tipo_producto) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = DatabaseConnection.get()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, p.getSku());
            ps.setString(2, p.getNombre());
            ps.setInt   (3, p.getIdCategoria());

            if (p.getIdProveedor() > 0) ps.setInt (4, p.getIdProveedor());
            else                        ps.setNull(4, Types.INTEGER);

            ps.setDouble(5, p.getPrecioCompra());
            ps.setDouble(6, p.getPrecioVenta());
            ps.setInt   (7, p.getStockActual());
            ps.setInt   (8, p.getStockMinimo());
            ps.setString(9, p.getTipoProducto() != null ? p.getTipoProducto() : "PAPELERIA");

            if (ps.executeUpdate() == 0) return -1;
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al insertar producto: " + e.getMessage(), e);
        }
    }

    /* ============================================================
       Actualizar
       ============================================================ */
    @Override
    public boolean actualizar(Producto p) {
        String sql =
                "UPDATE productos SET " +
                        " codigo_barras  = ?, " +
                        " nombre         = ?, " +
                        " id_categoria   = ?, " +
                        " id_proveedor   = ?, " +
                        " precio_mayoreo = ?, " +
                        " precio_menudeo = ?, " +
                        " stock_actual   = ?, " +
                        " stock_minimo   = ?, " +
                        " tipo_producto  = ?  " +
                        " WHERE id_producto = ?";

        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {

            ps.setString(1, p.getSku());
            ps.setString(2, p.getNombre());
            ps.setInt   (3, p.getIdCategoria());

            if (p.getIdProveedor() > 0) ps.setInt (4, p.getIdProveedor());
            else                        ps.setNull(4, Types.INTEGER);

            ps.setDouble(5, p.getPrecioCompra());
            ps.setDouble(6, p.getPrecioVenta());
            ps.setInt   (7, p.getStockActual());
            ps.setInt   (8, p.getStockMinimo());
            ps.setString(9, p.getTipoProducto() != null ? p.getTipoProducto() : "PAPELERIA");

            ps.setInt   (10, p.getIdProducto()); // 🔑 Índice 10 para el WHERE

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar producto: " + e.getMessage(), e);
        }
    }

    /* ============================================================
       Eliminar
       ============================================================ */
    @Override
    public boolean eliminar(int idProducto) {
        String sql = "DELETE FROM productos WHERE id_producto = ?";
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setInt(1, idProducto);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar producto: " + e.getMessage(), e);
        }
    }

    /* ============================================================
       Resolver id_categoria
       ============================================================ */
    @Override
    public Integer obtenerIdCategoriaPorNombre(String nombre) {
        String sql = "SELECT id_categoria FROM categorias WHERE nombre = ?";
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        } catch (SQLException e) {
            System.err.println("No se pudo resolver categoría: " + e.getMessage());
            return null;
        }
    }

    /* ============================================================
       Filtros por Proveedor
       ============================================================ */
    @Override
    public List<Producto> listarPorProveedor(int idProveedor, boolean soloStockBajo) {
        return buscarPorProveedor(idProveedor, soloStockBajo, null);
    }

    @Override
    public List<Producto> buscarPorProveedor(int idProveedor, Boolean soloStockBajo) {
        return buscarPorProveedor(idProveedor, soloStockBajo, null);
    }

    @Override
    public List<Producto> buscarPorProveedor(int idProveedor, Boolean soloStockBajo, String tipoProducto) {
        StringBuilder sql = new StringBuilder(
                "SELECT id_producto, sku, nombre, id_categoria, nombre_categoria, " +
                        "       precio_compra, precio_venta, stock_actual, stock_minimo, activo, " +
                        "       id_proveedor, tipo_producto " +
                        "  FROM producto WHERE activo = 1 AND id_proveedor = ? ");

        if (soloStockBajo != null && soloStockBajo) {
            sql.append(" AND stock_actual <= stock_minimo ");
        }
        if (tipoProducto != null && !tipoProducto.isEmpty()) {
            sql.append(" AND tipo_producto = ? ");
        }
        sql.append(" ORDER BY nombre");

        List<Producto> lista = new ArrayList<>();
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql.toString())) {
            ps.setInt(1, idProveedor);
            if (tipoProducto != null && !tipoProducto.isEmpty()) {
                ps.setString(2, tipoProducto);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error filtrando por proveedor: " + e.getMessage(), e);
        }
        return lista;
    }

    /* ============================================================
       Mapeo común (Limpio)
       ============================================================ */
    private Producto mapear(ResultSet rs) throws SQLException {
        Producto p = new Producto();
        p.setIdProducto(rs.getInt("id_producto"));
        p.setSku(rs.getString("sku"));
        p.setNombre(rs.getString("nombre"));
        p.setIdCategoria(rs.getInt("id_categoria"));
        p.setNombreCategoria(rs.getString("nombre_categoria"));
        p.setPrecioCompra(rs.getDouble("precio_compra"));
        p.setPrecioVenta(rs.getDouble("precio_venta"));
        p.setStockActual(rs.getInt("stock_actual"));
        p.setStockMinimo(rs.getInt("stock_minimo"));
        p.setIdProveedor(rs.getInt("id_proveedor"));
        p.setActivo(rs.getInt("activo") == 1);

        try {
            p.setTipoProducto(rs.getString("tipo_producto"));
        } catch (SQLException e) {
            p.setTipoProducto("PAPELERIA"); // Fallback de seguridad
        }

        return p;
    }
}