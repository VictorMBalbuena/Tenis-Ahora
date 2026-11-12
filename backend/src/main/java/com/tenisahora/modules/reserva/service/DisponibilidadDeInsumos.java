package com.tenisahora.modules.reserva.service;

import com.tenisahora.modules.cancha.service.FranjaHoraria;
import com.tenisahora.modules.insumo.dto.CategoriaInsumoResponse;
import com.tenisahora.modules.insumo.service.ConsultaDeStock;
import com.tenisahora.modules.reserva.dto.InsumoDisponibleResponse;
import com.tenisahora.modules.reserva.entity.EstadoReserva;
import com.tenisahora.modules.reserva.repository.ReservaInsumoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cuántas unidades de cada insumo se pueden prometer a un turno: las libres menos las que ya se prometieron, sin descontar
 * todavía, a otras reservas cuyo horario se pisa. Los turnos de otro día u otro horario no compiten, porque el stock vuelve
 * al terminar cada uno.
 */
@Component
@RequiredArgsConstructor
public class DisponibilidadDeInsumos {

    private static final Long NINGUNA = 0L;

    private final ReservaInsumoRepository reservaInsumoRepository;
    private final ConsultaDeStock consultaDeStock;

    /** Unidades ya prometidas por insumo en el turno. excluirReservaId es la reserva que se está reasignando; null si es nueva. */
    public Map<Long, Integer> comprometidos(Collection<Long> insumoIds, LocalDate fecha, FranjaHoraria franja, Long excluirReservaId) {
        Map<Long, Integer> comprometidos = new HashMap<>();
        if (insumoIds.isEmpty()) {
            return comprometidos;
        }
        for (Object[] fila : reservaInsumoRepository.comprometidos(insumoIds, EstadoReserva.queRetienenHorario(), fecha,
                franja.desde(), franja.hasta(), excluirReservaId == null ? NINGUNA : excluirReservaId)) {
            comprometidos.put((Long) fila[0], ((Number) fila[1]).intValue());
        }
        return comprometidos;
    }

    /** Todos los insumos activos con lo que se puede asignar a un turno (CU13 incluye CU11). */
    public List<InsumoDisponibleResponse> paraElTurno(LocalDate fecha, FranjaHoraria franja) {
        List<ConsultaDeStock.StockLibre> activos = consultaDeStock.activos();
        Map<Long, Integer> comprometidos = comprometidos(activos.stream().map(stock -> stock.insumo().getId()).toList(), fecha, franja, null);
        return activos.stream()
                .map(stock -> new InsumoDisponibleResponse(stock.insumo().getId(), stock.insumo().getNombre(),
                        new CategoriaInsumoResponse(stock.insumo().getCategoria().getId(), stock.insumo().getCategoria().getNombre()),
                        stock.cantidadLibre(), Math.max(0, stock.stockTotal() - comprometidos.getOrDefault(stock.insumo().getId(), 0))))
                .toList();
    }
}
