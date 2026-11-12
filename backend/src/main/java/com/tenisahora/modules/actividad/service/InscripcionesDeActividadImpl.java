package com.tenisahora.modules.actividad.service;

import com.tenisahora.modules.actividad.dto.InscripcionRequest;
import com.tenisahora.modules.actividad.dto.InscripcionResponse;
import com.tenisahora.modules.actividad.entity.Actividad;
import com.tenisahora.modules.actividad.entity.EstadoInscripcion;
import com.tenisahora.modules.actividad.entity.InscripcionClase;
import com.tenisahora.modules.actividad.mapper.ClaseMapper;
import com.tenisahora.modules.actividad.repository.ActividadRepository;
import com.tenisahora.modules.actividad.repository.InscripcionClaseRepository;
import com.tenisahora.modules.socio.entity.Socio;
import com.tenisahora.modules.socio.repository.SocioRepository;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.ConflictoException;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import com.tenisahora.shared.exception.SolicitudInvalidaException;
import com.tenisahora.shared.model.Rol;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

/** CU80 y CU82: inscribir y cancelar la inscripción de un socio a una clase o a un entrenamiento. */
@Slf4j
@Service
@RequiredArgsConstructor
public class InscripcionesDeActividadImpl implements InscripcionesDeActividad {

    private final ActividadRepository actividadRepository;
    private final InscripcionClaseRepository inscripcionRepository;
    private final SocioRepository socioRepository;
    private final CupoDeActividad cupoDeActividad;
    private final ClaseMapper claseMapper;
    private final Clock clock;
    private final PoliticaDeBajaDeInscripcion politica;
    private final ApplicationEventPublisher eventos;

    @Override
    @Transactional
    public InscripcionResponse inscribir(Long actividadId, Class<? extends Actividad> tipo, CodigoError codigoNoEncontrada,
                                         InscripcionRequest request, Actor actor) {
        socioIndicadoEsValido(request.socioId(), actor);
        Actividad actividad = actividadParaInscribir(actividadId, tipo, codigoNoEncontrada);
        Socio socio = socioAInscribir(request.socioId(), actor);
        cupoDeActividad.asegurarQueHayLugar(actividad.getId(), actividad.getCupoMaximo());

        InscripcionClase inscripcion = inscripcionRepository.findByActividadIdAndSocioId(actividad.getId(), socio.getId())
                .map(this::reactivar)
                .orElseGet(() -> nueva(actividad, socio));
        inscripcionRepository.save(inscripcion);
        log.info("Socio {} inscripto a la actividad {} por la persona {}", socio.getId(), actividad.getId(), actor.id());
        return claseMapper.aInscripcionResponse(inscripcion);
    }

    @Override
    @Transactional
    public void cancelar(Long actividadId, CodigoError codigoNoEncontrada, Long socioId, Actor actor) {
        if (actor.rol() == Rol.SOCIO && !socioId.equals(actor.id())) {
            throw new AccessDeniedException("Un socio solo cancela su propia inscripción");
        }
        InscripcionClase inscripcion = inscripcionRepository.findByActividadIdAndSocioId(actividadId, socioId)
                .filter(InscripcionClase::getActiva)
                .orElseThrow(() -> new RecursoNoEncontradoException(codigoNoEncontrada, "Inscripción no encontrada"));
        registrarBaja(inscripcion, actor);
        log.info("Inscripción del socio {} a la actividad {} cancelada por la persona {}", socioId, actividadId, actor.id());
    }

    private void registrarBaja(InscripcionClase inscripcion, Actor actor) {
        LocalDateTime ahora = LocalDateTime.now(clock);
        PoliticaDeBajaDeInscripcion.ResultadoDeBaja resultado = politica.evaluar(inscripcion, ahora);
        inscripcion.setActiva(false);
        inscripcion.setEstado(EstadoInscripcion.CANCELADA);
        inscripcion.setFechaCancelacion(ahora);
        inscripcion.setMontoPenalidad(resultado.montoPenalidad());
        inscripcion.setMontoReembolso(resultado.montoReembolso());
        inscripcionRepository.save(inscripcion);
        eventos.publishEvent(new InscripcionCancelada(inscripcion.getId(), resultado.montoReembolso(), actor.id()));
    }

    /** Con valor, retiene el lugar hasta pagar la seña; una actividad sin valor queda confirmada en el acto. */
    private void iniciarPago(InscripcionClase inscripcion, Actividad actividad) {
        BigDecimal valor = actividad.getValorMensual();
        boolean sinCargo = valor == null || valor.signum() <= 0;
        inscripcion.setMontoTotal(valor);
        inscripcion.setEstado(sinCargo ? EstadoInscripcion.CONFIRMADA : EstadoInscripcion.PENDIENTE_PAGO);
        inscripcion.setFechaCreacion(LocalDateTime.now(clock));
        inscripcion.setFechaCancelacion(null);
        inscripcion.setMontoPenalidad(null);
        inscripcion.setMontoReembolso(null);
    }

    private InscripcionClase reactivar(InscripcionClase inscripcion) {
        if (Boolean.TRUE.equals(inscripcion.getActiva())) {
            throw new ConflictoException(CodigoError.YA_INSCRIPTO, "Ya está inscripto en esta actividad");
        }
        inscripcion.setActiva(true);
        inscripcion.setFechaInscripcion(LocalDate.now(clock));
        iniciarPago(inscripcion, inscripcion.getActividad());
        return inscripcion;
    }

    private InscripcionClase nueva(Actividad actividad, Socio socio) {
        InscripcionClase inscripcion = new InscripcionClase();
        inscripcion.setActividad(actividad);
        inscripcion.setSocio(socio);
        inscripcion.setFechaInscripcion(LocalDate.now(clock));
        inscripcion.setActiva(true);
        iniciarPago(inscripcion, actividad);
        return inscripcion;
    }

    private Actividad actividadParaInscribir(Long actividadId, Class<? extends Actividad> tipo, CodigoError codigoNoEncontrada) {
        Actividad actividad = actividadRepository.buscarParaModificar(actividadId)
                .filter(tipo::isInstance)
                .orElseThrow(() -> new RecursoNoEncontradoException(codigoNoEncontrada, "No encontrada"));
        if (!actividad.getEstado().admiteInscripcion()) {
            throw new ReglaDeNegocioException(CodigoError.ACTIVIDAD_NO_INSCRIBIBLE,
                    "La actividad está " + actividad.getEstado() + ": no admite inscripciones");
        }
        return actividad;
    }

    /** Un socio no puede indicar otro socioId: se comprueba antes de buscar la actividad, así un 403 nunca se confunde con un 404. */
    private void socioIndicadoEsValido(Long socioId, Actor actor) {
        if (actor.rol() == Rol.SOCIO && socioId != null && !socioId.equals(actor.id())) {
            throw new AccessDeniedException("Un socio solo se inscribe a sí mismo");
        }
    }

    /** Un socio se inscribe siempre a sí mismo; el personal indica el socio a inscribir. */
    private Socio socioAInscribir(Long socioId, Actor actor) {
        if (actor.rol() == Rol.SOCIO) {
            return socioExistente(actor.id());
        }
        return Optional.ofNullable(socioId)
                .map(this::socioExistente)
                .orElseThrow(() -> new SolicitudInvalidaException(CodigoError.VALIDACION_FALLIDA, "Indicá el socio a inscribir"));
    }

    private Socio socioExistente(Long socioId) {
        return socioRepository.findById(socioId)
                .orElseThrow(() -> new RecursoNoEncontradoException(CodigoError.PERSONA_NO_ENCONTRADA, "Socio no encontrado"));
    }
}
