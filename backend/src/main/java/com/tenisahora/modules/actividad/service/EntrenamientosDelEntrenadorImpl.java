package com.tenisahora.modules.actividad.service;

import com.tenisahora.modules.actividad.dto.AlumnoDeClaseResponse;
import com.tenisahora.modules.actividad.dto.MiEntrenamientoResponse;
import com.tenisahora.modules.actividad.entity.Entrenamiento;
import com.tenisahora.modules.actividad.mapper.ClaseMapper;
import com.tenisahora.modules.actividad.repository.EntrenamientoRepository;
import com.tenisahora.modules.actividad.repository.InscripcionClaseRepository;
import com.tenisahora.modules.instructor.entity.Entrenador;
import com.tenisahora.modules.instructor.repository.EntrenadorRepository;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.shared.dto.PageResponse;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Espejo de ClasesDelProfesorImpl. */
@Service
@RequiredArgsConstructor
public class EntrenamientosDelEntrenadorImpl implements EntrenamientosDelEntrenador {

    private static final Sort ORDEN = Sort.by(Sort.Order.desc("fechaInicio"));

    private final EntrenamientoRepository entrenamientoRepository;
    private final InscripcionClaseRepository inscripcionRepository;
    private final EntrenadorRepository entrenadorRepository;
    private final ArmadoDeRespuestasDeEntrenamiento respuestas;
    private final ClaseMapper claseMapper;
    private final AccesoAActividades acceso;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<MiEntrenamientoResponse> misEntrenamientos(Actor actor, int page, int size) {
        asegurarEntrenadorValidado(actor.id());
        Page<Entrenamiento> pagina = entrenamientoRepository.findByEntrenadorId(actor.id(), PageRequest.of(page, size, ORDEN));
        return new PageResponse<>(respuestas.misEntrenamientos(pagina.getContent()), pagina.getNumber(), pagina.getSize(),
                pagina.getTotalElements(), pagina.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlumnoDeClaseResponse> inscriptos(Long entrenamientoId, Actor actor) {
        Entrenamiento entrenamiento = acceso.paraOperar(entrenamientoId, Entrenamiento.class, CodigoError.ENTRENAMIENTO_NO_ENCONTRADO, actor);
        return inscripcionRepository.deLaActividad(entrenamiento.getId()).stream().map(claseMapper::aAlumno).toList();
    }

    private void asegurarEntrenadorValidado(Long entrenadorId) {
        Entrenador entrenador = entrenadorRepository.findById(entrenadorId)
                .orElseThrow(() -> new RecursoNoEncontradoException(CodigoError.PERSONA_NO_ENCONTRADA, "Entrenador no encontrado"));
        if (!entrenador.estaValidado()) {
            throw new ReglaDeNegocioException(CodigoError.INSTRUCTOR_NO_VALIDADO, "No tenés la certificación validada");
        }
    }
}
