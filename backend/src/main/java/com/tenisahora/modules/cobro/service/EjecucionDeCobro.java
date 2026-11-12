package com.tenisahora.modules.cobro.service;

import com.tenisahora.modules.cobro.dto.CobroRequest;
import com.tenisahora.modules.cobro.dto.CobroResponse;
import com.tenisahora.modules.cobro.entity.Cobro;
import com.tenisahora.modules.cobro.entity.ConceptoCobro;
import com.tenisahora.modules.cobro.entity.EstadoCobro;
import com.tenisahora.modules.cobro.entity.MedioPago;
import com.tenisahora.modules.cobro.mapper.CobroMapper;
import com.tenisahora.modules.cobro.repository.CobroRepository;
import com.tenisahora.modules.cobro.service.pago.DatosDeCobro;
import com.tenisahora.modules.cobro.service.pago.FormasDeCobro;
import com.tenisahora.modules.cobro.service.pago.ResultadoDeCobro;
import com.tenisahora.modules.reserva.service.AccesoAReservas;
import com.tenisahora.shared.dto.Actor;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Los pasos comunes para cobrar una inscripción: montos con descuentos, medio de pago, recibo y aviso de que quedó pago. */
@Component
@RequiredArgsConstructor
public class EjecucionDeCobro {

    private final CobroRepository cobroRepository;
    private final CalculadoraDeCobro calculadoraDeCobro;
    private final FormasDeCobro formasDeCobro;
    private final EmisionDeRecibos emisionDeRecibos;
    private final CobroMapper cobroMapper;

    /** Débito, crédito y efectivo solo los opera el personal: el socio paga con Mercado Pago. */
    public void asegurarMedioPermitido(MedioPago medioPago, Actor actor) {
        if (medioPago != MedioPago.QR && !AccesoAReservas.esPersonal(actor)) {
            throw new AccessDeniedException("Solo el personal del club cobra con débito, crédito o efectivo");
        }
    }

    public Cobro nuevo(MedioPago medioPago, ConceptoCobro concepto, Actor actor) {
        Cobro cobro = new Cobro();
        cobro.setConcepto(concepto);
        cobro.setMedioPago(medioPago);
        cobro.setRegistradoPorId(AccesoAReservas.esPersonal(actor) ? actor.id() : null);
        cobro.setMontoBase(BigDecimal.ZERO);
        cobro.setMontoFinal(BigDecimal.ZERO);
        return cobro;
    }

    public CobroResponse asentar(Cobro cobro, BigDecimal montoBase, Long socioId, CobroRequest request, Runnable alQuedarPago) {
        cobroRepository.saveAndFlush(cobro);
        cobro.setCodigo(CobroMapper.codigoDe(cobro.getId()));
        CalculadoraDeCobro.MontoDeCobro monto = calculadoraDeCobro.aplicarDescuentos(cobro.getId(), montoBase, socioId);
        cobro.setMontoBase(monto.montoBase());
        cobro.setMontoFinal(monto.montoFinal());
        ResultadoDeCobro resultado = cobrar(cobro, request);
        cobroRepository.saveAndFlush(cobro);
        if (cobro.getEstado() == EstadoCobro.PAGADO) {
            emisionDeRecibos.emitir(cobro);
            alQuedarPago.run();
        }
        return cobroMapper.aRespuesta(cobro, resultado.qrData());
    }

    private ResultadoDeCobro cobrar(Cobro cobro, CobroRequest request) {
        DatosDeCobro datos = new DatosDeCobro(request.numeroOperacion(), request.cuotas(), request.aprobado(), cobro.getRegistradoPorId());
        ResultadoDeCobro resultado = formasDeCobro.de(request.medioPago()).cobrar(cobro, datos);
        cobro.setEstado(resultado.estado());
        if (resultado.numeroOperacion() != null) {
            cobro.setNumeroOperacion(resultado.numeroOperacion());
        }
        if (resultado.idPagoExterno() != null) {
            cobro.setIdPagoExterno(resultado.idPagoExterno());
            cobro.setFechaExpiracionQr(resultado.fechaExpiracionQr());
        }
        return resultado;
    }
}
