package com.tenisahora.modules.reserva.service;

import com.tenisahora.modules.reserva.dto.DisponibilidadReservaResponse;

import java.time.LocalDate;
import java.time.LocalTime;

public interface DisponibilidadParaReservar {

    /** CU19: la grilla de todas las canchas activas para una fecha, con filtro opcional por rango horario y por superficie. */
    DisponibilidadReservaResponse consultar(LocalDate fecha, LocalTime horaDesde, LocalTime horaHasta, String superficie);
}
