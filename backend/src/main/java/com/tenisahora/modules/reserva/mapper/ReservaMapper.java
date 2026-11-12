package com.tenisahora.modules.reserva.mapper;

import com.tenisahora.modules.socio.entity.Socio;
import com.tenisahora.modules.reserva.dto.EstadoDeAsignacion;
import com.tenisahora.modules.reserva.dto.InsumoAsignadoResponse;
import com.tenisahora.modules.reserva.dto.JugadorResponse;
import com.tenisahora.modules.reserva.dto.ReservaResponse;
import com.tenisahora.modules.reserva.entity.EstadoReserva;
import com.tenisahora.modules.reserva.entity.Reserva;
import com.tenisahora.modules.reserva.entity.ReservaInsumo;
import com.tenisahora.modules.reserva.entity.ReservaJugador;
import com.tenisahora.modules.reserva.service.CalculadoraDeMontos;
import com.tenisahora.modules.reserva.service.PagosDeReserva;
import com.tenisahora.modules.reserva.service.ReservasProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ReservaMapper {

    private final CalculadoraDeMontos calculadora;
    private final ReservasProperties propiedades;
    private final PagosDeReserva pagosDeReserva;

    public ReservaResponse aRespuesta(Reserva reserva, List<ReservaJugador> jugadores, List<ReservaInsumo> insumos) {
        PagosDeReserva.Resumen pagos = pagosDeReserva.resumen(reserva.getId());
        BigDecimal diferencia = reserva.getEstado() == EstadoReserva.CANCELADA ? BigDecimal.ZERO
                : reserva.getMontoTotal().subtract(pagos.baseCubierta());
        return new ReservaResponse(reserva.getId(), numeroDe(reserva.getId()), reserva.getCancha().getId(),
                reserva.getCancha().getIdentificador(), reserva.getFecha(), reserva.getHoraInicio(), reserva.getHoraFin(),
                reserva.getEstado(), reserva.getCantidadJugadores(), jugadores.stream().map(this::aJugador).toList(),
                reserva.getMontoTotal(), reserva.getCodigoDescuento(), reserva.getMontoDescuento(),
                calculadora.senia(reserva.getMontoTotal()), venceEn(reserva),
                reserva.getFechaCancelacion(), reserva.getMontoPenalidad(), reserva.getMontoReembolso(),
                insumos.stream().map(this::aInsumo).toList(), reserva.getInicioReal(), reserva.getFinReal(),
                pagos.cobrado(), diferencia.max(BigDecimal.ZERO), diferencia.negate().max(BigDecimal.ZERO));
    }

    /** El número de reserva es el id con formato: R-000123. */
    public static String numeroDe(Long id) {
        return String.format("R-%06d", id);
    }

    private LocalDateTime venceEn(Reserva reserva) {
        return reserva.getEstado() == EstadoReserva.PENDIENTE_PAGO
                ? reserva.inicioDelPlazoDePago().plusMinutes(propiedades.minutosParaVencerPendientes()) : null;
    }

    private JugadorResponse aJugador(ReservaJugador jugador) {
        Socio socio = jugador.getSocio();
        return socio == null
                ? new JugadorResponse(null, jugador.getNombre(), jugador.getApellido(), jugador.getDocumento(), jugador.getEsTitular(), true)
                : new JugadorResponse(socio.getId(), socio.getNombre(), socio.getApellido(), socio.getDni(), jugador.getEsTitular(), false);
    }

    private InsumoAsignadoResponse aInsumo(ReservaInsumo asignado) {
        EstadoDeAsignacion estado = Boolean.TRUE.equals(asignado.getRepuesto()) ? EstadoDeAsignacion.REPUESTO
                : Boolean.TRUE.equals(asignado.getDescontado()) ? EstadoDeAsignacion.EN_USO : EstadoDeAsignacion.ASIGNADO;
        return new InsumoAsignadoResponse(asignado.getInsumo().getId(), asignado.getInsumo().getNombre(), asignado.getCantidadAsignada(), estado);
    }
}
