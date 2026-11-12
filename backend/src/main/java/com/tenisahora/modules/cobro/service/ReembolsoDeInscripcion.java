package com.tenisahora.modules.cobro.service;

import com.tenisahora.modules.actividad.service.InscripcionCancelada;
import com.tenisahora.modules.cobro.entity.Cobro;
import com.tenisahora.modules.cobro.entity.ConceptoCobro;
import com.tenisahora.modules.cobro.entity.EstadoCobro;
import com.tenisahora.modules.cobro.mapper.CobroMapper;
import com.tenisahora.modules.cobro.repository.CobroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Al dar de baja una inscripción con reembolso: anula lo cobrado si se devuelve todo, o deja asentada la devolución parcial. */
@Component
@RequiredArgsConstructor
public class ReembolsoDeInscripcion {

    private static final String MOTIVO = "Reembolso por baja de la inscripción";
    private static final LocalDateTime SIEMPRE = LocalDateTime.of(2000, 1, 1, 0, 0);

    private final CobroRepository cobroRepository;
    private final RegistroDeAnulacion registroDeAnulacion;

    @EventListener
    public void alCancelar(InscripcionCancelada evento) {
        if (evento.montoReembolso() == null || evento.montoReembolso().signum() <= 0) {
            return;
        }
        List<Cobro> pagados = cobroRepository.deInscripcionActividad(evento.inscripcionId(), List.of(EstadoCobro.PAGADO), SIEMPRE).stream()
                .filter(cobro -> cobro.getConcepto() != ConceptoCobro.REEMBOLSO && cobro.getFechaAnulacion() == null).toList();
        BigDecimal cobrado = pagados.stream().map(Cobro::getMontoFinal).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (evento.montoReembolso().compareTo(cobrado) >= 0) {
            pagados.forEach(cobro -> registroDeAnulacion.aplicar(cobro, MOTIVO, evento.responsableId()));
        } else {
            registrarParcial(pagados.get(0), evento);
        }
    }

    private void registrarParcial(Cobro original, InscripcionCancelada evento) {
        Cobro reembolso = new Cobro();
        reembolso.setInscripcionActividad(original.getInscripcionActividad());
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
