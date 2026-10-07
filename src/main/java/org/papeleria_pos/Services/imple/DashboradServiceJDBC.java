package org.papeleria_pos.Services.imple;

import org.papeleria_pos.Services.DashboardService;
import org.papeleria_pos.config.DatabaseConnection;
import org.papeleria_pos.models.DashboardResumen;
// Importa tus modelos DTOs correspondientes
import org.papeleria_pos.dto.VentaDia;
import org.papeleria_pos.dto.TopProducto;
import org.papeleria_pos.dto.VentaReciente;
import org.papeleria_pos.dto.AlertaStock;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class DashboradServiceJDBC implements DashboardService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /* ============================================================
       RESUMEN (KPIs)
       ============================================================ */
    @Override
    public CompletableFuture<DashboardResumen> obtenerResumen(LocalDate dia) {
        return CompletableFuture.supplyAsync(() -> {
            double ventasHoy   = ventasTotales(dia);
            int    ticketsHoy  = ticketsDe(dia);
            double ventasAyer  = ventasTotales(dia.minusDays(1));
            int    ticketsAyer = ticketsDe(dia.minusDays(1));

            int stockBajo    = contarStockBajo();
            int turnosAbiert = contarTurnosAbiertos();

            double ticketHoy  = ticketsHoy  > 0 ? ventasHoy  / ticketsHoy  : 0;
            double ticketAyer = ticketsAyer > 0 ? ventasAyer / ticketsAyer : 0;

            return new DashboardResumen(ventasHoy, ticketHoy, stockBajo, turnosAbiert,
                    ventasAyer, ticketAyer);
        });
    }

    private double ventasTotales(LocalDate dia) {
        String sql = "SELECT COALESCE(SUM(total), 0) FROM venta "
                + "WHERE estado = 'COMPLETADA' AND DATE(fecha_hora) = ?";

        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(dia));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando ventas de " + dia, e);
        }
    }

    private int ticketsDe(LocalDate dia) {
        String sql = "SELECT COUNT(*) FROM venta "
                + "WHERE estado = 'COMPLETADA' AND DATE(fecha_hora) = ?";

        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(dia));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando tickets de " + dia, e);
        }
    }

    private int contarStockBajo() {
        String sql = "SELECT COUNT(*) FROM producto WHERE stock_actual <= stock_minimo";
        try (Statement st = DatabaseConnection.get().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando stock bajo", e);
        }
    }

    private int contarTurnosAbiertos() {
        // En la BD, un turno abierto no tiene fecha_fin registrada
        String sql = "SELECT COUNT(*) FROM turno WHERE fecha_fin IS NULL";
        try (Statement st = DatabaseConnection.get().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando turnos abiertos", e);
        }
    }

    /* ============================================================
       VENTAS POR DÍA (gráfica)
       ============================================================ */
    @Override
    public CompletableFuture<List<VentaDia>> obtenerVentasPorDia(LocalDate hasta, int dias) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT DATE(fecha_hora) AS d, COALESCE(SUM(total), 0) "
                    + "  FROM venta "
                    + " WHERE estado = 'COMPLETADA' "
                    + "   AND DATE(fecha_hora) BETWEEN ? AND ? "
                    + " GROUP BY DATE(fecha_hora) "
                    + " ORDER BY d";

            LocalDate desde = hasta.minusDays(dias - 1L);
            Map<LocalDate, Double> mapa = new HashMap<>();

            try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
                ps.setDate(1, Date.valueOf(desde));
                ps.setDate(2, Date.valueOf(hasta));

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        mapa.put(rs.getDate("d").toLocalDate(), rs.getDouble(2));
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("Error al obtener ventas por día", e);
            }

            // Rellenar días sin venta con 0.0
            List<VentaDia> resultado = new ArrayList<>();
            for (int i = 0; i < dias; i++) {
                LocalDate d = desde.plusDays(i);
                resultado.add(new VentaDia(d, mapa.getOrDefault(d, 0.0)));
            }
            return resultado;
        });
    }

    /* ============================================================
       TOP PRODUCTOS
       ============================================================ */
    @Override
    public CompletableFuture<List<TopProducto>> obtenerTopProductos(LocalDate desde, LocalDate hasta, int limite) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT p.id_producto, p.nombre, "
                    + "       SUM(dv.cantidad) AS unidades, "
                    + "       SUM(dv.subtotal) AS importe "
                    + "  FROM detalle_venta dv "
                    + "  JOIN venta v    ON v.id_venta = dv.id_venta "
                    + "  JOIN producto p ON p.id_producto = dv.id_producto "
                    + " WHERE v.estado = 'COMPLETADA' "
                    + "   AND DATE(v.fecha_hora) BETWEEN ? AND ? "
                    + " GROUP BY p.id_producto, p.nombre "
                    + " ORDER BY unidades DESC "
                    + " LIMIT ?";

            List<TopProducto> lista = new ArrayList<>();
            try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
                ps.setDate(1, Date.valueOf(desde));
                ps.setDate(2, Date.valueOf(hasta));
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
                throw new RuntimeException("Error en top productos", e);
            }
            return lista;
        });
    }

    /* ============================================================
       ÚLTIMAS VENTAS
       ============================================================ */
    @Override
    public CompletableFuture<List<VentaReciente>> obtenerUltimasVentas(int limite) {
        return CompletableFuture.supplyAsync(() -> {
            // Se hace JOIN con usuario para mostrar el nombre/username del cajero
            String sql = "SELECT v.id_venta AS folio, u.username AS usuario, v.fecha_hora, v.total, v.estado "
                    + "  FROM venta v "
                    + "  JOIN cajero c ON v.id_cajero = c.id_usuario "
                    + "  JOIN usuario u ON c.id_usuario = u.id_usuario "
                    + " ORDER BY v.fecha_hora DESC "
                    + " LIMIT ?";

            List<VentaReciente> lista = new ArrayList<>();
            try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
                ps.setInt(1, limite);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        LocalDateTime fechaHora = rs.getTimestamp("fecha_hora").toLocalDateTime();
                        lista.add(new VentaReciente(
                                rs.getInt("folio"),
                                rs.getString("usuario"),
                                fechaHora,
                                rs.getDouble("total"),
                                rs.getString("estado")));
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("Error en últimas ventas", e);
            }
            return lista;
        });
    }

    /* ============================================================
       ALERTAS DE STOCK
       ============================================================ */
    @Override
    public CompletableFuture<List<AlertaStock>> obtenerAlertasStock(int limite) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT id_producto, nombre, stock_actual, stock_minimo "
                    + "  FROM producto "
                    + " WHERE stock_actual <= stock_minimo "
                    + " ORDER BY (stock_actual * 1.0 / NULLIF(stock_minimo, 0)) ASC "
                    + " LIMIT ?";

            List<AlertaStock> lista = new ArrayList<>();
            try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
                ps.setInt(1, limite);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        lista.add(new AlertaStock(
                                rs.getInt("id_producto"),
                                rs.getString("nombre"),
                                rs.getInt("stock_actual"),
                                rs.getInt("stock_minimo")));
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("Error en alertas de stock", e);
            }
            return lista;
        });
    }
}