package org.papeleria_pos.dao;

import org.papeleria_pos.config.DatabaseConnection;
import org.papeleria_pos.dao.Interface.IMovimientoDAO;
import org.papeleria_pos.dto.MovimientoResumen;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MovimientoDAOImpl implements IMovimientoDAO {


    /* Regex para extraer referencia del motivo */
    private static final Pattern PAT_LOTE =
            Pattern.compile("(L-\\d{4}-\\d{2}-\\d{3})", Pattern.CASE_INSENSITIVE);
    private static final Pattern PAT_VENTA =
            Pattern.compile("Venta\\s+(?:F-\\d{4}-)?(\\d+)", Pattern.CASE_INSENSITIVE);

    @Override
    public List<MovimientoResumen> listarPorRango(LocalDate desde, LocalDate hasta) {

        String sql =
                "SELECT m.id_movimiento, m.fecha, p.nombre AS producto, " +
                        "       m.tipo, m.cantidad, m.motivo, m.referencia, u.nombre AS usuario " +
                        "  FROM movimientos_inventario m " +
                        "  LEFT JOIN productos p ON p.id_producto    = m.id_producto " +
                        "  LEFT JOIN usuario  u ON u.id_user          = m.id_administrador " +
                        " WHERE DATE(m.fecha) BETWEEN ? AND ? " +
                        " ORDER BY m.fecha DESC";

        List<MovimientoResumen> lista = new ArrayList<>();
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setString(1, desde.toString());
            ps.setString(2, hasta.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String ref = rs.getString("referencia");

                    lista.add(new MovimientoResumen(
                            rs.getInt("id_movimiento"),
                            rs.getTimestamp("fecha") != null
                                    ? rs.getTimestamp("fecha").toLocalDateTime() : null,
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
            throw new RuntimeException(
                    "Error listando movimientos: " + e.getMessage(), e);
        }
        return lista;
    }

    /* ------------------------------------------------------------
       Extrae la referencia del motivo según el tipo de movimiento
       ------------------------------------------------------------ */
/* ------------------------------------------------------------
   Extrae la referencia del motivo según el tipo de movimiento
   ------------------------------------------------------------ */
    private String extraerReferencia(String tipo, String motivo) {
        if (motivo == null || motivo.isBlank()) return "—";

        // 1) SALIDA_VENTA: "Venta F-2026-00104" o "Venta #1042"
        if ("SALIDA_VENTA".equals(tipo)) {
            Matcher m = PAT_VENTA.matcher(motivo);
            return m.find() ? m.group(1) : "—";
        }

        // 2) ENTRADA_COMPRA: "Recepción lote L-2026-09-001"
        if ("ENTRADA_COMPRA".equals(tipo)) {
            Matcher m = PAT_LOTE.matcher(motivo);
            return m.find() ? m.group(1) : "—";
        }

        // 3) AJUSTE_INVENTARIO: "AJ-XXX"
        if ("AJUSTE_INVENTARIO".equals(tipo)) {
            Matcher m = Pattern.compile("(AJ-\\d+)", Pattern.CASE_INSENSITIVE).matcher(motivo);
            return m.find() ? m.group(1) : "—";
        }

        // 4) MERMA: no lleva referencia
        return "—";
    }
    @Override
    public int registrar(int idProducto, int idUsuario, String tipo,
                         int cantidad, String motivo, String referencia) {
        String sql = "INSERT INTO movimientos_inventario " +
                "(id_producto, id_administrador, tipo, cantidad, fecha, motivo, referencia) " +
                "VALUES (?, ?, ?, ?, NOW(), ?, ?)";
        try (PreparedStatement ps = DatabaseConnection.get()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, idProducto);
            ps.setInt(2, idUsuario);
            ps.setString(3, tipo);            // ENTRADA_COMPRA | MERMA | AJUSTE_INVENTARIO
            ps.setInt(4, cantidad);
            ps.setString(5, motivo);
            ps.setString(6, referencia);      // L-2026-09-001 | AJ-001 | null
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error registrando movimiento: " + e.getMessage(), e);
        }
    }
}