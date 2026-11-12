package com.tenisahora.modules.perfil.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** GET y PUT /me/preferencias: misma forma para los dos, el PUT manda los tres booleanos y devuelve cómo quedaron. */
@Schema(description = "Preferencias de notificaciones por correo de la persona.")
public record PreferenciasDeNotificacion(
        @Schema(description = "Recordatorio el día anterior de cada reserva de cancha", example = "true")
        @NotNull(message = "recordatorioReservas es obligatorio")
        Boolean recordatorioReservas,

        @Schema(description = "Recordatorio el día anterior de cada clase o entrenamiento", example = "true")
        @NotNull(message = "recordatorioClases es obligatorio")
        Boolean recordatorioClases,

        @Schema(description = "Avisos de torneos y novedades del club", example = "false")
        @NotNull(message = "novedadesDelClub es obligatorio")
        Boolean novedadesDelClub
) {

    public static final boolean RECORDATORIO_RESERVAS_POR_DEFECTO = true;
    public static final boolean RECORDATORIO_CLASES_POR_DEFECTO = true;
    public static final boolean NOVEDADES_DEL_CLUB_POR_DEFECTO = false;

    public static PreferenciasDeNotificacion porDefecto() {
        return new PreferenciasDeNotificacion(RECORDATORIO_RESERVAS_POR_DEFECTO, RECORDATORIO_CLASES_POR_DEFECTO,
                NOVEDADES_DEL_CLUB_POR_DEFECTO);
    }
}
