package com.tenisahora.modules.insumo.service;

import com.tenisahora.modules.insumo.entity.Insumo;
import com.tenisahora.modules.insumo.repository.InsumoRepository;
import com.tenisahora.modules.insumo.repository.InsumoSpecs;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConsultaDeStockImpl implements ConsultaDeStock {

    private final InsumoRepository insumoRepository;
    private final UsoDeInsumos usoDeInsumos;

    @Override
    @Transactional
    public Map<Long, StockLibre> bloquear(Collection<Long> insumoIds) {
        Set<Long> pedidos = new TreeSet<>(insumoIds);
        List<Insumo> insumos = insumoRepository.bloquearActivos(pedidos);
        if (insumos.size() != pedidos.size()) {
            throw new RecursoNoEncontradoException(CodigoError.INSUMO_NO_ENCONTRADO, "Insumo no encontrado");
        }
        Map<Long, Integer> enUso = usoDeInsumos.enUso(pedidos);
        Map<Long, StockLibre> stock = new LinkedHashMap<>();
        insumos.forEach(insumo -> {
            int disponible = insumo.getCantidadDisponible() != null ? insumo.getCantidadDisponible() : 0;
            int uso = enUso.getOrDefault(insumo.getId(), 0);
            stock.put(insumo.getId(), new StockLibre(insumo, disponible, disponible + uso));
        });
        return stock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockLibre> activos() {
        List<Insumo> activos = insumoRepository.findAll(InsumoSpecs.activo(), Sort.by("nombre"));
        Set<Long> ids = activos.stream().map(Insumo::getId).collect(Collectors.toSet());
        Map<Long, Integer> enUso = usoDeInsumos.enUso(ids);
        return activos.stream()
                .map(insumo -> {
                    int disponible = insumo.getCantidadDisponible() != null ? insumo.getCantidadDisponible() : 0;
                    int uso = enUso.getOrDefault(insumo.getId(), 0);
                    return new StockLibre(insumo, disponible, disponible + uso);
                }).toList();
    }
}
