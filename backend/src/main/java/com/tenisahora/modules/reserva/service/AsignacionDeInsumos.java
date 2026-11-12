package com.tenisahora.modules.reserva.service;

import com.tenisahora.modules.cancha.service.FranjaHoraria;
import com.tenisahora.modules.insumo.service.ConsultaDeStock;
import com.tenisahora.modules.reserva.dto.InsumoPedidoRequest;
import com.tenisahora.modules.reserva.entity.Reserva;
import com.tenisahora.modules.reserva.entity.ReservaInsumo;
import com.tenisahora.modules.reserva.repository.ReservaInsumoRepository;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import com.tenisahora.shared.exception.SolicitudInvalidaException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Asigna los insumos a una reserva (CU13): comprueba que alcancen para ese horario y reemplaza lo que tenía. */
@Component
@RequiredArgsConstructor
public class AsignacionDeInsumos {

    private final ConsultaDeStock consultaDeStock;
    private final DisponibilidadDeInsumos disponibilidad;
    private final ReservaInsumoRepository reservaInsumoRepository;
    private final ReservasProperties propiedades;

    /** Reemplaza los insumos de la reserva por el pedido; un pedido vacío o nulo los quita todos. Todo o nada. */
    public List<ReservaInsumo> asignar(Reserva reserva, List<InsumoPedidoRequest> pedidos) {
        List<InsumoPedidoRequest> pedido = pedidos == null ? List.of() : pedidos;
        validar(pedido);
        Map<Long, ConsultaDeStock.StockLibre> stock = pedido.isEmpty() ? Map.of() : comprobar(reserva, pedido);
        reservaInsumoRepository.borrarDeLaReserva(reserva.getId());
        return reservaInsumoRepository.saveAll(pedido.stream().map(renglon -> asignado(reserva, renglon, stock)).toList());
    }

    /** Vuelve a comprobar lo que ya tiene la reserva contra su turno actual: se usa cuando se mueve la reserva. */
    public void revalidar(Reserva reserva) {
        List<ReservaInsumo> actuales = reservaInsumoRepository.deLaReserva(reserva.getId());
        if (!actuales.isEmpty()) {
            comprobar(reserva, actuales.stream()
                    .map(actual -> new InsumoPedidoRequest(actual.getInsumo().getId(), actual.getCantidadAsignada())).toList());
        }
    }

    private Map<Long, ConsultaDeStock.StockLibre> comprobar(Reserva reserva, List<InsumoPedidoRequest> pedido) {
        List<Long> ids = pedido.stream().map(InsumoPedidoRequest::insumoId).toList();
        Map<Long, ConsultaDeStock.StockLibre> stock = consultaDeStock.bloquear(ids);
        Map<Long, Integer> comprometidos = disponibilidad.comprometidos(ids, reserva.getFecha(),
                new FranjaHoraria(reserva.getHoraInicio(), reserva.getHoraFin()), reserva.getId());
        pedido.forEach(renglon -> asegurarQueAlcanza(renglon, stock.get(renglon.insumoId()), comprometidos.getOrDefault(renglon.insumoId(), 0)));
        return stock;
    }

    private void asegurarQueAlcanza(InsumoPedidoRequest renglon, ConsultaDeStock.StockLibre stock, int comprometido) {
        int asignables = Math.max(0, stock.stockTotal() - comprometido);
        if (renglon.cantidad() > asignables) {
            throw new ReglaDeNegocioException(CodigoError.INSUMO_STOCK_INSUFICIENTE, "Solo hay " + asignables + " unidades de "
                    + stock.insumo().getNombre() + " para ese horario y se pidieron " + renglon.cantidad());
        }
    }

    private void validar(List<InsumoPedidoRequest> pedido) {
        if (pedido.size() > propiedades.maxRenglonesDeInsumos()) {
            throw invalido("Una reserva lleva hasta " + propiedades.maxRenglonesDeInsumos() + " insumos distintos");
        }
        Set<Long> vistos = new HashSet<>();
        for (InsumoPedidoRequest renglon : pedido) {
            if (!vistos.add(renglon.insumoId())) {
                throw invalido("Un insumo figura más de una vez: sumá las unidades en un solo renglón");
            }
            if (renglon.cantidad() > propiedades.maxUnidadesPorInsumo()) {
                throw invalido("Se pueden pedir hasta " + propiedades.maxUnidadesPorInsumo() + " unidades de cada insumo");
            }
        }
    }

    private ReservaInsumo asignado(Reserva reserva, InsumoPedidoRequest renglon, Map<Long, ConsultaDeStock.StockLibre> stock) {
        ReservaInsumo asignado = new ReservaInsumo();
        asignado.setReserva(reserva);
        asignado.setInsumo(stock.get(renglon.insumoId()).insumo());
        asignado.setCantidadAsignada(renglon.cantidad());
        asignado.setRepuesto(false);
        asignado.setDescontado(false);
        return asignado;
    }

    private SolicitudInvalidaException invalido(String mensaje) {
        return new SolicitudInvalidaException(CodigoError.RESERVA_INSUMOS_INVALIDOS, mensaje);
    }
}
