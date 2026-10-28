package org.papeleria_pos.dao.Interface;

import org.papeleria_pos.dto.TurnoAbierto;
import org.papeleria_pos.dto.TurnoResumen;

import java.time.LocalDate;
import java.util.List;

public interface ITurnoDAO {



    TurnoAbierto obtenerTurnoAbierto(int idCajero);
    int abrirTurno(int idCajero, int caja, double montoInicial);
    boolean cerrarTurno(int idTurno, double montoEsperado, double montoContado, double diferencia);
    double calcularEfectivoEsperado(int idTurno);
    List<TurnoResumen> listarPorRango(LocalDate desde, LocalDate hasta);
    int contarAbiertos();
}