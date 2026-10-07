package org.papeleria_pos.dao;

import org.papeleria_pos.config.DatabaseConnection;
import org.papeleria_pos.dao.Interface.ITurnoDAO;
import org.papeleria_pos.dto.TurnoAbierto;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TurnoDAOImpl implements ITurnoDAO {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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

    @Override
    public TurnoAbierto obtenerTurnoAbierto(int idCajero) {
        String sql =
                "SELECT t.id_turno, t.id_cajero, u.nombre AS cajero, " +
                        "       t.fecha_inicio, t.monto_inicial " +
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
        String sql = "INSERT INTO turnos " +
                "(id_cajero, fecha_inicio, monto_inicial, total_ventas, estado) " +
                "VALUES (?, NOW(), ?, 0, 'ABIERTO')";
        try (PreparedStatement ps = DatabaseConnection.get()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
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
    public boolean cerrarTurno(int idTurno, double montoFinal) {
        String sql = "UPDATE turnos SET fecha_fin = NOW(), monto_final_efectivo = ?, " +
                "estado = 'CERRADO' WHERE id_turno = ?";
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setDouble(1, montoFinal);
            ps.setInt(2, idTurno);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error cerrando turno: " + e.getMessage(), e);
        }
    }
}