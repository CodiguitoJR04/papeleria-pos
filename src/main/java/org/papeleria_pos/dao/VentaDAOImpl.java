package org.papeleria_pos.dao;

import org.papeleria_pos.config.DatabaseConnection;
import org.papeleria_pos.dao.Interface.IVentaDAO;
import org.papeleria_pos.dto.*;
import org.papeleria_pos.models.Venta;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class VentaDAOImpl implements IVentaDAO {

    /* ============================================================
       DASHBOARD / REPORTES
       ============================================================ */
    @Override
    public List<Venta> obtenerPorFecha(LocalDate fecha) {
        String sql = "SELECT * FROM venta WHERE DATE(fecha) = ? ORDER BY fecha DESC";
        return ejecutarVentas(sql, fecha.toString());
    }

    @Override
    public List<Venta> obtenerPorRango(LocalDate desde, LocalDate hasta) {
        String sql = "SELECT * FROM venta WHERE DATE(fecha) BETWEEN ? AND ? ORDER BY fecha";
        List<Venta> lista = new ArrayList<>();
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setString(1, desde.toString());
            ps.setString(2, hasta.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error en obtenerPorRango: " + e.getMessage(), e);
        }
        return lista;
    }

    @Override
    public List<Venta> obtenerUltimas(int limite) {
        String sql = "SELECT * FROM venta ORDER BY fecha DESC LIMIT ?";
        List<Venta> lista = new ArrayList<>();
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setInt(1, limite);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error en obtenerUltimas: " + e.getMessage(), e);
        }
        return lista;
    }

    @Override
    public List<VentaReciente> obtenerUltimasRecientes(int limite) {
        String sql = "SELECT folio, usuario, fecha, total, estado FROM venta " +
                "ORDER BY fecha DESC LIMIT ?";
        List<VentaReciente> lista = new ArrayList<>();
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setInt(1, limite);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new VentaReciente(
                            rs.getInt("folio"),
                            rs.getString("usuario"),
                            rs.getTimestamp("fecha").toLocalDateTime(),
                            rs.getDouble("total"),
                            rs.getString("estado")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error en obtenerUltimasRecientes: " + e.getMessage(), e);
        }
        return lista;
    }

    @Override
    public List<TopProducto> obtenerTopProductos(LocalDate desde, LocalDate hasta, int limite) {
        String sql =
                "SELECT p.id_producto, p.nombre, " +
                        "       SUM(dv.cantidad) AS unidades, SUM(dv.importe) AS importe " +
                        "  FROM detalle_venta dv " +
                        "  JOIN venta v ON v.id_venta = dv.id_venta " +
                        "  JOIN producto p ON p.id_producto = dv.id_producto " +
                        " WHERE v.estado = 'COMPLETADA' " +
                        "   AND DATE(v.fecha) BETWEEN DATE(?) AND DATE(?) " +
                        " GROUP BY p.id_producto, p.nombre " +
                        " ORDER BY unidades DESC LIMIT ?";

        List<TopProducto> lista = new ArrayList<>();
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setString(1, desde.toString());
            ps.setString(2, hasta.toString());
            ps.setInt(3, limite);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new TopProducto(
                            rs.getInt("id_producto"),
                            rs.getString("nombre"),
                            rs.getInt("unidades"),
                            rs.getDouble("importe")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error en obtenerTopProductos: " + e.getMessage(), e);
        }
        return lista;
    }

    @Override
    public List<VentaDia> obtenerVentasPorDia(LocalDate hasta, int dias) {
        LocalDate desde = hasta.minusDays(dias - 1L);
        String sql =
                "SELECT DATE(fecha) AS d, COALESCE(SUM(total), 0) AS total " +
                        "  FROM venta WHERE estado = 'COMPLETADA' " +
                        "   AND DATE(fecha) BETWEEN DATE(?) AND DATE(?) GROUP BY DATE(fecha)";

        Map<LocalDate, Double> mapa = new HashMap<>();
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setString(1, desde.toString());
            ps.setString(2, hasta.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    mapa.put(LocalDate.parse(rs.getString("d")), rs.getDouble("total"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error en obtenerVentasPorDia: " + e.getMessage(), e);
        }

        List<VentaDia> resultado = new ArrayList<>();
        for (int i = 0; i < dias; i++) {
            LocalDate d = desde.plusDays(i);
            resultado.add(new VentaDia(d, mapa.getOrDefault(d, 0.0)));
        }
        return resultado;
    }

    /* ============================================================
       POS — REGISTRAR VENTA COMPLETA (transacción)
       ============================================================ */

    @Override
    public String registrarVentaCompleta(VentaRequest req) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.get();
            conn.setAutoCommit(false);

            double subtotal = 0;
            for (ItemCarrito it : req.getItems()) subtotal += it.getImporte();
            double iva = subtotal * 0.16;
            double total = subtotal + iva;

            String folio = generarFolio(conn);
            int idVenta = insertarVenta(conn, folio, subtotal, iva, total, req);

            for (ItemCarrito it : req.getItems()) {
                insertarDetalle(conn, idVenta, it);
                descontarStock(conn, it.getProducto().getIdProducto(), it.getCantidad());
                insertarMovimiento(conn, it.getProducto().getIdProducto(),
                        it.getCantidad(), req.getIdCajero(), folio);
            }
            insertarPago(conn, idVenta, req.getMetodoPago(), total, req.getMontoRecibido());

            conn.commit();
            return folio;
        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ignored) {}
            throw new RuntimeException("Error registrando venta: " + e.getMessage(), e);
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }

    @Override
    public boolean hayStock(int idProducto, int cantidad) {
        String sql = "SELECT stock_actual FROM productos WHERE id_producto = ?";
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setInt(1, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) >= cantidad;
            }
        } catch (SQLException e) {
            return false;
        }
    }

    /* ============================================================
       Helpers internos
       ============================================================ */
    private List<Venta> ejecutarVentas(String sql, String param) {
        List<Venta> lista = new ArrayList<>();
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando ventas: " + e.getMessage(), e);
        }
        return lista;
    }

    private Venta mapear(ResultSet rs) throws SQLException {
        Venta v = new Venta();
        v.setIdVenta(rs.getInt("id_venta"));
        v.setFolio(rs.getInt("folio"));
        Timestamp ts = rs.getTimestamp("fecha");
        if (ts != null) v.setFecha(ts.toLocalDateTime());
        v.setIdUsuario(rs.getInt("id_usuario"));
        v.setUsuario(rs.getString("usuario"));
        v.setMetodoPago(rs.getString("metodo_pago"));
        v.setSubtotal(rs.getDouble("subtotal"));
        v.setIva(rs.getDouble("iva"));
        v.setTotal(rs.getDouble("total"));
        v.setEstado(rs.getString("estado"));
        return v;
    }

    private String generarFolio(Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) + 1 FROM ventas WHERE YEAR(fecha_hora) = YEAR(CURDATE())";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            int num = rs.next() ? rs.getInt(1) : 1;
            return String.format("F-%d-%05d", LocalDate.now().getYear(), num);
        }
    }

    private int insertarVenta(Connection conn, String folio, double sub,
                              double iva, double total, VentaRequest req) throws SQLException {
        String sql = "INSERT INTO ventas " +
                "(folio, fecha_hora, subtotal, iva, total, estado, id_cajero, id_turno) " +
                "VALUES (?, NOW(), ?, ?, ?, 'COMPLETADA', ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, folio);
            ps.setDouble(2, sub);
            ps.setDouble(3, iva);
            ps.setDouble(4, total);
            ps.setInt(5, req.getIdCajero());
            ps.setInt(6, req.getIdTurno());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    private void insertarDetalle(Connection conn, int idVenta, ItemCarrito it)
            throws SQLException {
        String sql = "INSERT INTO detalle_ventas " +
                "(id_venta, id_producto, cantidad, precio_unitario_snapshot, subtotal) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idVenta);
            ps.setInt(2, it.getProducto().getIdProducto());
            ps.setInt(3, it.getCantidad());
            ps.setDouble(4, it.getProducto().getPrecioVenta());
            ps.setDouble(5, it.getImporte());
            ps.executeUpdate();
        }
    }

    private void descontarStock(Connection conn, int idProducto, int cantidad)
            throws SQLException {
        String sql = "UPDATE productos SET stock_actual = stock_actual - ? WHERE id_producto = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, cantidad);
            ps.setInt(2, idProducto);
            ps.executeUpdate();
        }
    }

    private void insertarMovimiento(Connection conn, int idProducto, int cantidad,
                                    int idCajero, String folio) throws SQLException {
        String sql = "INSERT INTO movimientos_inventario " +
                "(id_producto, id_administrador, tipo, cantidad, fecha, motivo) " +
                "VALUES (?, ?, 'SALIDA_VENTA', ?, NOW(), ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idProducto);
            ps.setInt(2, idCajero);
            ps.setInt(3, cantidad);
            ps.setString(4, "Venta " + folio);
            ps.executeUpdate();
        }
    }

    private void insertarPago(Connection conn, int idVenta, String metodo,
                              double total, double recibido) throws SQLException {
        String sql = "INSERT INTO pagos_venta " +
                "(id_venta, tipo_pago, monto, monto_recibido, cambio, fecha_pago) " +
                "VALUES (?, ?, ?, ?, ?, NOW())";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idVenta);
            ps.setString(2, metodo);
            ps.setDouble(3, total);
            ps.setDouble(4, recibido);
            ps.setDouble(5, Math.max(0, recibido - total));
            ps.executeUpdate();
        }
    }
}