package com.tenisahora.modules.actividad.mapper;

import com.tenisahora.modules.actividad.dto.EntrenamientoResponse;
import com.tenisahora.modules.actividad.dto.MiEntrenamientoResponse;
import com.tenisahora.modules.actividad.entity.Entrenamiento;
import com.tenisahora.shared.entity.Persona;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.util.Set;

@Component
public class EntrenamientoMapper {

    public EntrenamientoResponse aRespuesta(Entrenamiento entrenamiento, Set<DayOfWeek> dias, int cupoOcupado) {
        return new EntrenamientoResponse(entrenamiento.getId(), entrenamiento.getCodigo(), entrenamiento.getNombre(),
                entrenamiento.getModalidad(), entrenamiento.getObjetivo(), entrenamiento.getEstado(), entrenamiento.getCancha().getId(),
                entrenamiento.getCancha().getIdentificador(), entrenamiento.getEntrenador().getId(), nombreDe(entrenamiento.getEntrenador()),
                entrenamiento.getFechaInicio(), entrenamiento.getFechaFin(), dias, entrenamiento.getHoraInicio(), entrenamiento.getHoraFin(),
                entrenamiento.getCupoMaximo(), entrenamiento.getCupoMinimo(), cupoOcupado, entrenamiento.getCupoMaximo() - cupoOcupado,
                entrenamiento.getValorMensual(), entrenamiento.getCategoria());
    }

    public MiEntrenamientoResponse aMiEntrenamiento(Entrenamiento entrenamiento, Set<DayOfWeek> dias, int cupoOcupado) {
        return new MiEntrenamientoResponse(entrenamiento.getId(), entrenamiento.getCodigo(), entrenamiento.getNombre(),
                entrenamiento.getObjetivo(), entrenamiento.getEstado(), entrenamiento.getCancha().getIdentificador(),
                entrenamiento.getFechaInicio(), entrenamiento.getFechaFin(), dias, entrenamiento.getHoraInicio(), entrenamiento.getHoraFin(),
                entrenamiento.getCupoMaximo(), cupoOcupado);
    }

    private String nombreDe(Persona persona) {
        return persona.getApellido() + ", " + persona.getNombre();
    }
}
