package com.tenisahora.modules.cobro.service;

import com.tenisahora.modules.actividad.entity.EstadoInscripcion;
import com.tenisahora.modules.torneo.entity.EstadoInscripcionTorneo;
import com.tenisahora.modules.cobro.dto.CobroResponse;
import com.tenisahora.modules.cobro.dto.QrResponse;
import com.tenisahora.modules.cobro.entity.Cobro;
import com.tenisahora.modules.cobro.entity.MedioPago;
import com.tenisahora.modules.cobro.mapper.CobroMapper;
import com.tenisahora.modules.cobro.repository.CobroRepository;
import com.tenisahora.modules.cobro.service.pago.DatosDeCobro;
import com.tenisahora.modules.cobro.service.pago.FormasDeCobro;
import com.tenisahora.modules.cobro.service.pago.ResultadoDeCobro;
import com.tenisahora.modules.reserva.entity.EstadoReserva;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QrDeCobroImpl implements QrDeCobro {

    private final CobroPorId cobroPorId;
    private final AccesoACobros accesoACobros;
    private final FormasDeCobro formasDeCobro;
    private final CobroRepository cobroRepository;
    private final CobroMapper cobroMapper;
    private final AcreditacionDePagos acreditacionDePagos;
    private final CierreDeOrden cierreDeOrden;

    @Override
    @Transactional
    public QrResponse generar(Long cobroId, Actor actor) {
        Cobro cobro = accesoACobros.visible(cobroPorId.buscarParaModificar(cobroId), actor);
        if (cobro.getMedioPago() != MedioPago.QR || !cobro.getEstado().puedeGenerarQr() || origenCancelado(cobro)) {
            throw new ReglaDeNegocioException(CodigoError.COBRO_NO_ADMITE_QR, "El cobro no admite generar un QR nuevo");
        }
        if (cobro.getIdPagoExterno() != null) {
            cierreDeOrden.cerrar(cobro.getIdPagoExterno());
        }
        DatosDeCobro datos = new DatosDeCobro(null, null, true, cobro.getRegistradoPorId());
        ResultadoDeCobro resultado = formasDeCobro.de(MedioPago.QR).cobrar(cobro, datos);
        cobro.setEstado(resultado.estado());
        cobro.setIdPagoExterno(resultado.idPagoExterno());
        cobro.setFechaExpiracionQr(resultado.fechaExpiracionQr());
        cobroRepository.saveAndFlush(cobro);
        return cobroMapper.aQrResponse(cobro, resultado.qrData());
    }

    private boolean origenCancelado(Cobro cobro) {
        if (cobro.getReserva() != null) {
            return cobro.getReserva().getEstado() == EstadoReserva.CANCELADA;
        }
        if (cobro.getInscripcionActividad() != null) {
            return cobro.getInscripcionActividad().getEstado() == EstadoInscripcion.CANCELADA;
        }
        return cobro.getInscripcionTorneo() != null && cobro.getInscripcionTorneo().getEstado() == EstadoInscripcionTorneo.CANCELADA;
    }

    @Override
    @Transactional
    public CobroResponse verificar(Long cobroId, Actor actor) {
        Cobro cobro = accesoACobros.visible(cobroPorId.buscar(cobroId), actor);
        if (cobro.getMedioPago() == MedioPago.QR && cobro.getIdPagoExterno() != null && cobro.getEstado().admiteAviso()) {
            acreditacionDePagos.acreditar(cobro.getIdPagoExterno());
        }
        return cobroMapper.aRespuesta(cobro);
    }
}
