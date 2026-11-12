package com.tenisahora.modules.torneo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tenisahora.modules.torneo.entity.CategoriaTorneo;
import com.tenisahora.modules.torneo.entity.EstadoTorneo;
import com.tenisahora.modules.torneo.entity.Modalidad;
import com.tenisahora.modules.torneo.entity.TipoCompetencia;
import com.tenisahora.modules.torneo.entity.Torneo;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TorneoResponse(
        Long id,
        String nombre,
        TipoCompetencia tipo,
        Long canchaId,
        String identificadorCancha,
        CategoriaTorneo categoria,
        Modalidad modalidad,
        String tipoSuperficie,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        EstadoTorneo estado,
        BigDecimal costoInscripcion,
        String reglamento,
        Integer cantidadInscriptos) {
    @JsonProperty("admiteInscripcion")
    public boolean admiteInscripcion() {
        return estado == EstadoTorneo.PLANIFICADO;
    }

    public static TorneoResponse de(Torneo torneo) {
        return de(torneo, null);
    }

    public static TorneoResponse de(Torneo torneo, Integer cantidadInscriptos) {
        Long canchaId = torneo.getCancha() != null ? torneo.getCancha().getId() : null;
        String identificadorCancha = torneo.getCancha() != null ? torneo.getCancha().getIdentificador() : null;
        return new TorneoResponse(
                torneo.getId(),
                torneo.getNombre(),
                torneo.getTipo() == null ? TipoCompetencia.TORNEO : torneo.getTipo(),
                canchaId,
                identificadorCancha,
                torneo.getCategoria(),
                torneo.getModalidad(),
                torneo.getTipoSuperficie(),
                torneo.getFechaInicio(),
                torneo.getFechaFin(),
                torneo.getEstado(),
                torneo.getCostoInscripcion(),
                torneo.getReglamento(),
                cantidadInscriptos);
    }
}
