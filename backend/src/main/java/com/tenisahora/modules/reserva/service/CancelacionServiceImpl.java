package com.tenisahora.modules.reserva.service;

import com.tenisahora.shared.dto.Actor;
import com.tenisahora.modules.reserva.dto.CancelacionResponse;
import com.tenisahora.modules.reserva.dto.ReservaResponse;
import com.tenisahora.modules.reserva.entity.EstadoReserva;
import com.tenisahora.modules.reserva.entity.Reserva;
import com.tenisahora.modules.reserva.repository.ReservaInsumoRepository;
import com.tenisahora.modules.reserva.repository.ReservaRepository;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CancelacionServiceImpl implements CancelacionService {

    private final AccesoAReservas acceso;
    private final PoliticaDeCancelacion politica;
    private final ReservaRepository reservaRepository;
    private final ReservaInsumoRepository reservaInsumoRepository;
    private final ArmadoDeRespuestas respuestas;
    private final ReservasProperties propiedades;
    private final Clock clock;
    private final ApplicationEventPublisher eventos;

    @Override
    @Transactional(readOnly = true)
    public CancelacionResponse previsualizar(Long id, Actor actor) {
        Reserva reserva = acceso.paraCancelar(id, actor, false);
        asegurarCancelable(reserva);
        ResultadoDeCancelacion resultado = politica.evaluar(reserva, LocalDateTime.now(clock));
        return new CancelacionResponse(resultado.aTiempo(), resultado.horasDeAntelacion(), resultado.montoPenalidad(),
                resultado.montoReembolso(), mensajeDe(reserva, resultado));
    }

    @Override
    @Transactional
    public ReservaResponse cancelar(Long id, Actor actor) {
        Reserva reserva = acceso.paraCancelar(id, actor, true);
        asegurarCancelable(reserva);
        LocalDateTime ahora = LocalDateTime.now(clock);
        ResultadoDeCancelacion resultado = politica.evaluar(reserva, ahora);
        marcarCancelada(reserva, resultado, ahora);
        eventos.publishEvent(new ReservaCancelada(id, resultado.montoReembolso(), actor.id()));
        log.info("Reserva {} cancelada por la persona {}", id, actor.id());
        return respuestas.deReserva(reserva);
    }

    @Override
    @Transactional
    public int cancelarVencidas() {
        LocalDateTime ahora = LocalDateTime.now(clock);
        List<Reserva> vencidas = reservaRepository.creadasAntesDe(EstadoReserva.PENDIENTE_PAGO,
                ahora.minusMinutes(propiedades.minutosParaVencerPendientes()));
        ResultadoDeCancelacion sinCargo = new ResultadoDeCancelacion(true, 0, BigDecimal.ZERO, BigDecimal.ZERO);
        vencidas.forEach(reserva -> marcarCancelada(reserva, sinCargo, ahora));
        return vencidas.size();
    }

    private void marcarCancelada(Reserva reserva, ResultadoDeCancelacion resultado, LocalDateTime ahora) {
        reservaInsumoRepository.liberarDeReserva(reserva.getId());
        reserva.setEstado(EstadoReserva.CANCELADA);
        reserva.setFechaCancelacion(ahora);
        reserva.setMontoPenalidad(resultado.montoPenalidad());
        reserva.setMontoReembolso(resultado.montoReembolso());
        reservaRepository.saveAndFlush(reserva);
    }

    private void asegurarCancelable(Reserva reserva) {
        if (!reserva.getEstado().puedeCancelarse()) {
            throw new ReglaDeNegocioException(CodigoError.RESERVA_NO_CANCELABLE,
                    "No es posible cancelar esta reserva: ya está " + reserva.getEstado().etiqueta() + ".");
        }
    }

    private String mensajeDe(Reserva reserva, ResultadoDeCancelacion resultado) {
        if (reserva.getEstado() != EstadoReserva.CONFIRMADA) {
            return "La reserva todavía no tiene la seña paga: se cancela sin cargo ni reembolso";
        }
        return resultado.aTiempo()
                ? "Cancelás con " + resultado.horasDeAntelacion() + " horas de antelación: corresponde el reembolso de " + resultado.montoReembolso()
                : "Cancelás con menos de " + propiedades.plazoCancelacionHoras() + " horas de antelación: se pierde la seña de " + resultado.montoPenalidad();
    }
}
