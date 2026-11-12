package com.tenisahora.modules.actividad.service;

import com.tenisahora.modules.actividad.dto.ClaseResponse;
import com.tenisahora.modules.actividad.dto.MiClaseResponse;
import com.tenisahora.modules.actividad.entity.Clase;
import com.tenisahora.modules.actividad.mapper.ClaseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Arma las respuestas de clase con sus días y su cupo ocupado, apoyado en {@link DiasYCupoDeActividades}. */
@Component
@RequiredArgsConstructor
public class ArmadoDeRespuestasDeClase {

    private final DiasYCupoDeActividades diasYCupo;
    private final ClaseMapper claseMapper;

    public ClaseResponse deClase(Clase clase) {
        return deClases(List.of(clase)).get(0);
    }

    public List<ClaseResponse> deClases(List<Clase> clases) {
        Map<Long, Set<DayOfWeek>> dias = diasYCupo.diasDe(clases);
        Map<Long, Integer> ocupado = diasYCupo.ocupadoDe(clases);
        return clases.stream()
                .map(clase -> claseMapper.aRespuesta(clase, dias.getOrDefault(clase.getId(), Set.of()), ocupado.getOrDefault(clase.getId(), 0)))
                .toList();
    }

    public MiClaseResponse miClase(Clase clase) {
        return misClases(List.of(clase)).get(0);
    }

    public List<MiClaseResponse> misClases(List<Clase> clases) {
        Map<Long, Set<DayOfWeek>> dias = diasYCupo.diasDe(clases);
        Map<Long, Integer> ocupado = diasYCupo.ocupadoDe(clases);
        return clases.stream()
                .map(clase -> claseMapper.aMiClase(clase, dias.getOrDefault(clase.getId(), Set.of()), ocupado.getOrDefault(clase.getId(), 0)))
                .toList();
    }
}
