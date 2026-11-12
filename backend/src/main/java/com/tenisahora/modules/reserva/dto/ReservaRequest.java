package com.tenisahora.modules.reserva.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Datos para registrar una reserva (CU16). No lleva estado ni montos: los pone el servidor. */
public record ReservaRequest(
        @Schema(example = "1") @NotNull @Positive Long canchaId,
        @Schema(description = "Hasta 30 días desde hoy", example = "2026-10-20") @NotNull LocalDate fecha,
        @Schema(description = "Hora en punto", example = "18:00", type = "string") @NotNull LocalTime horaInicio,
        @Schema(description = "1 o 2 horas", example = "2") @NotNull @Min(1) @Max(12) Integer duracionHoras,
        @Schema(description = "Solo lo indica un empleado: el socio titular. Un socio reserva siempre para sí mismo", example = "3") @Positive Long titularId,
        @Schema(description = "1 o 3 acompañantes: con el titular, los jugadores son 2 o 4")
        @NotEmpty @Size(max = 3) List<@NotNull @Valid JugadorRequest> acompaniantes,
        @Schema(description = "Insumos que lleva la reserva (opcional, CU13). Si no hay stock para ese horario, la reserva no se crea")
        @Size(max = 10) List<@NotNull @Valid InsumoPedidoRequest> insumos,
        @Schema(description = "Código de descuento a aplicar sobre el total (opcional, RF69)", example = "CYBERWEEK")
        @Size(max = 30) String codigoDescuento
) {

    /** Sin código de descuento, que es como se reservaba antes de que existieran. */
    public ReservaRequest(Long canchaId, LocalDate fecha, LocalTime horaInicio, Integer duracionHoras, Long titularId,
                          List<JugadorRequest> acompaniantes, List<InsumoPedidoRequest> insumos) {
        this(canchaId, fecha, horaInicio, duracionHoras, titularId, acompaniantes, insumos, null);
    }
}
