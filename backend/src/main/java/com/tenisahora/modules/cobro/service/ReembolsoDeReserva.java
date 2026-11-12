package com.tenisahora.modules.cobro.service;

import com.tenisahora.modules.cobro.entity.Cobro;
import com.tenisahora.modules.cobro.entity.ConceptoCobro;
import com.tenisahora.modules.cobro.entity.EstadoCobro;
import com.tenisahora.modules.cobro.mapper.CobroMapper;
import com.tenisahora.modules.cobro.repository.CobroRepository;
import com.tenisahora.modules.reserva.service.ReservaCancelada;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/** Registra la devolución de una reserva cancelada a tiempo: si se devuelve todo se anulan sus cobros; si es parcial, queda un movimiento de reembolso. */
@Component
@RequiredArgsConstructor
public class ReembolsoDeReserva {

    private static final String MOTIVO = "Reembolso por cancelación de la reserva";

    private final CobroRepository cobroRepository;
    private final RegistroDeAnulacion registroDeAnulacion;

    @EventListener
    public void alCancelar(ReservaCancelada evento) {
        if (evento.montoReembolso() == null || evento.montoReembolso().signum() <= 0) {
            return;
        }
        List<Cobro> pagados = cobroRepository.findByReservaIdAndEstado(evento.reservaId(), EstadoCobro.PAGADO);
        BigDecimal cobrado = pagados.stream().map(Cobro::getMontoFinal).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (evento.montoReembolso().compareTo(cobrado) >= 0) {
            pagados.forEach(cobro -> registroDeAnulacion.aplicar(cobro, MOTIVO, evento.responsableId()));
        } else {
            registrarParcial(pagados.get(0), evento);
        }
    }

    private void registrarParcial(Cobro original, ReservaCancelada evento) {
        Cobro reembolso = new Cobro();
        reembolso.setReserva(original.getReserva());
        reembolso.setConcepto(ConceptoCobro.REEMBOLSO);
        reembolso.setEstado(EstadoCobro.PAGADO);
        reembolso.setMedioPago(original.getMedioPago());
        reembolso.setMontoBase(evento.montoReembolso());
        reembolso.setMontoFinal(evento.montoReembolso());
        reembolso.setRegistradoPorId(evento.responsableId());
        cobroRepository.saveAndFlush(reembolso);
        reembolso.setCodigo(CobroMapper.codigoDe(reembolso.getId()));
    }
}
