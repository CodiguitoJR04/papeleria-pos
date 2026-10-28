package org.papeleria_pos.dao.Interface;

import org.papeleria_pos.dto.TopProducto;
import org.papeleria_pos.dto.VentaDia;
import org.papeleria_pos.dto.VentaRequest;
import org.papeleria_pos.dto.VentaReciente;
import org.papeleria_pos.models.Venta;

import java.time.LocalDate;
import java.util.List;

public interface IVentaDAO {

    /* ===== Dashboard / Reportes ===== */
    List<Venta> obtenerPorFecha(LocalDate fecha);

    // Versión de 2 parámetros (para el Dashboard y otras vistas)
    List<Venta> obtenerPorRango(LocalDate desde, LocalDate hasta);

    // Versión de 3 parámetros (para los Reportes con filtro de Papelería/Skincare)
    List<Venta> obtenerPorRango(LocalDate desde, LocalDate hasta, String tipoProducto);

    List<Venta> obtenerUltimas(int limite);
    List<VentaReciente> obtenerUltimasRecientes(int limite);

    // Versión de 3 parámetros y 4 parámetros para TopProductos
    List<TopProducto> obtenerTopProductos(LocalDate desde, LocalDate hasta, int limite);
    List<TopProducto> obtenerTopProductos(LocalDate desde, LocalDate hasta, int limite, String tipoProducto);

    List<VentaDia> obtenerVentasPorDia(LocalDate hasta, int dias);

    // Versión de 2 y 3 parámetros para contar artículos
    int contarArticulosVendidos(LocalDate desde, LocalDate hasta);
    int contarArticulosVendidos(LocalDate desde, LocalDate hasta, String tipoProducto);

    /* ===== POS ===== */
    String registrarVentaCompleta(VentaRequest request);
    boolean hayStock(int idProducto, int cantidad);
}