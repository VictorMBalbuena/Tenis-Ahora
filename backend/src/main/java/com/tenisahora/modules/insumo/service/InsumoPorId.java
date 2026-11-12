package com.tenisahora.modules.insumo.service;

import com.tenisahora.modules.insumo.entity.Insumo;
import com.tenisahora.modules.insumo.repository.InsumoRepository;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Busca el insumo de un pedido; uno dado de baja se trata como inexistente. */
@Component
@RequiredArgsConstructor
public class InsumoPorId {

    private final InsumoRepository insumoRepository;

    public Insumo activo(Long id) {
        return insumoRepository.findByIdAndActivoTrue(id).orElseThrow(this::noEncontrado);
    }

    /** Igual que activo, pero con la fila bloqueada hasta terminar la transacción. */
    public Insumo activoParaModificar(Long id) {
        return insumoRepository.buscarActivoParaModificar(id).orElseThrow(this::noEncontrado);
    }

    private RecursoNoEncontradoException noEncontrado() {
        return new RecursoNoEncontradoException(CodigoError.INSUMO_NO_ENCONTRADO, "Insumo no encontrado");
    }
}
