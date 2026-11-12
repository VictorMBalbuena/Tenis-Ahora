package com.tenisahora.modules.actividad.service;

import com.tenisahora.modules.actividad.dto.BajaDeInscripcionResponse;
import com.tenisahora.modules.actividad.dto.InscripcionDelSocioResponse;
import com.tenisahora.modules.actividad.entity.Actividad;
import com.tenisahora.modules.actividad.entity.Clase;
import com.tenisahora.modules.actividad.entity.EstadoInscripcion;
import com.tenisahora.modules.actividad.entity.InscripcionClase;
import com.tenisahora.modules.actividad.repository.InscripcionClaseRepository;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import com.tenisahora.shared.model.Rol;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/** Las inscripciones vistas desde quien las paga: estado, seña, saldo y qué pasa si se da de baja. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsultaDeInscripciones {

    private final InscripcionClaseRepository inscripcionRepository;
    private final PagosDeInscripcion pagos;
    private final PoliticaDeBajaDeInscripcion politica;
    private final InscripcionesProperties propiedades;
    private final Clock clock;

    public List<InscripcionDelSocioResponse> delSocio(Long socioId) {
        return inscripcionRepository.delSocio(socioId).stream().map(this::aRespuesta).toList();
    }

    public InscripcionDelSocioResponse detalle(Long id, Actor actor) {
        return aRespuesta(visible(id, actor));
    }

    public BajaDeInscripcionResponse baja(Long id, Actor actor) {
        InscripcionClase inscripcion = visible(id, actor);
        PoliticaDeBajaDeInscripcion.ResultadoDeBaja resultado = politica.evaluar(inscripcion, LocalDateTime.now(clock));
        return new BajaDeInscripcionResponse(resultado.aTiempo(), resultado.montoPenalidad(), resultado.montoReembolso(),
                mensajeDe(resultado));
    }

    private String mensajeDe(PoliticaDeBajaDeInscripcion.ResultadoDeBaja resultado) {
        if (resultado.montoReembolso().signum() > 0) {
            return "Se libera tu lugar y corresponde el reembolso de lo que pagaste.";
        }
        if (resultado.montoPenalidad().signum() > 0) {
            return "Faltan menos de " + propiedades.plazoCancelacionHoras() + " horas para el inicio: se libera tu lugar y no se devuelve lo pagado.";
        }
        return "Se libera tu lugar, sin cargo.";
    }

    private InscripcionClase visible(Long id, Actor actor) {
        InscripcionClase inscripcion = inscripcionRepository.conActividad(id).orElseThrow(this::noEncontrada);
        if (actor.rol() == Rol.SOCIO && !inscripcion.getSocio().getId().equals(actor.id())) {
            throw noEncontrada();
        }
        return inscripcion;
    }

    private RecursoNoEncontradoException noEncontrada() {
        return new RecursoNoEncontradoException(CodigoError.INSCRIPCION_NO_ENCONTRADA, "Inscripción no encontrada");
    }

    private InscripcionDelSocioResponse aRespuesta(InscripcionClase inscripcion) {
        Actividad actividad = inscripcion.getActividad();
        PagosDeInscripcion.Resumen resumen = pagos.resumen(inscripcion);
        return new InscripcionDelSocioResponse(inscripcion.getId(), actividad.getId(),
                actividad instanceof Clase ? "CLASE" : "ENTRENAMIENTO", actividad.getNombre(),
                actividad.getFechaInicio(), actividad.getFechaFin(), actividad.getHoraInicio(), actividad.getHoraFin(),
                inscripcion.getActiva(), inscripcion.getEstado(), inscripcion.getFechaInscripcion(), inscripcion.getMontoTotal(),
                resumen.montoSenia(), resumen.cobrado(), saldoDe(inscripcion, resumen), venceEn(inscripcion),
                inscripcion.getFechaCancelacion(), inscripcion.getMontoPenalidad(), inscripcion.getMontoReembolso());
    }

    private BigDecimal saldoDe(InscripcionClase inscripcion, PagosDeInscripcion.Resumen resumen) {
        if (inscripcion.getMontoTotal() == null || inscripcion.getEstado() == EstadoInscripcion.CANCELADA) {
            return BigDecimal.ZERO;
        }
        return inscripcion.getMontoTotal().subtract(resumen.baseCubierta()).max(BigDecimal.ZERO);
    }

    private LocalDateTime venceEn(InscripcionClase inscripcion) {
        if (inscripcion.getEstado() != EstadoInscripcion.PENDIENTE_PAGO || inscripcion.getFechaCreacion() == null) {
            return null;
        }
        return inscripcion.getFechaCreacion().plusMinutes(propiedades.minutosParaVencer());
    }
}
