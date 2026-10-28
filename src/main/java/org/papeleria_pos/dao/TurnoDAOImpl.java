package org.papeleria_pos.dao;

import org.papeleria_pos.config.DatabaseConnection;
import org.papeleria_pos.dao.Interface.ITurnoDAO;
import org.papeleria_pos.dto.TurnoAbierto;
import org.papeleria_pos.dto.TurnoResumen;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TurnoDAOImpl implements ITurnoDAO {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public TurnoAbierto obtenerTurnoAbierto(int idCajero) {
        String sql = "SELECT t.id_turno, t.id_cajero, u.nombre AS cajero, t.fecha_inicio, t.monto_inicial " +
                "  FROM turnos t JOIN usuarios u ON u.id_usuario = t.id_cajero " +
                " WHERE t.id_cajero = ? AND t.estado = 'ABIERTO' " +
                " ORDER BY t.fecha_inicio DESC LIMIT 1";
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setInt(1, idCajero);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new TurnoAbierto(
                            rs.getInt("id_turno"),
                            rs.getInt("id_cajero"),
                            rs.getString("cajero"),
                            1,
                            LocalDateTime.parse(rs.getString("fecha_inicio"), FMT),
                            rs.getDouble("monto_inicial"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando turno: " + e.getMessage(), e);
        }
        return null;
    }

    @Override
    public int abrirTurno(int idCajero, int caja, double montoInicial) {
        String sql = "INSERT INTO turnos (id_cajero, fecha_inicio, monto_inicial, total_ventas, estado) " +
                "VALUES (?, NOW(), ?, 0, 'ABIERTO')";
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, idCajero);
            ps.setDouble(2, montoInicial);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error abriendo turno: " + e.getMessage(), e);
        }
    }

    @Override
    public double calcularEfectivoEsperado(int idTurno) {
        // Suma el monto inicial + todas las ventas pagadas en EFECTIVO
        String sql = "SELECT t.monto_inicial + COALESCE(SUM(p.monto), 0) " +
                "  FROM turnos t " +
                "  LEFT JOIN ventas v ON v.id_turno = t.id_turno AND v.estado = 'COMPLETADA' " +
                "  LEFT JOIN pagos_venta p ON p.id_venta = v.id_venta AND p.tipo_pago = 'EFECTIVO' " +
                " WHERE t.id_turno = ? GROUP BY t.id_turno";
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setInt(1, idTurno);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0;
            }
        } catch (SQLException e) {
            return 0;
        }
    }

    @Override
    public boolean cerrarTurno(int idTurno, double montoEsperado, double montoContado, double diferencia) {
        String sql = "UPDATE turnos SET fecha_fin = NOW(), monto_final_efectivo = ?, " +
                "monto_esperado = ?, diferencia = ?, estado = 'CERRADO' WHERE id_turno = ?";
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setDouble(1, montoContado);
            ps.setDouble(2, montoEsperado);
            ps.setDouble(3, diferencia);
            ps.setInt(4, idTurno);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error cerrando turno: " + e.getMessage(), e);
        }
    }

    @Override
    public List<TurnoResumen> listarPorRango(LocalDate desde, LocalDate hasta) {
        String sql = "SELECT t.id_turno, t.id_cajero, u.nombre AS cajero, " +
                "       t.fecha_inicio, t.fecha_fin, t.monto_inicial, t.monto_final_efectivo, " +
                "       t.total_ventas, t.monto_esperado, t.diferencia, t.estado, " +
                "       (SELECT COUNT(*) FROM ventas v WHERE v.id_turno = t.id_turno) AS num_ventas " +
                "  FROM turnos t LEFT JOIN usuarios u ON u.id_usuario = t.id_cajero " +
                " WHERE DATE(t.fecha_inicio) BETWEEN ? AND ? ORDER BY t.fecha_inicio DESC";
        List<TurnoResumen> lista = new ArrayList<>();
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setString(1, desde.toString());
            ps.setString(2, hasta.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp ini = rs.getTimestamp("fecha_inicio");
                    Timestamp fin = rs.getTimestamp("fecha_fin");
                    lista.add(new TurnoResumen(
                            rs.getInt("id_turno"),
                            1,
                            rs.getString("cajero"),
                            ini != null ? ini.toLocalDateTime() : null,
                            fin != null ? fin.toLocalDateTime() : null,
                            rs.getInt("num_ventas"),
                            rs.getDouble("monto_inicial"),
                            rs.getDouble("total_ventas"),
                            rs.getDouble("monto_esperado"),
                            rs.getDouble("monto_final_efectivo"),
                            rs.getDouble("diferencia"),
                            rs.getString("estado")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error listando turnos: " + e.getMessage(), e);
        }
        return lista;
    }
    @Override
    public int contarAbiertos() {
        String sql = "SELECT COUNT(*) FROM turnos WHERE estado = 'ABIERTO'";
        try (Statement st = DatabaseConnection.get().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error en contarAbiertos: " + e.getMessage(), e);
        }
    }
}