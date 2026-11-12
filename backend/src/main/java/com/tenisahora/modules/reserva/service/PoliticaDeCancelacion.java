package com.tenisahora.modules.reserva.service;

import com.tenisahora.modules.reserva.entity.Reserva;

import java.time.LocalDateTime;

/** Strategy: cómo se trata una cancelación. Cambiar la política del club es cambiar esta pieza, no el servicio de reservas. */
public interface PoliticaDeCancelacion {

    ResultadoDeCancelacion evaluar(Reserva reserva, LocalDateTime ahora);
}
