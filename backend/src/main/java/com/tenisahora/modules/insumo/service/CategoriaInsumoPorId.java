package com.tenisahora.modules.insumo.service;

import com.tenisahora.modules.insumo.entity.CategoriaInsumo;
import com.tenisahora.modules.insumo.repository.CategoriaInsumoRepository;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Busca la categoría de un pedido de Insumo. */
@Component
@RequiredArgsConstructor
public class CategoriaInsumoPorId {

    private final CategoriaInsumoRepository categoriaInsumoRepository;

    public CategoriaInsumo buscar(Long id) {
        return categoriaInsumoRepository.findById(id).orElseThrow(
                () -> new RecursoNoEncontradoException(CodigoError.CATEGORIA_INSUMO_NO_ENCONTRADA, "Categoría de insumo no encontrada"));
    }
}
