package com.tenisahora.modules.insumo.service;

import com.tenisahora.modules.insumo.dto.InsumoResponse;
import com.tenisahora.modules.insumo.dto.LimitesDeStock;
import com.tenisahora.modules.insumo.dto.MovimientoRequest;
import com.tenisahora.modules.insumo.dto.MovimientoResponse;
import com.tenisahora.modules.insumo.entity.Insumo;
import com.tenisahora.modules.insumo.entity.TipoMovimiento;
import com.tenisahora.modules.insumo.mapper.InsumoMapper;
import com.tenisahora.modules.insumo.repository.InsumoRepository;
import com.tenisahora.modules.insumo.repository.MovimientoStockRepository;
import com.tenisahora.shared.dto.PageResponse;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import com.tenisahora.shared.exception.SolicitudInvalidaException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class InsumoStockImpl implements InsumoStock {

    private final InsumoRepository insumoRepository;
    private final MovimientoStockRepository movimientoRepository;
    private final InsumoPorId insumoPorId;
    private final InsumoMapper insumoMapper;
    private final RegistroDeMovimientos movimientos;
    private final UsoDeInsumos usoDeInsumos;

    @Override
    @Transactional
    public InsumoResponse descontar(Long insumoId, int cantidad, String motivo, Long personaId) {
        validarCantidad(cantidad);
        if (insumoRepository.descontar(insumoId, cantidad) == 0) {
            throw sinStockSuficiente(insumoId, cantidad);
        }
        return asentar(insumoId, TipoMovimiento.DESCUENTO, -cantidad, motivo, personaId);
    }

    @Override
    @Transactional
    public InsumoResponse reponer(Long insumoId, int cantidad, String motivo, Long personaId) {
        validarCantidad(cantidad);
        if (insumoRepository.reponer(insumoId, cantidad, LimitesDeStock.MAXIMO_STOCK - cantidad) == 0) {
            insumoPorId.activo(insumoId);
            throw new ReglaDeNegocioException(CodigoError.MOVIMIENTO_INVALIDO,
                    "El stock no puede superar " + LimitesDeStock.MAXIMO_STOCK + " unidades");
        }
        return asentar(insumoId, TipoMovimiento.REPOSICION, cantidad, motivo, personaId);
    }

    @Override
    @Transactional
    public InsumoResponse mover(Long insumoId, MovimientoRequest request, Long personaId) {
        return switch (request.tipo()) {
            case DESCUENTO -> descontar(insumoId, request.cantidad(), request.motivo(), personaId);
            case REPOSICION -> reponer(insumoId, request.cantidad(), request.motivo(), personaId);
            case AJUSTE -> throw new SolicitudInvalidaException(CodigoError.MOVIMIENTO_INVALIDO,
                    "El ajuste se hace modificando el insumo; acá solo se descuenta o se repone");
        };
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<MovimientoResponse> historial(Long insumoId, int page, int size) {
        insumoPorId.activo(insumoId);
        return PageResponse.de(movimientoRepository.findByInsumoIdOrderByFechaDescIdDesc(insumoId, PageRequest.of(page, size)),
                insumoMapper::aMovimiento);
    }

    /** Deja el movimiento en el rastro y devuelve el insumo tal como quedó. */
    private InsumoResponse asentar(Long insumoId, TipoMovimiento tipo, int variacion, String motivo, Long personaId) {
        Insumo actual = insumoPorId.activo(insumoId);
        movimientos.registrar(actual, tipo, variacion, actual.getCantidadDisponible(), motivo, personaId);
        log.info("Insumo {}: {} de {} unidades por la persona {}", insumoId, tipo, Math.abs(variacion), personaId);
        return insumoMapper.aRespuesta(actual, usoDeInsumos.enUso(Set.of(insumoId)).getOrDefault(insumoId, 0));
    }

    /** No alcanzó el stock: si el insumo no existe es un 404, si existe se dice cuántas unidades hay. */
    private ReglaDeNegocioException sinStockSuficiente(Long insumoId, int pedido) {
        Insumo insumo = insumoPorId.activo(insumoId);
        return new ReglaDeNegocioException(CodigoError.INSUMO_STOCK_INSUFICIENTE, "Solo hay "
                + insumo.getCantidadDisponible() + " unidades disponibles de " + insumo.getNombre() + " y se pidieron " + pedido);
    }

    private void validarCantidad(int cantidad) {
        if (cantidad < 1 || cantidad > LimitesDeStock.MAXIMO_POR_MOVIMIENTO) {
            throw new SolicitudInvalidaException(CodigoError.MOVIMIENTO_INVALIDO,
                    "La cantidad debe estar entre 1 y " + LimitesDeStock.MAXIMO_POR_MOVIMIENTO);
        }
    }
}
