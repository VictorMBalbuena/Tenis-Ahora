package com.tenisahora.infrastructure.mercadopago;

import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.core.MPRequestOptions;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.net.MPSearchRequest;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Adaptador con el SDK oficial de Mercado Pago (Adapter). Envuelve sus tipos y excepciones: nada de
 * {@code com.mercadopago.*} se filtra fuera de esta clase (los módulos hablan con {@link PasarelaDePagos}).
 *
 * <p>El SDK de Orders (v1/orders) modela pagos "online" (Checkout Bricks) y el QR in-store exige una tienda y un
 * punto de venta ya dados de alta en la cuenta, algo que no tiene sentido pedirle a esta demo. Por eso el QR se
 * arma con una {@link PreferenceClient} de Checkout Pro: la preferencia da un {@code initPoint} (una URL), y esa
 * URL es el contenido que el frontend dibuja como código QR — el mismo mecanismo que usa cualquier comercio chico
 * sin terminal propia. Es íntegramente SDK oficial; no hay ninguna llamada HTTP armada a mano.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MercadoPagoClient implements PasarelaDePagos {

    private final MercadoPagoProperties propiedades;

    @Override
    public OrdenQr crearOrdenQr(SolicitudDeOrden solicitud) {
        asegurarConfigurada();
        OffsetDateTime hasta = OffsetDateTime.now().plus(solicitud.vigencia());
        PreferenceRequest request = PreferenceRequest.builder()
                .items(List.of(itemDe(solicitud)))
                .externalReference(solicitud.referenciaExterna())
                .expires(true)
                .expirationDateTo(hasta)
                .build();
        try {
            Preference preference = new PreferenceClient().create(request, opciones(solicitud.claveDeIdempotencia()));
            String link = propiedades.sandbox() ? preference.getSandboxInitPoint() : preference.getInitPoint();
            return new OrdenQr(preference.getId(), link, hasta.toInstant());
        } catch (MPException | MPApiException ex) {
            throw new PasarelaNoDisponibleException("No se pudo crear la orden de pago en Mercado Pago", ex);
        }
    }

    @Override
    public EstadoDeOrden consultar(String idExterno) {
        asegurarConfigurada();
        try {
            Preference preference = new PreferenceClient().get(idExterno, opciones(null));
            return estadoDesdeLosPagos(idExterno, preference);
        } catch (MPException | MPApiException ex) {
            throw new PasarelaNoDisponibleException("No se pudo consultar la orden en Mercado Pago", ex);
        }
    }

    @Override
    public void cerrarOrden(String idExterno) {
        asegurarConfigurada();
        PreferenceRequest request = PreferenceRequest.builder().expires(true).expirationDateTo(OffsetDateTime.now()).build();
        try {
            new PreferenceClient().update(idExterno, request, opciones(null));
        } catch (MPException | MPApiException ex) {
            throw new PasarelaNoDisponibleException("No se pudo cerrar la orden en Mercado Pago", ex);
        }
    }

    private static final String ESTADO_APROBADO = "approved";
    private static final int MAXIMO_DE_PAGOS = 50;

    /** La preferencia en sí no tiene estado de pago: hay que preguntarle a los pagos asociados por su referencia externa. */
    private EstadoDeOrden estadoDesdeLosPagos(String idExterno, Preference preference) throws MPException, MPApiException {
        MPSearchRequest busqueda = MPSearchRequest.builder().limit(MAXIMO_DE_PAGOS).offset(0)
                .filters(Map.of("external_reference", preference.getExternalReference())).build();
        List<Payment> pagos = new PaymentClient().search(busqueda, opciones(null)).getResults();
        return mejorPago(pagos, preference.getDateCreated())
                .map(pago -> new EstadoDeOrden(idExterno, resultadoDe(pago.getStatus()), pago.getTransactionAmount(), String.valueOf(pago.getId())))
                .orElse(new EstadoDeOrden(idExterno, ResultadoDePago.PENDIENTE, BigDecimal.ZERO, null));
    }

    /**
     * Una misma referencia externa puede tener más de un pago: la preferencia queda abierta hasta vencer, así que
     * un reintento tras una tarjeta rechazada deja un segundo {@code Payment} más reciente. Si se eligiera siempre
     * el más nuevo por fecha, un rechazo posterior taparía un pago aprobado anterior y la plata ya cobrada quedaría
     * sin acreditar para siempre (RECHAZADO no se vuelve a revisar). Por eso un pago aprobado gana siempre,
     * sin importar la fecha; entre pagos del mismo estado, se toma el más reciente. Un QR nuevo comparte la referencia
     * con los anteriores: un intento fallido previo a esta orden no cuenta, o el QR nuevo nacería rechazado.
     */
    static Optional<Payment> mejorPago(List<Payment> pagos, OffsetDateTime ordenCreadaEl) {
        return pagos.stream().filter(pago -> cuentaParaLaOrden(pago, ordenCreadaEl)).max(
                Comparator.<Payment>comparingInt(pago -> ESTADO_APROBADO.equals(pago.getStatus()) ? 1 : 0)
                        .thenComparing(Payment::getDateCreated));
    }

    private static boolean cuentaParaLaOrden(Payment pago, OffsetDateTime ordenCreadaEl) {
        return ESTADO_APROBADO.equals(pago.getStatus()) || ordenCreadaEl == null || !pago.getDateCreated().isBefore(ordenCreadaEl);
    }

    @Override
    public String referenciaExternaDelPago(String idDePago) {
        asegurarConfigurada();
        try {
            Payment pago = new PaymentClient().get(Long.valueOf(idDePago), opciones(null));
            return pago.getExternalReference();
        } catch (MPException | MPApiException ex) {
            throw new PasarelaNoDisponibleException("No se pudo consultar el pago en Mercado Pago", ex);
        }
    }

    private ResultadoDePago resultadoDe(String estadoMp) {
        return switch (estadoMp) {
            case "approved" -> ResultadoDePago.ACREDITADO;
            case "rejected" -> ResultadoDePago.RECHAZADO;
            case "cancelled" -> ResultadoDePago.VENCIDO;
            default -> ResultadoDePago.PENDIENTE; // pending, in_process, in_mediation, authorized...
        };
    }

    private PreferenceItemRequest itemDe(SolicitudDeOrden solicitud) {
        return PreferenceItemRequest.builder()
                .title("Cobro de reserva " + solicitud.referenciaExterna())
                .quantity(1)
                .currencyId("ARS")
                .unitPrice(solicitud.monto())
                .build();
    }

    /** El access token viaja por pedido, no por configuración global del SDK: evita estado mutable compartido entre hilos. */
    private MPRequestOptions opciones(String claveDeIdempotencia) {
        MPRequestOptions.MPRequestOptionsBuilder opciones = MPRequestOptions.builder().accessToken(propiedades.accessToken());
        if (claveDeIdempotencia != null) {
            opciones.customHeaders(Map.of("X-Idempotency-Key", claveDeIdempotencia));
        }
        return opciones.build();
    }

    private void asegurarConfigurada() {
        if (!propiedades.estaConfigurada()) {
            log.error("Falta configurar MP_ACCESS_TOKEN: no se puede hablar con Mercado Pago");
            throw new PasarelaNoDisponibleException("La pasarela de pagos no está configurada");
        }
    }
}
