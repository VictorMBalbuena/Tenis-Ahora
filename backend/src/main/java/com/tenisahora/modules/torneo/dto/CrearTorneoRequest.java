package com.tenisahora.modules.torneo.dto;

import com.tenisahora.modules.torneo.entity.CategoriaTorneo;
import com.tenisahora.modules.torneo.entity.Modalidad;
import com.tenisahora.modules.torneo.entity.TipoCompetencia;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CrearTorneoRequest(
        String nombre,
        /** TORNEO (eliminación directa) o LIGA (todos contra todos). Si no viene, es un torneo. */
        TipoCompetencia tipo,
        Long canchaId,
        @NotNull(message = "La categoría es obligatoria") CategoriaTorneo categoria,
        @NotNull(message = "La modalidad es obligatoria") Modalidad modalidad,
        @NotBlank(message = "El tipo de superficie es obligatorio") String tipoSuperficie,
        @NotNull(message = "La fecha de inicio es obligatoria") LocalDate fechaInicio,
        @NotNull(message = "La fecha de fin es obligatoria") LocalDate fechaFin,
        BigDecimal costoInscripcion,
        @Size(max = 2000, message = "El reglamento no puede superar los 2000 caracteres") String reglamento
) {

    /** Sin indicar el tipo: es un torneo, que es lo que había antes de que existieran las ligas. */
    public CrearTorneoRequest(String nombre, Long canchaId, CategoriaTorneo categoria, Modalidad modalidad,
                              String tipoSuperficie, LocalDate fechaInicio, LocalDate fechaFin,
                              BigDecimal costoInscripcion, String reglamento) {
        this(nombre, TipoCompetencia.TORNEO, canchaId, categoria, modalidad, tipoSuperficie, fechaInicio, fechaFin,
                costoInscripcion, reglamento);
    }

    /** El tipo no es obligatorio: lo que ya existía eran todos torneos. */
    public TipoCompetencia tipoOTorneo() {
        return tipo == null ? TipoCompetencia.TORNEO : tipo;
    }
}
