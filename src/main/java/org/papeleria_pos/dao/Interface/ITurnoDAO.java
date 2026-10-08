package org.papeleria_pos.dao.Interface;

import org.papeleria_pos.dto.TurnoAbierto;
import org.papeleria_pos.dto.TurnoResumen;

import java.time.LocalDate;
import java.util.List;

public interface ITurnoDAO {

    int contarAbiertos();
    TurnoAbierto obtenerTurnoAbierto(int idCajero);
    int abrirTurno(int idCajero, int caja, double montoInicial);
    boolean cerrarTurno(int idTurno, double montoFinal);
    List<TurnoResumen> listarPorRango(LocalDate desde, LocalDate hasta);
}