package org.papeleria_pos.dao.Interface;

import org.papeleria_pos.dto.TopProducto;
import org.papeleria_pos.dto.VentaDia;
import org.papeleria_pos.dto.VentaRequest;
import org.papeleria_pos.dto.VentaReciente;
import org.papeleria_pos.models.Venta;

import java.time.LocalDate;
import java.util.List;

public interface IVentaDAO {

    /* ===== Dashboard / Reportes (ya existían) ===== */
    /* Dashboard / Reportes */
    List<Venta> obtenerPorFecha(LocalDate fecha);
    List<Venta> obtenerPorRango(LocalDate desde, LocalDate hasta);
    List<Venta> obtenerUltimas(int limite);
    List<TopProducto> obtenerTopProductos(LocalDate desde, LocalDate hasta, int limite);
    List<VentaDia> obtenerVentasPorDia(LocalDate hasta, int dias);
    List<VentaReciente> obtenerUltimasRecientes(int limite);

    /* POS */
    String registrarVentaCompleta(VentaRequest request);
    boolean hayStock(int idProducto, int cantidad);
}