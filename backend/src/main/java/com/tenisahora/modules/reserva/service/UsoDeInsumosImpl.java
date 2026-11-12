package com.tenisahora.modules.reserva.service;

import com.tenisahora.modules.insumo.service.UsoDeInsumos;
import com.tenisahora.modules.reserva.entity.EstadoReserva;
import com.tenisahora.modules.reserva.repository.ReservaInsumoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Lo que el módulo de insumos pregunta de las reservas. Una reserva pendiente de pago o en curso también retiene sus insumos. */
@Service
@RequiredArgsConstructor
public class UsoDeInsumosImpl implements UsoDeInsumos {

    private static final List<EstadoReserva> ESTADOS_VIGENTES = EstadoReserva.queRetienenHorario();

    private final ReservaInsumoRepository reservaInsumoRepository;

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Integer> enUso(Collection<Long> insumoIds) {
        Map<Long, Integer> enUso = new HashMap<>();
        if (insumoIds.isEmpty()) {
            return enUso;
        }
        for (Object[] fila : reservaInsumoRepository.sumaEnUso(insumoIds)) {
            enUso.put((Long) fila[0], ((Number) fila[1]).intValue());
        }
        return enUso;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> reservasVigentesCon(Long insumoId, LocalDate desde) {
        return reservaInsumoRepository.reservasConInsumo(insumoId, ESTADOS_VIGENTES, desde);
    }
}
