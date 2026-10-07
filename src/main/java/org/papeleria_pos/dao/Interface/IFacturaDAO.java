package org.papeleria_pos.dao.Interface;

import org.papeleria_pos.dto.FacturaRequest;
import org.papeleria_pos.dto.FacturaResumen;
import org.papeleria_pos.models.Factura;

import java.time.LocalDate;
import java.util.List;

public interface IFacturaDAO {

    /** Genera una factura en estado GENERADA. */
    Factura generar(FacturaRequest req);

    /** Devuelve la factura asociada a una venta, o null. */
    Factura obtenerPorVenta(int idVenta);

    /** Devuelve una factura por id. */
    Factura obtenerPorId(int idFactura);

    /** Historial de facturas por rango (para Reportes). */
    List<FacturaResumen> listarPorRango(LocalDate desde, LocalDate hasta);

    /** Cancela una factura. */
    boolean cancelar(int idFactura);

    /** Cuenta cuántas facturas hay en el período. */
    int contarPorRango(LocalDate desde, LocalDate hasta);
}