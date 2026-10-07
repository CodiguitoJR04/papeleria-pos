package org.papeleria_pos.Services;

import org.papeleria_pos.dto.AlertaStock;
import org.papeleria_pos.dto.TopProducto;
import org.papeleria_pos.dto.VentaDia;
import org.papeleria_pos.dto.VentaReciente;
import org.papeleria_pos.models.DashboardResumen;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface DashboardService {

    /* ============================================================
       RESUMEN (KPIs)
       ============================================================ */
    CompletableFuture<DashboardResumen> obtenerResumen(LocalDate dia);

    /** Devuelve los últimos {@code dias} días con ventas totales (rellena días sin venta con 0). */
    CompletableFuture<List<VentaDia>> obtenerVentasPorDia(LocalDate hasta, int dias);

    CompletableFuture<List<TopProducto>> obtenerTopProductos(LocalDate desde, LocalDate hasta, int limite);

    CompletableFuture<List<VentaReciente>> obtenerUltimasVentas(int limite);

    CompletableFuture<List<AlertaStock>> obtenerAlertasStock(int limite);
}