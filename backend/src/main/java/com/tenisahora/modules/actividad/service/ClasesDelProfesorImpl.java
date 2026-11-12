package com.tenisahora.modules.actividad.service;

import com.tenisahora.modules.actividad.dto.AlumnoDeClaseResponse;
import com.tenisahora.modules.actividad.dto.MiClaseResponse;
import com.tenisahora.modules.actividad.entity.Clase;
import com.tenisahora.modules.actividad.mapper.ClaseMapper;
import com.tenisahora.modules.actividad.repository.ClaseRepository;
import com.tenisahora.modules.actividad.repository.InscripcionClaseRepository;
import com.tenisahora.modules.instructor.entity.Profesor;
import com.tenisahora.modules.instructor.repository.ProfesorRepository;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.shared.dto.PageResponse;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import static com.tenisahora.shared.exception.CodigoError.CLASE_NO_ENCONTRADA;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClasesDelProfesorImpl implements ClasesDelProfesor {

    private static final Sort ORDEN = Sort.by(Sort.Order.desc("fechaInicio"));

    private final ClaseRepository claseRepository;
    private final InscripcionClaseRepository inscripcionRepository;
    private final ProfesorRepository profesorRepository;
    private final ArmadoDeRespuestasDeClase respuestas;
    private final ClaseMapper claseMapper;
    private final AccesoAActividades acceso;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<MiClaseResponse> misClases(Actor actor, int page, int size) {
        asegurarProfesorValidado(actor.id());
        Page<Clase> pagina = claseRepository.findByProfesorId(actor.id(), PageRequest.of(page, size, ORDEN));
        return new PageResponse<>(respuestas.misClases(pagina.getContent()), pagina.getNumber(), pagina.getSize(),
                pagina.getTotalElements(), pagina.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlumnoDeClaseResponse> inscriptos(Long claseId, Actor actor) {
        Clase clase = acceso.paraOperar(claseId, Clase.class, CLASE_NO_ENCONTRADA, actor);
        return inscripcionRepository.deLaActividad(clase.getId()).stream().map(claseMapper::aAlumno).toList();
    }

    private void asegurarProfesorValidado(Long profesorId) {
        Profesor profesor = profesorRepository.findById(profesorId)
                .orElseThrow(() -> new RecursoNoEncontradoException(CodigoError.PERSONA_NO_ENCONTRADA, "Profesor no encontrado"));
        if (!profesor.estaValidado()) {
            throw new ReglaDeNegocioException(CodigoError.PROFESOR_NO_VALIDADO, "No tenés la certificación validada");
        }
    }
}
