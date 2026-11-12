package com.tenisahora.modules.cobro.service.pago;

import com.tenisahora.infrastructure.mercadopago.MercadoPagoProperties;
import com.tenisahora.infrastructure.mercadopago.OrdenQr;
import com.tenisahora.infrastructure.mercadopago.PasarelaDePagos;
import com.tenisahora.infrastructure.mercadopago.SolicitudDeOrden;
import com.tenisahora.modules.cobro.entity.Cobro;
import com.tenisahora.modules.cobro.entity.MedioPago;
import com.tenisahora.modules.reserva.entity.EstadoReserva;
import com.tenisahora.modules.reserva.service.ReservasProperties;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * QR de Mercado Pago: pide la orden a la pasarela y deja el cobro {@code PENDIENTE} hasta que llegue el aviso
 * (CU88). Si la pasarela no contesta, {@link com.tenisahora.infrastructure.mercadopago.PasarelaNoDisponibleException}
 * sube tal cual: la propia pasarela ya la pensó para no filtrar tipos del SDK.
 */
@Component
@RequiredArgsConstructor
public class PagoMercadoPago implements MedioDePago {

    private static final Duration MINIMO_PARA_PAGAR = Duration.ofMinutes(1);

    private final PasarelaDePagos pasarela;
    private final MercadoPagoProperties propiedades;
    private final ReservasProperties propiedadesDeReservas;
    private final Clock clock;

    @Override
    public MedioPago medio() {
        return MedioPago.QR;
    }

    @Override
    public ResultadoDeCobro cobrar(Cobro cobro, DatosDeCobro datos) {
        Duration vigencia = vigenciaPara(cobro);
        SolicitudDeOrden solicitud = new SolicitudDeOrden(cobro.getMontoFinal(), cobro.getCodigo(), vigencia, claveDeIdempotencia(cobro));
        OrdenQr orden = pasarela.crearOrdenQr(solicitud);
        LocalDateTime venceEn = LocalDateTime.ofInstant(orden.venceEn(), clock.getZone());
        return ResultadoDeCobro.pendienteConQr(orden.idExterno(), orden.qrData(), venceEn);
    }

    /** Cambia con cada QR que reemplaza al anterior: con la misma clave, Mercado Pago devolvería la orden que se acaba de cerrar. */
    private String claveDeIdempotencia(Cobro cobro) {
        String clave = "cobro-" + cobro.getId();
        return cobro.getIdPagoExterno() == null ? clave : clave + "-" + Integer.toHexString(cobro.getIdPagoExterno().hashCode());
    }

    /** El QR no dura más que la reserva pendiente: así nadie paga un turno que ya se liberó ni estira la espera generando QR nuevos. */
    private Duration vigenciaPara(Cobro cobro) {
        Duration vigencia = Duration.ofMinutes(propiedades.minutosVigenciaQr());
        if (cobro.getReserva() == null || cobro.getReserva().getEstado() != EstadoReserva.PENDIENTE_PAGO) {
            return vigencia;
        }
        LocalDateTime venceLaReserva = cobro.getReserva().inicioDelPlazoDePago().plusMinutes(propiedadesDeReservas.minutosParaVencerPendientes());
        Duration restante = Duration.between(LocalDateTime.now(clock), venceLaReserva);
        if (restante.compareTo(MINIMO_PARA_PAGAR) < 0) {
            throw new ReglaDeNegocioException(CodigoError.RESERVA_NO_COBRABLE, "Se terminó el plazo para pagar la seña de esta reserva");
        }
        return vigencia.compareTo(restante) < 0 ? vigencia : restante;
    }
}
