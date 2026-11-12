package com.tenisahora.modules.torneo.dto;

import com.tenisahora.modules.torneo.entity.CategoriaTorneo;
import com.tenisahora.modules.torneo.entity.Modalidad;
import com.tenisahora.modules.torneo.entity.TipoCompetencia;

import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ModificarTorneoRequest(
        String nombre,
        TipoCompetencia tipo,
        Long canchaId,
        CategoriaTorneo categoria,
        Modalidad modalidad,
        String tipoSuperficie,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        BigDecimal costoInscripcion,
        @Size(max = 2000, message = "El reglamento no puede superar los 2000 caracteres") String reglamento
) {

    /** Sin tocar el tipo de competencia, que es lo más común al editar. */
    public ModificarTorneoRequest(String nombre, Long canchaId, CategoriaTorneo categoria, Modalidad modalidad,
                                  String tipoSuperficie, LocalDate fechaInicio, LocalDate fechaFin,
                                  BigDecimal costoInscripcion, String reglamento) {
        this(nombre, null, canchaId, categoria, modalidad, tipoSuperficie, fechaInicio, fechaFin, costoInscripcion,
                reglamento);
    }
}
