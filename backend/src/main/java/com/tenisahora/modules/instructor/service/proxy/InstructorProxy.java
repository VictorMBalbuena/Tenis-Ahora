package com.tenisahora.modules.instructor.service.proxy;

import com.tenisahora.modules.instructor.dto.HabilitacionResponse;
import com.tenisahora.modules.instructor.entity.Instructor;
import com.tenisahora.modules.instructor.repository.EntrenadorRepository;
import com.tenisahora.modules.instructor.repository.ProfesorRepository;
import com.tenisahora.modules.instructor.service.AsignacionDeInstructores;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import com.tenisahora.shared.model.Rol;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Patrón Proxy: controla el acceso a la asignación de un instructor, negándola si no está habilitado (RD02, RD03).
 * El guardián único que van a usar BACK 11 y BACK 12: si la regla cambia, cambia acá y no en cada módulo que asigna.
 */
@Component
@RequiredArgsConstructor
public class InstructorProxy implements AsignacionDeInstructores {

    private final ProfesorRepository profesores;
    private final EntrenadorRepository entrenadores;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public void asegurarAsignable(Rol rol, Long instructorId) {
        motivoNoAsignable(buscar(rol, instructorId)).ifPresent(motivo -> {
            throw new ReglaDeNegocioException(CodigoError.INSTRUCTOR_NO_VALIDADO, motivo);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public HabilitacionResponse habilitacion(Rol rol, Long instructorId) {
        return motivoNoAsignable(buscar(rol, instructorId))
                .map(HabilitacionResponse::deNoAsignable)
                .orElseGet(HabilitacionResponse::deAsignable);
    }

    private Optional<String> motivoNoAsignable(Instructor instructor) {
        if (!instructor.getEstadoValidacion().puedeAsignarse()) {
            return Optional.of("La certificación no está validada");
        }
        if (instructor.getCertificacion() == null) {
            return Optional.of("No tiene una certificación registrada");
        }
        if (!instructor.getCertificacion().estaVigente(LocalDate.now(clock))) {
            return Optional.of("La certificación está vencida");
        }
        return Optional.empty();
    }

    private Instructor buscar(Rol rol, Long id) {
        return switch (rol) {
            case PROFESOR -> profesores.findById(id).map(Instructor.class::cast).orElseThrow(this::noEncontrado);
            case ENTRENADOR -> entrenadores.findById(id).map(Instructor.class::cast).orElseThrow(this::noEncontrado);
            default -> throw new IllegalArgumentException("Rol sin instructor: " + rol);
        };
    }

    private RecursoNoEncontradoException noEncontrado() {
        return new RecursoNoEncontradoException(CodigoError.PERSONA_NO_ENCONTRADA, "Persona no encontrada");
    }
}
