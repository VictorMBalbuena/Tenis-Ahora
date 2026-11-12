package com.tenisahora.modules.torneo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Un renglón de la tabla de posiciones de una liga.
 *
 * <p>No hay columna de puntos con una escala inventada: en tenis no hay empate, así que los partidos ganados
 * <b>son</b> la puntuación. Lo que desempata son las diferencias de sets y de games, que es como se lee una tabla
 * de liga de tenis y sale entero de los resultados cargados.
 */
@Schema(description = "Posición de un participante en la tabla de una liga")
public record PosicionResponse(

        @Schema(description = "Puesto, empezando en 1", example = "1")
        int puesto,

        @Schema(example = "12")
        Long inscripcionId,

        @Schema(example = "Ana Gómez")
        String participante,

        @Schema(description = "Partidos jugados", example = "4")
        int jugados,

        @Schema(description = "Partidos ganados", example = "3")
        int ganados,

        @Schema(description = "Partidos perdidos", example = "1")
        int perdidos,

        @Schema(description = "Sets ganados", example = "7")
        int setsAFavor,

        @Schema(description = "Sets perdidos", example = "3")
        int setsEnContra,

        @Schema(description = "Games ganados", example = "48")
        int gamesAFavor,

        @Schema(description = "Games perdidos", example = "31")
        int gamesEnContra) {

    public int diferenciaDeSets() {
        return setsAFavor - setsEnContra;
    }

    public int diferenciaDeGames() {
        return gamesAFavor - gamesEnContra;
    }
}
