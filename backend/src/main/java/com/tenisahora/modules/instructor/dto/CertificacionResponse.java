package com.tenisahora.modules.instructor.dto;

import com.tenisahora.modules.instructor.entity.Certificacion;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Título o certificación habilitante del instructor, con su vigencia.")
public record CertificacionResponse(
        @Schema(example = "CERT-2026-001") String numeroCertificacion,
        @Schema(example = "Federación Argentina de Tenis") String entidadEmisora,
        @Schema(example = "2024-03-01") LocalDate fechaEmision,
        @Schema(example = "2027-03-01") LocalDate fechaVencimiento,
        @Schema(example = "https://club.com/titulos/cert-2026-001.pdf") String archivoTituloUrl,
        @Schema(description = "Si no está vencida a la fecha de hoy", example = "true") boolean vigente
) {

    public static CertificacionResponse de(Certificacion certificacion, LocalDate hoy) {
        return new CertificacionResponse(certificacion.getNumeroCertificacion(), certificacion.getEntidadEmisora(),
                certificacion.getFechaEmision(), certificacion.getFechaVencimiento(), certificacion.getArchivoTituloUrl(),
                certificacion.estaVigente(hoy));
    }
}
