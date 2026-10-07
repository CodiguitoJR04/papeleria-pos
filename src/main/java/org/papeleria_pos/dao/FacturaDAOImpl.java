package org.papeleria_pos.dao;

import org.papeleria_pos.config.DatabaseConnection;
import org.papeleria_pos.dao.Interface.IFacturaDAO;
import org.papeleria_pos.dto.FacturaRequest;
import org.papeleria_pos.dto.FacturaResumen;
import org.papeleria_pos.models.Factura;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class FacturaDAOImpl implements IFacturaDAO {

    /* ============================================================
       Generar factura (transacción)
       ============================================================ */
    @Override
    public Factura generar(FacturaRequest req) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.get();
            conn.setAutoCommit(false);

            // 1) Datos fiscales de la venta
            double subtotal = 0, iva = 0, total = 0;
            String sqlV = "SELECT subtotal, iva, total FROM ventas WHERE id_venta = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlV)) {
                ps.setInt(1, req.getIdVenta());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        subtotal = rs.getDouble("subtotal");
                        iva      = rs.getDouble("iva");
                        total    = rs.getDouble("total");
                    } else {
                        throw new RuntimeException("Venta no encontrada: " + req.getIdVenta());
                    }
                }
            }

            // 2) Cliente (crear/reutilizar por RFC)
            int idCliente = obtenerOCrearCliente(conn,
                    req.getRfcReceptor(), req.getRazonSocial());

            // 3) Siguiente folio para la serie "A"
            int folio = siguienteFolio(conn, "A");

            // 4) Insertar factura
            String sql = "INSERT INTO facturas " +
                    "(id_venta, id_cliente, serie, folio, rfc_receptor, razon_social, " +
                    " regimen_fiscal, uso_cfdi, subtotal, iva, total, metodo_pago, " +
                    " fecha_emision, estado) " +
                    "VALUES (?, ?, 'A', ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), 'GENERADA')";

            try (PreparedStatement ps = conn.prepareStatement(sql,
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, req.getIdVenta());
                ps.setInt(2, idCliente);
                ps.setInt(3, folio);
                ps.setString(4, req.getRfcReceptor());
                ps.setString(5, req.getRazonSocial());
                ps.setString(6, req.getRegimenFiscal());
                ps.setString(7, req.getUsoCfdi());
                ps.setDouble(8, subtotal);
                ps.setDouble(9, iva);
                ps.setDouble(10, total);
                ps.setString(11, req.getMetodoPago());
                ps.executeUpdate();

                int idFactura;
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    idFactura = rs.next() ? rs.getInt(1) : -1;
                }

                conn.commit();

                Factura f = new Factura();
                f.setIdFactura(idFactura);
                f.setIdVenta(req.getIdVenta());
                f.setIdCliente(idCliente);
                f.setSerie("A");
                f.setFolio(folio);
                f.setRfcReceptor(req.getRfcReceptor());
                f.setRazonSocial(req.getRazonSocial());
                f.setRegimenFiscal(req.getRegimenFiscal());
                f.setUsoCfdi(req.getUsoCfdi());
                f.setSubtotal(subtotal);
                f.setIva(iva);
                f.setTotal(total);
                f.setMetodoPago(req.getMetodoPago());
                f.setEstado("GENERADA");
                return f;
            }

        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ignored) {}
            throw new RuntimeException("Error generando factura: " + e.getMessage(), e);
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }

    /* ============================================================
       Obtener / crear cliente por RFC
       ============================================================ */
    private int obtenerOCrearCliente(Connection conn, String rfc, String razon) throws SQLException {
        String sqlSelect = "SELECT id_cliente FROM clientes WHERE rfc = ?";
        try (PreparedStatement ps = conn.prepareStatement(sqlSelect)) {
            ps.setString(1, rfc);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        String sqlInsert = "INSERT INTO clientes (nombre, rfc, es_mayorista) VALUES (?, ?, FALSE)";
        try (PreparedStatement ps = conn.prepareStatement(sqlInsert,
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, razon);
            ps.setString(2, rfc);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    /* ============================================================
       Siguiente folio por serie
       ============================================================ */
    private int siguienteFolio(Connection conn, String serie) throws SQLException {
        String sql = "SELECT COALESCE(MAX(folio), 0) + 1 FROM facturas WHERE serie = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, serie);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 1;
            }
        }
    }

    /* ============================================================
       Consultas
       ============================================================ */
    @Override
    public Factura obtenerPorVenta(int idVenta) {
        String sql = "SELECT * FROM facturas WHERE id_venta = ? LIMIT 1";
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setInt(1, idVenta);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Factura obtenerPorId(int idFactura) {
        String sql = "SELECT * FROM facturas WHERE id_factura = ?";
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setInt(1, idFactura);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<FacturaResumen> listarPorRango(LocalDate desde, LocalDate hasta) {
        String sql =
                "SELECT f.id_factura, f.serie, f.folio, f.fecha_emision, " +
                        "       f.rfc_receptor, f.razon_social, f.total, f.estado, " +
                        "       v.folio AS folio_venta " +
                        "  FROM facturas f " +
                        "  JOIN ventas v ON v.id_venta = f.id_venta " +
                        " WHERE DATE(f.fecha_emision) BETWEEN ? AND ? " +
                        " ORDER BY f.fecha_emision DESC";

        List<FacturaResumen> lista = new ArrayList<>();
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setString(1, desde.toString());
            ps.setString(2, hasta.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("fecha_emision");
                    lista.add(new FacturaResumen(
                            rs.getInt("id_factura"),
                            rs.getString("serie"),
                            rs.getInt("folio"),
                            ts != null ? ts.toLocalDateTime().toLocalDate() : null,
                            rs.getString("rfc_receptor"),
                            rs.getString("razon_social"),
                            rs.getDouble("total"),
                            rs.getString("estado"),
                            rs.getInt("folio_venta")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return lista;
    }

    @Override
    public boolean cancelar(int idFactura) {
        String sql = "UPDATE facturas SET estado = 'CANCELADA' WHERE id_factura = ?";
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setInt(1, idFactura);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int contarPorRango(LocalDate desde, LocalDate hasta) {
        String sql = "SELECT COUNT(*) FROM facturas " +
                "WHERE DATE(fecha_emision) BETWEEN ? AND ?";
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setString(1, desde.toString());
            ps.setString(2, hasta.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /* ============================================================
       Mapeo
       ============================================================ */
    private Factura mapear(ResultSet rs) throws SQLException {
        Factura f = new Factura();
        f.setIdFactura(rs.getInt("id_factura"));
        f.setIdVenta(rs.getInt("id_venta"));
        f.setIdCliente(rs.getInt("id_cliente"));
        f.setSerie(rs.getString("serie"));
        f.setFolio(rs.getInt("folio"));
        f.setUuid(rs.getString("folio_fiscal_uuid"));
        f.setRfcReceptor(rs.getString("rfc_receptor"));
        f.setRazonSocial(rs.getString("razon_social"));
        f.setRegimenFiscal(rs.getString("regimen_fiscal"));
        f.setUsoCfdi(rs.getString("uso_cfdi"));
        f.setSubtotal(rs.getDouble("subtotal"));
        f.setIva(rs.getDouble("iva"));
        f.setTotal(rs.getDouble("total"));
        f.setMetodoPago(rs.getString("metodo_pago"));
        f.setEstado(rs.getString("estado"));
        f.setXml(rs.getString("xml"));
        Timestamp ts = rs.getTimestamp("fecha_emision");
        if (ts != null) f.setFechaEmision(ts.toLocalDateTime());
        return f;
    }
}