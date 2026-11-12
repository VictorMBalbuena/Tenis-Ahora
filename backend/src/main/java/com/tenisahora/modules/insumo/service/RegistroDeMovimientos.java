package com.tenisahora.modules.insumo.service;

import com.tenisahora.modules.insumo.entity.Insumo;
import com.tenisahora.modules.insumo.entity.MovimientoStock;
import com.tenisahora.modules.insumo.entity.TipoMovimiento;
import com.tenisahora.modules.insumo.repository.MovimientoStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

/** Deja asentado cada cambio de stock. Se usa dentro de la transacción que cambia el stock, así nunca hay uno sin el otro. */
@Component
@RequiredArgsConstructor
public class RegistroDeMovimientos {

    private final MovimientoStockRepository movimientoRepository;
    private final Clock clock;

    public void registrar(Insumo insumo, TipoMovimiento tipo, int variacion, int stockResultante, String motivo, Long personaId) {
        MovimientoStock movimiento = new MovimientoStock();
        movimiento.setInsumo(insumo);
        movimiento.setTipo(tipo);
        movimiento.setVariacion(variacion);
        movimiento.setStockResultante(stockResultante);
        movimiento.setMotivo(motivo == null || motivo.isBlank() ? null : motivo.strip());
        movimiento.setFecha(LocalDateTime.now(clock));
        movimiento.setPersonaId(personaId);
        movimientoRepository.save(movimiento);
    }
}
