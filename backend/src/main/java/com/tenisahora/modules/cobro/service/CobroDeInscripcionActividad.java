package com.tenisahora.modules.cobro.service;

import com.tenisahora.modules.actividad.entity.EstadoInscripcion;
import com.tenisahora.modules.actividad.entity.InscripcionClase;
import com.tenisahora.modules.actividad.repository.InscripcionClaseRepository;
import com.tenisahora.modules.actividad.service.ConfirmacionDeInscripcion;
import com.tenisahora.modules.actividad.service.PagosDeInscripcion;
import com.tenisahora.modules.cobro.dto.CobroRequest;
import com.tenisahora.modules.cobro.dto.CobroResponse;
import com.tenisahora.modules.cobro.entity.Cobro;
import com.tenisahora.modules.cobro.entity.ConceptoCobro;
import com.tenisahora.modules.cobro.entity.EstadoCobro;
import com.tenisahora.modules.cobro.entity.MedioPago;
import com.tenisahora.modules.cobro.mapper.CobroMapper;
import com.tenisahora.modules.cobro.repository.CobroRepository;
import com.tenisahora.modules.reserva.service.AccesoAReservas;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.ConflictoException;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** Clases y entrenamientos se cobran como una cancha: el 50% para confirmar el lugar y después el saldo. */
@Service
@RequiredArgsConstructor
public class CobroDeInscripcionActividad {

    private final InscripcionClaseRepository inscripcionRepository;
    private final CobroRepository cobroRepository;
    private final PagosDeInscripcion pagos;
    private final ConfirmacionDeInscripcion confirmacion;
    private final EjecucionDeCobro ejecucion;
    private final ReemplazoDeQr reemplazoDeQr;
    private final CobroMapper cobroMapper;

    @Transactional
    public CobroResponse registrar(Long inscripcionId, CobroRequest request, Actor actor) {
        InscripcionClase inscripcion = visible(inscripcionId, actor);
        ejecucion.asegurarMedioPermitido(request.medioPago(), actor);
        if (!Boolean.TRUE.equals(inscripcion.getActiva())) {
            throw new ReglaDeNegocioException(CodigoError.INSCRIPCION_NO_COBRABLE, "La inscripción está cancelada: no admite cobros");
        }
        Optional<Cobro> qrEnCurso = qrEnCurso(inscripcion);
        if (qrEnCurso.isPresent() && request.medioPago() == MedioPago.QR) {
            return cobroMapper.aRespuesta(qrEnCurso.get());
        }
        qrEnCurso.ifPresent(qr -> reemplazoDeQr.reemplazar(qr, actor));
        reemplazoDeQr.reemplazarRechazados(cobroRepository.deInscripcionActividad(inscripcionId, List.of(EstadoCobro.RECHAZADO),
                PagosDeInscripcionImpl.desdeDe(inscripcion)), actor);
        boolean esSenia = inscripcion.getEstado() == EstadoInscripcion.PENDIENTE_PAGO;
        Cobro cobro = ejecucion.nuevo(request.medioPago(), esSenia ? ConceptoCobro.SEÑA : ConceptoCobro.SALDO, actor);
        cobro.setInscripcionActividad(inscripcion);
        return ejecucion.asentar(cobro, montoBase(inscripcion, esSenia), inscripcion.getSocio().getId(), request,
                () -> confirmacion.confirmar(inscripcionId));
    }

    private BigDecimal montoBase(InscripcionClase inscripcion, boolean esSenia) {
        PagosDeInscripcion.Resumen resumen = pagos.resumen(inscripcion);
        if (esSenia) {
            return resumen.montoSenia();
        }
        BigDecimal total = inscripcion.getMontoTotal() != null ? inscripcion.getMontoTotal() : BigDecimal.ZERO;
        BigDecimal saldo = total.subtract(resumen.baseCubierta());
        if (saldo.signum() <= 0) {
            throw new ConflictoException(CodigoError.COBRO_YA_REGISTRADO, "La inscripción no tiene saldo pendiente");
        }
        return saldo;
    }

    private Optional<Cobro> qrEnCurso(InscripcionClase inscripcion) {
        List<Cobro> pendientes = cobroRepository.deInscripcionActividad(inscripcion.getId(), List.of(EstadoCobro.PENDIENTE),
                PagosDeInscripcionImpl.desdeDe(inscripcion));
        return pendientes.stream().filter(cobro -> cobro.getMedioPago() == MedioPago.QR).findFirst();
    }

    private InscripcionClase visible(Long id, Actor actor) {
        InscripcionClase inscripcion = inscripcionRepository.conActividad(id).orElseThrow(this::noEncontrada);
        if (!AccesoAReservas.esPersonal(actor) && !inscripcion.getSocio().getId().equals(actor.id())) {
            throw noEncontrada();
        }
        return inscripcion;
    }

    private RecursoNoEncontradoException noEncontrada() {
        return new RecursoNoEncontradoException(CodigoError.INSCRIPCION_NO_ENCONTRADA, "Inscripción no encontrada");
    }
}
