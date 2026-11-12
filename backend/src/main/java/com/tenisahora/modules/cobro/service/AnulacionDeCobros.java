package com.tenisahora.modules.cobro.service;

import com.tenisahora.modules.actividad.service.ConfirmacionDeInscripcion;
import com.tenisahora.modules.torneo.service.ConfirmacionDeInscripcionTorneo;
import com.tenisahora.modules.cobro.dto.CobroResponse;
import com.tenisahora.modules.cobro.entity.Cobro;
import com.tenisahora.modules.cobro.entity.ConceptoCobro;
import com.tenisahora.modules.cobro.entity.EstadoCobro;
import com.tenisahora.modules.cobro.mapper.CobroMapper;
import com.tenisahora.modules.cobro.repository.CobroRepository;
import com.tenisahora.modules.reserva.service.ConfirmacionDeReserva;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** CU55: anular un cobro. Lo acreditado en Mercado Pago se devuelve desde su panel: acá queda registrado, no se mueve plata. */
@Service
@RequiredArgsConstructor
public class AnulacionDeCobros {

    private final CobroPorId cobroPorId;
    private final RegistroDeAnulacion registroDeAnulacion;
    private final CierreDeOrden cierreDeOrden;
    private final ConfirmacionDeReserva confirmacionDeReserva;
    private final CobroMapper cobroMapper;
    private final ConfirmacionDeInscripcion confirmacionDeInscripcion;
    private final ConfirmacionDeInscripcionTorneo confirmacionDeTorneo;
    private final CobroRepository cobroRepository;

    @Transactional
    public CobroResponse anular(Long id, String motivo, Actor actor) {
        Cobro cobro = cobroPorId.buscarParaModificar(id);
        boolean estabaPagado = cobro.getEstado() == EstadoCobro.PAGADO;
        if (!estabaPagado && cobro.getEstado() != EstadoCobro.PENDIENTE) {
            throw new ReglaDeNegocioException(CodigoError.COBRO_NO_ANULABLE, "El cobro está " + cobro.getEstado() + ": ya no se puede anular");
        }
        if (estabaPagado && cobro.getConcepto() == ConceptoCobro.SEÑA && tieneSaldoPagado(cobro)) {
            throw new ReglaDeNegocioException(CodigoError.COBRO_NO_ANULABLE, "Hay un saldo pagado: primero hay que anular ese cobro");
        }
        registroDeAnulacion.aplicar(cobro, motivo, actor.id());
        if (cobro.getIdPagoExterno() != null) {
            cierreDeOrden.cerrar(cobro.getIdPagoExterno());
        }
        if (estabaPagado) {
            revertirConfirmacion(cobro);
        }
        return cobroMapper.aRespuesta(cobro);
    }

    /** Sin la seña la reserva vuelve a pendiente: con un saldo pagado quedaría sin poder cobrarse ni confirmarse. */
    private boolean tieneSaldoPagado(Cobro cobro) {
        List<Cobro> pagados = List.of();
        if (cobro.getReserva() != null) {
            pagados = cobroRepository.findByReservaIdAndEstado(cobro.getReserva().getId(), EstadoCobro.PAGADO);
        } else if (cobro.getInscripcionActividad() != null) {
            pagados = cobroRepository.deInscripcionActividad(cobro.getInscripcionActividad().getId(), List.of(EstadoCobro.PAGADO),
                    PagosDeInscripcionImpl.desdeDe(cobro.getInscripcionActividad()));
        }
        return pagados.stream().anyMatch(pagado -> pagado.getConcepto() == ConceptoCobro.SALDO);
    }

    private void revertirConfirmacion(Cobro cobro) {
        if (cobro.getInscripcionTorneo() != null) {
            confirmacionDeTorneo.volverAPendiente(cobro.getInscripcionTorneo().getId());
        } else if (cobro.getConcepto() != ConceptoCobro.SEÑA) {
            return;
        } else if (cobro.getReserva() != null) {
            confirmacionDeReserva.volverAPendiente(cobro.getReserva().getId());
        } else if (cobro.getInscripcionActividad() != null) {
            confirmacionDeInscripcion.volverAPendiente(cobro.getInscripcionActividad().getId());
        }
    }
}
