package com.tenisahora.modules.actividad.service;

import com.tenisahora.modules.actividad.entity.Actividad;
import com.tenisahora.modules.actividad.entity.ActividadDia;
import com.tenisahora.modules.actividad.repository.ActividadDiaRepository;
import com.tenisahora.modules.actividad.repository.ConteoDeInscriptos;
import com.tenisahora.modules.actividad.repository.InscripcionClaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Trae los días y el cupo ocupado de varias clases o entrenamientos en una sola consulta cada uno, para no hacer
 * N+1 al listarlos. Lo comparten {@code ArmadoDeRespuestasDeClase} y {@code ArmadoDeRespuestasDeEntrenamiento}.
 */
@Component
@RequiredArgsConstructor
class DiasYCupoDeActividades {

    private final ActividadDiaRepository diasRepository;
    private final InscripcionClaseRepository inscripcionRepository;

    Map<Long, Set<DayOfWeek>> diasDe(List<? extends Actividad> actividades) {
        List<Long> ids = actividades.stream().map(Actividad::getId).toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return diasRepository.findByActividadIdIn(ids).stream()
                .collect(Collectors.groupingBy(dia -> dia.getActividad().getId(), Collectors.mapping(ActividadDia::getDiaSemana, Collectors.toSet())));
    }

    Map<Long, Integer> ocupadoDe(List<? extends Actividad> actividades) {
        List<Long> ids = actividades.stream().map(Actividad::getId).toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return inscripcionRepository.contarActivasPorActividad(ids).stream()
                .collect(Collectors.toMap(ConteoDeInscriptos::getActividadId, conteo -> conteo.getCantidad().intValue()));
    }
}
