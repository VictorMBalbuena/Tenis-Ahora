package com.tenisahora.modules.cobro.service;

import com.tenisahora.infrastructure.mercadopago.EstadoDeOrden;
import com.tenisahora.infrastructure.mercadopago.PasarelaDePagos;
import com.tenisahora.infrastructure.mercadopago.ResultadoDePago;
import com.tenisahora.modules.actividad.service.ConfirmacionDeInscripcion;
import com.tenisahora.modules.torneo.service.ConfirmacionDeInscripcionTorneo;
import com.tenisahora.modules.cobro.entity.Cobro;
import com.tenisahora.modules.cobro.entity.EstadoCobro;
import com.tenisahora.modules.cobro.repository.CobroRepository;
import com.tenisahora.modules.reserva.service.ConfirmacionDeReserva;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AcreditacionDePagosImpl implements AcreditacionDePagos {

    private final CobroRepository cobroRepository;
    private final PasarelaDePagos pasarela;
    private final EmisionDeRecibos emisionDeRecibos;
    private final ConfirmacionDeReserva confirmacionDeReserva;
    private final CierreDeOrden cierreDeOrden;
    private final ConfirmacionDeInscripcion confirmacionDeInscripcion;
    private final ConfirmacionDeInscripcionTorneo confirmacionDeTorneo;

    @Override
    @Transactional
    public void acreditar(String idExterno) {
        Cobro cobro = cobroRepository.buscarPorPagoExternoParaModificar(idExterno).orElse(null);
        if (cobro == null) {
            log.warn("Aviso de Mercado Pago para un pago sin cobro asociado: {}", idExterno);
            return;
        }
        if (!cobro.getEstado().admiteAviso()) {
            log.info("Cobro {} ya estaba {}: aviso repetido ignorado", cobro.getId(), cobro.getEstado());
            return;
        }
        aplicar(cobro, pasarela.consultar(idExterno));
    }

    /** Se confía en lo que responde la pasarela, no en lo que trajo el aviso (A08). */
    private void aplicar(Cobro cobro, EstadoDeOrden estado) {
        if (estado.resultado() == ResultadoDePago.ACREDITADO) {
            acreditarPago(cobro, estado);
        } else if (cobro.getEstado() == EstadoCobro.VENCIDO) {
            log.debug("Cobro {} vencido: solo un pago acreditado le cambia el estado", cobro.getId());
        } else if (estado.resultado() == ResultadoDePago.RECHAZADO) {
            cobroRepository.save(setEstado(cobro, EstadoCobro.RECHAZADO));
        } else if (estado.resultado() == ResultadoDePago.VENCIDO) {
            cobroRepository.save(setEstado(cobro, EstadoCobro.VENCIDO));
        }
        // PENDIENTE: todavía no hay nada que registrar; sigue esperando el próximo aviso o la conciliación.
    }

    private void acreditarPago(Cobro cobro, EstadoDeOrden estado) {
        if (cobro.getMontoFinal().compareTo(estado.montoPagado()) != 0) {
            log.error("Cobro {}: Mercado Pago acreditó {} pero el cobro era por {}; no se confirma la reserva",
                    cobro.getId(), estado.montoPagado(), cobro.getMontoFinal());
            return;
        }
        cobro.setNumeroOperacion(estado.idPago());
        cobroRepository.save(setEstado(cobro, EstadoCobro.PAGADO));
        emisionDeRecibos.emitir(cobro);
        if (!confirmarOrigen(cobro)) {
            log.error("Cobro {}: se acreditó con la reserva o inscripción ya cancelada; hay que devolver {}",
                    cobro.getId(), cobro.getMontoFinal());
        }
        cierreDeOrden.cerrar(cobro.getIdPagoExterno());
        log.info("Cobro {} acreditado por Mercado Pago", cobro.getId());
    }

    private boolean confirmarOrigen(Cobro cobro) {
        if (cobro.getReserva() != null) {
            return confirmacionDeReserva.confirmarPagoAcreditado(cobro.getReserva().getId(), cobro.getMontoFinal());
        }
        if (cobro.getInscripcionActividad() != null) {
            return confirmacionDeInscripcion.confirmarPagoAcreditado(cobro.getInscripcionActividad().getId(), cobro.getMontoFinal());
        }
        return confirmacionDeTorneo.confirmarPagoAcreditado(cobro.getInscripcionTorneo().getId());
    }

    private Cobro setEstado(Cobro cobro, EstadoCobro estado) {
        cobro.setEstado(estado);
        return cobro;
    }
}
