package org.papeleria_pos.dao.Interface;

import org.papeleria_pos.dto.TurnoAbierto;

public interface ITurnoDAO {

    int contarAbiertos();
    TurnoAbierto obtenerTurnoAbierto(int idCajero);
    int abrirTurno(int idCajero, int caja, double montoInicial);
    boolean cerrarTurno(int idTurno, double montoFinal);
}