package org.papeleria_pos.dao.Interface;

import org.papeleria_pos.dto.MovimientoResumen;
import java.time.LocalDate;
import java.util.List;

public interface IMovimientoDAO {
    /** Lista movimientos en un rango de fechas, ordenados por fecha descendente. */
    List<MovimientoResumen> listarPorRango(LocalDate desde, LocalDate hasta);
    int registrar(int idProducto, int idUsuario, String tipo,
                  int cantidad, String motivo, String referencia);
}