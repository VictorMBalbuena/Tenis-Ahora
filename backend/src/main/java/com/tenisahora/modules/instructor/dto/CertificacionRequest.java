package com.tenisahora.modules.instructor.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

import static com.tenisahora.shared.util.Textos.recortar;

/** Datos de la certificación o título habilitante (CU30, RD02). */
@Schema(description = "Título o certificación habilitante del instructor.")
public record CertificacionRequest(
        @Schema(description = "Único entre todos los instructores", example = "CERT-2026-001")
        @NotBlank(message = "El número de certificación es obligatorio")
        @Size(max = 50, message = "El número de certificación no puede superar los 50 caracteres")
        String numeroCertificacion,

        @Schema(example = "Federación Argentina de Tenis")
        @NotBlank(message = "La entidad emisora es obligatoria")
        @Size(max = 120, message = "La entidad emisora no puede superar los 120 caracteres")
        String entidadEmisora,

        @Schema(example = "2024-03-01")
        @NotNull(message = "La fecha de emisión es obligatoria")
        @PastOrPresent(message = "La fecha de emisión no puede ser posterior a hoy")
        LocalDate fechaEmision,

        @Schema(description = "Si no se envía, el título no vence", example = "2027-03-01")
        LocalDate fechaVencimiento,

        @Schema(description = "URL del archivo escaneado del título", example = "https://club.com/titulos/cert-2026-001.pdf")
        @Size(max = 300, message = "La URL del archivo no puede superar los 300 caracteres")
        String archivoTituloUrl
) {

    public CertificacionRequest {
        numeroCertificacion = recortar(numeroCertificacion);
        entidadEmisora = recortar(entidadEmisora);
        archivoTituloUrl = recortar(archivoTituloUrl);
    }
}
