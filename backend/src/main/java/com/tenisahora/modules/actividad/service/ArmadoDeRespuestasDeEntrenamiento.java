package com.tenisahora.modules.actividad.service;

import com.tenisahora.modules.actividad.dto.EntrenamientoResponse;
import com.tenisahora.modules.actividad.dto.MiEntrenamientoResponse;
import com.tenisahora.modules.actividad.entity.Entrenamiento;
import com.tenisahora.modules.actividad.mapper.EntrenamientoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Arma las respuestas de entrenamiento con sus días y su cupo ocupado, apoyado en {@link DiasYCupoDeActividades}. Espejo de ArmadoDeRespuestasDeClase. */
@Component
@RequiredArgsConstructor
public class ArmadoDeRespuestasDeEntrenamiento {

    private final DiasYCupoDeActividades diasYCupo;
    private final EntrenamientoMapper entrenamientoMapper;

    public EntrenamientoResponse deEntrenamiento(Entrenamiento entrenamiento) {
        return deEntrenamientos(List.of(entrenamiento)).get(0);
    }

    public List<EntrenamientoResponse> deEntrenamientos(List<Entrenamiento> entrenamientos) {
        Map<Long, Set<DayOfWeek>> dias = diasYCupo.diasDe(entrenamientos);
        Map<Long, Integer> ocupado = diasYCupo.ocupadoDe(entrenamientos);
        return entrenamientos.stream()
                .map(e -> entrenamientoMapper.aRespuesta(e, dias.getOrDefault(e.getId(), Set.of()), ocupado.getOrDefault(e.getId(), 0)))
                .toList();
    }

    public MiEntrenamientoResponse miEntrenamiento(Entrenamiento entrenamiento) {
        return misEntrenamientos(List.of(entrenamiento)).get(0);
    }

    public List<MiEntrenamientoResponse> misEntrenamientos(List<Entrenamiento> entrenamientos) {
        Map<Long, Set<DayOfWeek>> dias = diasYCupo.diasDe(entrenamientos);
        Map<Long, Integer> ocupado = diasYCupo.ocupadoDe(entrenamientos);
        return entrenamientos.stream()
                .map(e -> entrenamientoMapper.aMiEntrenamiento(e, dias.getOrDefault(e.getId(), Set.of()), ocupado.getOrDefault(e.getId(), 0)))
                .toList();
    }
}
