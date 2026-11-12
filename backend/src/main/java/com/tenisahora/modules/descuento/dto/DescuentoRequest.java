package com.tenisahora.modules.descuento.dto;

import com.tenisahora.modules.descuento.entity.TipoDescuento;
import com.tenisahora.shared.util.Textos;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record DescuentoRequest(
        @Schema(description = "Código con el que se pide el descuento al reservar. Se guarda en mayúsculas", example = "CYBERWEEK")
        @NotBlank @Size(max = 30) String codigo,
        @Schema(description = "Tipo de descuento (PAQUETE_HORAS, LIGA_REGULAR, ESCUELA_AFILIADA, PROMOCIONAL)", example = "PROMOCIONAL")
        @NotNull TipoDescuento tipo,
        @Schema(description = "Porcentaje de beneficio (1-100). Excluyente con montoFijo", example = "15.00")
        @DecimalMin("0.01") @DecimalMax("100.00") @Digits(integer = 3, fraction = 2) BigDecimal porcentaje,
        @Schema(description = "Monto fijo a descontar. Excluyente con porcentaje", example = "5000.00")
        @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal montoFijo,
        @Schema(description = "Condiciones o descripción del descuento", example = "Válido para paquetes de 10 horas")
        @Size(max = 255) String condiciones,
        @Schema(description = "Si el descuento se encuentra activo", example = "true")
        Boolean activo
) {
    public DescuentoRequest {
        condiciones = Textos.recortar(condiciones);
        if (activo == null) {
            activo = true;
        }
    }
}
