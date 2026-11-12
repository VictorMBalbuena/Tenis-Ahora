package com.tenisahora.modules.reserva;

import com.tenisahora.modules.cancha.entity.Cancha;
import com.tenisahora.shared.model.Rol;
import com.tenisahora.modules.socio.entity.Socio;
import com.tenisahora.modules.reserva.entity.EstadoReserva;
import com.tenisahora.modules.reserva.entity.Reserva;
import com.tenisahora.modules.reserva.service.ReservasProperties;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;

/** Datos comunes de los tests de reservas. "Hoy" es el lunes 19 de octubre de 2026 a las 12:00. */
public final class DatosDePrueba {

    public static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-19T12:00:00Z"), ZoneOffset.UTC);
    public static final LocalDate HOY = LocalDate.of(2026, 10, 19);
    public static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 19, 12, 0);
    public static final ReservasProperties PROPIEDADES = new ReservasProperties(30, 2, 6, 50, 100, 30, 30, 30, 10, 20);

    private DatosDePrueba() {
    }

    public static Cancha cancha(Long id, String identificador, String precio) {
        Cancha cancha = new Cancha();
        cancha.setId(id);
        cancha.setIdentificador(identificador);
        cancha.setTipoSuperficie("CEMENTO");
        cancha.setCapacidad(4);
        cancha.setPrecioPorHora(new BigDecimal(precio));
        cancha.setActiva(true);
        return cancha;
    }

    public static Socio persona(Long id, boolean activa) {
        Socio persona = new Socio();
        persona.setId(id);
        persona.setNombre("Nombre" + id);
        persona.setApellido("Apellido" + id);
        persona.setDni("3000000" + id);
        persona.setActivo(activa);
        return persona;
    }

    /** Una reserva del día y la hora indicados, de 2 horas, por 16000 en total. */
    public static Reserva reserva(Long id, EstadoReserva estado, LocalDate fecha, LocalTime horaInicio) {
        Reserva reserva = new Reserva();
        reserva.setId(id);
        reserva.setCancha(cancha(1L, "Cancha 1", "8000"));
        reserva.setFecha(fecha);
        reserva.setHoraInicio(horaInicio);
        reserva.setHoraFin(horaInicio.plusHours(2));
        reserva.setEstado(estado);
        reserva.setCantidadJugadores(2);
        reserva.setMontoTotal(new BigDecimal("16000.00"));
        reserva.setFechaCreacion(AHORA.minusMinutes(5));
        return reserva;
    }
}
