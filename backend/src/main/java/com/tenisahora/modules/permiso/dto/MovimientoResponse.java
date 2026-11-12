package com.tenisahora.modules.permiso.dto;

import com.tenisahora.modules.permiso.entity.MovimientoDeCuenta;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/** Un renglón del historial de la cuenta: qué se le hizo, quién y cuándo (RF74, RF76). */
@Schema(description = "Movimiento registrado sobre una cuenta")
public record MovimientoResponse(
        Long id,

        @Schema(example = "CAMBIO_DE_ROL")
        MovimientoDeCuenta.Tipo tipo,

        String detalle,
        String motivo,
        Long hechoPorId,
        LocalDateTime fecha) {

    public static MovimientoResponse de(MovimientoDeCuenta movimiento) {
        return new MovimientoResponse(movimiento.getId(), movimiento.getTipo(), movimiento.getDetalle(),
                movimiento.getMotivo(), movimiento.getHechoPorId(), movimiento.getFecha());
    }
}
