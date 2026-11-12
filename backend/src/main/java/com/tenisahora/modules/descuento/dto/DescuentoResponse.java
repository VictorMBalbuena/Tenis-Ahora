package com.tenisahora.modules.descuento.dto;

import com.tenisahora.modules.descuento.entity.TipoDescuento;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

public record DescuentoResponse(
        @Schema(example = "1") Long id,
        @Schema(description = "Código con el que se pide el descuento al reservar", example = "CYBERWEEK") String codigo,
        @Schema(example = "PAQUETE_HORAS") TipoDescuento tipo,
        @Schema(example = "15.00") BigDecimal porcentaje,
        @Schema(example = "null") BigDecimal montoFijo,
        @Schema(example = "Válido para paquetes de 10 horas") String condiciones,
        @Schema(example = "true") Boolean activo,
        @Schema(description = "Cantidad de veces que fue aplicado en cobros registrados", example = "12") long vecesAplicado
) {
}
