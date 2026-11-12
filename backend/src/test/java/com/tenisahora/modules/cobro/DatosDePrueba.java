package com.tenisahora.modules.cobro;

import com.tenisahora.infrastructure.mercadopago.MercadoPagoProperties;
import com.tenisahora.modules.cobro.entity.Cobro;
import com.tenisahora.modules.cobro.entity.ConceptoCobro;
import com.tenisahora.modules.cobro.entity.EstadoCobro;
import com.tenisahora.modules.cobro.entity.MedioPago;
import com.tenisahora.modules.cobro.service.CobrosProperties;
import com.tenisahora.modules.reserva.entity.EstadoReserva;
import com.tenisahora.modules.reserva.entity.Reserva;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** Datos comunes de los tests de cobros. "Ahora" es el lunes 19 de octubre de 2026 a las 12:00. */
public final class DatosDePrueba {

    public static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-19T12:00:00Z"), ZoneOffset.UTC);
    public static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 19, 12, 0);
    public static final CobrosProperties PROPIEDADES = new CobrosProperties(12, 60000, 300000);
    public static final MercadoPagoProperties MP_PROPIEDADES = new MercadoPagoProperties("token-de-prueba", "secreto-de-prueba",
            "https://api.mercadopago.com", 10, false);

    private DatosDePrueba() {
    }

    public static Reserva reserva(Long id, EstadoReserva estado, String montoTotal) {
        Reserva reserva = new Reserva();
        reserva.setId(id);
        reserva.setEstado(estado);
        reserva.setMontoTotal(new BigDecimal(montoTotal));
        return reserva;
    }

    public static Cobro cobro(Long id, EstadoCobro estado, MedioPago medio, String montoFinal) {
        Cobro cobro = new Cobro();
        cobro.setId(id);
        cobro.setCodigo(String.format("C-%06d", id));
        cobro.setReserva(reserva(100L + id, EstadoReserva.PENDIENTE_PAGO, "16000.00"));
        cobro.setConcepto(ConceptoCobro.SEÑA);
        cobro.setEstado(estado);
        cobro.setMedioPago(medio);
        cobro.setMontoBase(new BigDecimal(montoFinal));
        cobro.setMontoFinal(new BigDecimal(montoFinal));
        cobro.setFecha(AHORA);
        return cobro;
    }
}
