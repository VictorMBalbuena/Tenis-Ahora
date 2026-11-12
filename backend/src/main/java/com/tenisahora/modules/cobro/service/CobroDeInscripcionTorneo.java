package com.tenisahora.modules.cobro.service;

import com.tenisahora.modules.cobro.dto.CobroRequest;
import com.tenisahora.modules.cobro.dto.CobroResponse;
import com.tenisahora.modules.cobro.entity.Cobro;
import com.tenisahora.modules.cobro.entity.ConceptoCobro;
import com.tenisahora.modules.cobro.entity.EstadoCobro;
import com.tenisahora.modules.cobro.entity.MedioPago;
import com.tenisahora.modules.cobro.mapper.CobroMapper;
import com.tenisahora.modules.cobro.repository.CobroRepository;
import com.tenisahora.modules.reserva.service.AccesoAReservas;
import com.tenisahora.modules.torneo.entity.EstadoInscripcionTorneo;
import com.tenisahora.modules.torneo.entity.Inscripcion;
import com.tenisahora.modules.torneo.repository.InscripcionRepository;
import com.tenisahora.modules.torneo.service.ConfirmacionDeInscripcionTorneo;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** RF52 y CU52: el arancel del torneo se cobra completo y recién ahí queda firme la inscripción. */
@Service
@RequiredArgsConstructor
public class CobroDeInscripcionTorneo {

    private final InscripcionRepository inscripcionRepository;
    private final CobroRepository cobroRepository;
    private final ConfirmacionDeInscripcionTorneo confirmacion;
    private final EjecucionDeCobro ejecucion;
    private final ReemplazoDeQr reemplazoDeQr;
    private final CobroMapper cobroMapper;

    @Transactional
    public CobroResponse registrar(Long inscripcionId, CobroRequest request, Actor actor) {
        Inscripcion inscripcion = visible(inscripcionId, actor);
        ejecucion.asegurarMedioPermitido(request.medioPago(), actor);
        asegurarArancelPendiente(inscripcion);
        Optional<Cobro> qrEnCurso = cobroRepository.deInscripcionTorneo(inscripcionId, List.of(EstadoCobro.PENDIENTE)).stream()
                .filter(cobro -> cobro.getMedioPago() == MedioPago.QR).findFirst();
        if (qrEnCurso.isPresent() && request.medioPago() == MedioPago.QR) {
            return cobroMapper.aRespuesta(qrEnCurso.get());
        }
        qrEnCurso.ifPresent(qr -> reemplazoDeQr.reemplazar(qr, actor));
        reemplazoDeQr.reemplazarRechazados(cobroRepository.deInscripcionTorneo(inscripcionId, List.of(EstadoCobro.RECHAZADO)), actor);
        asegurarArancelPendiente(inscripcion);
        Cobro cobro = ejecucion.nuevo(request.medioPago(), ConceptoCobro.INSCRIPCION, actor);
        cobro.setInscripcionTorneo(inscripcion);
        cobro.setTorneo(inscripcion.getTorneo());
        return ejecucion.asentar(cobro, inscripcion.getMontoTotal(), inscripcion.getJugador1().getId(), request,
                () -> confirmacion.confirmar(inscripcionId));
    }

    private void asegurarArancelPendiente(Inscripcion inscripcion) {
        if (inscripcion.getEstado() != EstadoInscripcionTorneo.PENDIENTE_PAGO) {
            throw new ReglaDeNegocioException(CodigoError.INSCRIPCION_NO_COBRABLE, "La inscripción no tiene un arancel pendiente de pago");
        }
    }

    private Inscripcion visible(Long id, Actor actor) {
        Inscripcion inscripcion = inscripcionRepository.findById(id).orElseThrow(this::noEncontrada);
        if (!AccesoAReservas.esPersonal(actor) && !inscripcion.participa(actor.id())) {
            throw noEncontrada();
        }
        return inscripcion;
    }

    private RecursoNoEncontradoException noEncontrada() {
        return new RecursoNoEncontradoException(CodigoError.INSCRIPCION_NO_ENCONTRADA, "Inscripción no encontrada");
    }
}
