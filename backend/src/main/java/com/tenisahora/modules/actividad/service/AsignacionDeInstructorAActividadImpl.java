package com.tenisahora.modules.actividad.service;

import com.tenisahora.modules.actividad.dto.AsignacionDeInstructorRequest;
import com.tenisahora.modules.actividad.dto.InstructorDisponibleResponse;
import com.tenisahora.modules.actividad.entity.Actividad;
import com.tenisahora.modules.actividad.entity.ActividadDia;
import com.tenisahora.modules.actividad.entity.EstadoActividad;
import com.tenisahora.modules.actividad.entity.Modalidad;
import com.tenisahora.modules.actividad.repository.ActividadDiaRepository;
import com.tenisahora.modules.actividad.repository.ActividadRepository;
import com.tenisahora.modules.cancha.service.FranjaHoraria;
import com.tenisahora.modules.instructor.entity.Entrenador;
import com.tenisahora.modules.instructor.entity.Profesor;
import com.tenisahora.modules.instructor.repository.EntrenadorRepository;
import com.tenisahora.modules.instructor.repository.ProfesorRepository;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import com.tenisahora.shared.model.Rol;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** CU44: asignar un profesor o un entrenador a una actividad particular. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsignacionDeInstructorAActividadImpl implements AsignacionDeInstructorAActividad {

    private static final List<EstadoActividad> QUE_OCUPAN = EstadoActividad.queOcupanCancha();

    private final ActividadRepository actividadRepository;
    private final ActividadDiaRepository diasRepository;
    private final ProfesorRepository profesorRepository;
    private final EntrenadorRepository entrenadorRepository;

    @Override
    @Transactional(readOnly = true)
    public List<InstructorDisponibleResponse> disponibles(Long actividadId) {
        Actividad actividad = actividadParticular(actividadId);
        Set<DayOfWeek> dias = diasRepository.findByActividadId(actividadId).stream().map(ActividadDia::getDiaSemana).collect(Collectors.toSet());
        FranjaHoraria franja = new FranjaHoraria(actividad.getHoraInicio(), actividad.getHoraFin());
        LocalDate desde = actividad.getFechaInicio();
        LocalDate hasta = actividad.getFechaFin();

        List<InstructorDisponibleResponse> disponibles = profesorRepository.findAll().stream()
                .filter(Profesor::estaValidado)
                .filter(p -> !actividadRepository.profesorConSuperposicion(p.getId(), desde, hasta, dias, franja.desde(), franja.hasta(), QUE_OCUPAN, actividadId))
                .map(p -> new InstructorDisponibleResponse(p.getId(), p.getApellido() + ", " + p.getNombre(), Rol.PROFESOR))
                .collect(Collectors.toCollection(ArrayList::new));
        entrenadorRepository.findAll().stream()
                .filter(Entrenador::estaValidado)
                .filter(e -> !actividadRepository.entrenadorConSuperposicion(e.getId(), desde, hasta, dias, franja.desde(), franja.hasta(), QUE_OCUPAN, actividadId))
                .map(e -> new InstructorDisponibleResponse(e.getId(), e.getApellido() + ", " + e.getNombre(), Rol.ENTRENADOR))
                .forEach(disponibles::add);
        return disponibles;
    }

    @Override
    @Transactional
    public void asignar(Long actividadId, AsignacionDeInstructorRequest request, Actor actor) {
        Actividad actividad = actividadParticular(actividadId);
        if (request.rol() == Rol.PROFESOR) {
            actividad.setProfesor(profesorValidado(request.instructorId()));
            actividad.setEntrenador(null);
        } else if (request.rol() == Rol.ENTRENADOR) {
            actividad.setEntrenador(entrenadorValidado(request.instructorId()));
            actividad.setProfesor(null);
        } else {
            throw new ReglaDeNegocioException(CodigoError.MODALIDAD_INVALIDA, "El rol debe ser PROFESOR o ENTRENADOR");
        }
        actividadRepository.save(actividad);
        log.info("Instructor {} ({}) asignado a la actividad {} por la persona {}", request.instructorId(), request.rol(), actividadId, actor.id());
    }

    private Actividad actividadParticular(Long actividadId) {
        Actividad actividad = actividadRepository.findById(actividadId)
                .orElseThrow(() -> new RecursoNoEncontradoException(CodigoError.ACTIVIDAD_NO_ENCONTRADA, "Actividad no encontrada"));
        if (actividad.getModalidad() != Modalidad.PARTICULAR) {
            throw new ReglaDeNegocioException(CodigoError.MODALIDAD_INVALIDA, "Solo se puede asignar instructor a una actividad particular");
        }
        return actividad;
    }

    private Profesor profesorValidado(Long profesorId) {
        Profesor profesor = profesorRepository.findById(profesorId)
                .orElseThrow(() -> new RecursoNoEncontradoException(CodigoError.PERSONA_NO_ENCONTRADA, "Profesor no encontrado"));
        if (!profesor.estaValidado()) {
            throw new ReglaDeNegocioException(CodigoError.INSTRUCTOR_NO_VALIDADO, "El profesor no tiene la certificación validada");
        }
        return profesor;
    }

    private Entrenador entrenadorValidado(Long entrenadorId) {
        Entrenador entrenador = entrenadorRepository.findById(entrenadorId)
                .orElseThrow(() -> new RecursoNoEncontradoException(CodigoError.PERSONA_NO_ENCONTRADA, "Entrenador no encontrado"));
        if (!entrenador.estaValidado()) {
            throw new ReglaDeNegocioException(CodigoError.INSTRUCTOR_NO_VALIDADO, "El entrenador no tiene la certificación validada");
        }
        return entrenador;
    }
}
