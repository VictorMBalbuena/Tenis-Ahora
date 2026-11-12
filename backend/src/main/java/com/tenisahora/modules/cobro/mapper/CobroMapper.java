package com.tenisahora.modules.cobro.mapper;

import com.tenisahora.modules.cobro.dto.CobroResponse;
import com.tenisahora.modules.cobro.dto.QrResponse;
import com.tenisahora.modules.cobro.dto.ReciboResponse;
import com.tenisahora.modules.cobro.entity.Cobro;
import com.tenisahora.modules.cobro.entity.Recibo;
import org.springframework.stereotype.Component;

@Component
public class CobroMapper {

    /** El número de cobro es el id con formato: C-000123. */
    public static String codigoDe(Long id) {
        return String.format("C-%06d", id);
    }

    /** El número de recibo es el id con formato: R-000123. */
    public static String numeroDe(Long id) {
        return String.format("R-%06d", id);
    }

    public CobroResponse aRespuesta(Cobro cobro) {
        return aRespuesta(cobro, null);
    }

    /** {@code qrData} solo viaja recién generado: no se persiste en el cobro. */
    public CobroResponse aRespuesta(Cobro cobro, String qrData) {
        return armar(cobro, qrData, null);
    }

    public CobroResponse aDetalle(Cobro cobro, Recibo recibo) {
        return armar(cobro, null, recibo != null ? aRespuesta(recibo) : null);
    }

    private CobroResponse armar(Cobro cobro, String qrData, ReciboResponse recibo) {
        return new CobroResponse(cobro.getId(), cobro.getCodigo(), cobro.getReserva() != null ? cobro.getReserva().getId() : null, origenDe(cobro), cobro.getConcepto(),
                cobro.getEstado(), cobro.getMedioPago(), cobro.getCuotas(), cobro.getMontoBase(), cobro.getMontoFinal(),
                cobro.getFechaExpiracionQr(), cobro.getNumeroOperacion(), cobro.getFecha(), qrData,
                cobro.getMotivoAnulacion(), cobro.getFechaAnulacion(), cobro.getAnuladoPorId(), recibo);
    }

    private String origenDe(Cobro cobro) {
        if (cobro.getInscripcionActividad() != null) {
            return "Inscripción a " + cobro.getInscripcionActividad().getActividad().getNombre();
        }
        return cobro.getTorneo() != null ? "Inscripción al torneo " + cobro.getTorneo().getNombre() : null;
    }

    public QrResponse aQrResponse(Cobro cobro, String qrData) {
        return new QrResponse(qrData, cobro.getFechaExpiracionQr(), cobro.getEstado());
    }

    public ReciboResponse aRespuesta(Recibo recibo) {
        Cobro cobro = recibo.getCobro();
        return new ReciboResponse(recibo.getId(), recibo.getNumeroRecibo(), recibo.getEstado(), recibo.getMonto(),
                recibo.getConcepto(), recibo.getMedioPago(), recibo.getNumeroOperacion(), recibo.getObservaciones(),
                recibo.getFechaEmision(),
                new ReciboResponse.CobroDeRecibo(cobro.getId(), cobro.getCodigo(), cobro.getMontoFinal(), cobro.getFecha()));
    }
}
