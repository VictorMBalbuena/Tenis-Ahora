package com.tenisahora.modules.actividad.dto;

import com.tenisahora.modules.actividad.entity.EstadoInscripcion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Una inscripción con su situación de pago. Incluye las canceladas: es también el historial del socio. */
public record InscripcionDelSocioResponse(
        Long id,
        Long actividadId,
        String tipo,
        String actividad,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        LocalTime horaInicio,
        LocalTime horaFin,
        Boolean activa,
        EstadoInscripcion estado,
        LocalDate fechaInscripcion,
        BigDecimal montoTotal,
        BigDecimal montoSenia,
        BigDecimal montoPagado,
        BigDecimal saldoPendiente,
        LocalDateTime venceEn,
        LocalDateTime fechaCancelacion,
        BigDecimal montoPenalidad,
        BigDecimal montoReembolso
) {
}
