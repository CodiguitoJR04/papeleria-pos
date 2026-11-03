package org.papeleria_pos.dao;

import org.papeleria_pos.config.DatabaseConnection;
import org.papeleria_pos.dao.Interface.IMovimientoDAO;
import org.papeleria_pos.dto.MovimientoResumen;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MovimientoDAOImpl implements IMovimientoDAO {

    @Override
    public List<MovimientoResumen> listarPorRango(LocalDate desde, LocalDate hasta) {
        // 🔑 Corregido el JOIN a la tabla 'usuarios' y columna 'id_usuario'
        String sql = "SELECT m.id_movimiento, m.fecha, p.nombre AS producto, " +
                "       m.tipo, m.cantidad, m.motivo, m.referencia, u.nombre AS usuario " +
                "  FROM movimientos_inventario m " +
                "  LEFT JOIN productos p ON p.id_producto = m.id_producto " +
                "  LEFT JOIN usuarios  u ON u.id_usuario  = m.id_administrador " +
                " WHERE DATE(m.fecha) BETWEEN ? AND ? " +
                " ORDER BY m.fecha DESC";

        List<MovimientoResumen> lista = new ArrayList<>();
        try (Connection conn = DatabaseConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, desde.toString());
            ps.setString(2, hasta.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String ref = rs.getString("referencia");
                    lista.add(new MovimientoResumen(
                            rs.getInt("id_movimiento"),
                            rs.getTimestamp("fecha") != null ? rs.getTimestamp("fecha").toLocalDateTime() : null,
                            rs.getString("producto"),
                            rs.getString("tipo"),
                            rs.getInt("cantidad"),
                            rs.getString("motivo"),
                            rs.getString("usuario"),
                            ref != null && !ref.isBlank() ? ref : "—"
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error listando movimientos: " + e.getMessage(), e);
        }
        return lista;
    }

    @Override
    public int registrar(int idProducto, int idUsuario, String tipo,
                         int cantidad, String motivo, String referencia) {
        String sql = "INSERT INTO movimientos_inventario " +
                "(id_producto, id_administrador, tipo, cantidad, fecha, motivo, referencia) " +
                "VALUES (?, ?, ?, ?, NOW(), ?, ?)";
        try (Connection conn = DatabaseConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, idProducto);
            ps.setInt(2, idUsuario);
            ps.setString(3, tipo);
            ps.setInt(4, cantidad);
            ps.setString(5, motivo);
            ps.setString(6, referencia);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error registrando movimiento: " + e.getMessage(), e);
        }
    }
}