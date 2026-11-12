package com.tenisahora.modules.cobro.service;

import com.tenisahora.modules.cobro.entity.Cobro;
import com.tenisahora.modules.cobro.repository.CobroRepository;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Buscar un cobro o devolver 404. */
@Component
@RequiredArgsConstructor
public class CobroPorId {

    private final CobroRepository cobroRepository;

    public Cobro buscar(Long id) {
        return cobroRepository.findById(id).orElseThrow(this::noEncontrado);
    }

    /** Bloquea la fila hasta terminar: dos cambios de estado sobre el mismo cobro se atienden de a uno. */
    public Cobro buscarParaModificar(Long id) {
        return cobroRepository.buscarParaModificar(id).orElseThrow(this::noEncontrado);
    }

    private RecursoNoEncontradoException noEncontrado() {
        return new RecursoNoEncontradoException(CodigoError.COBRO_NO_ENCONTRADO, "Cobro no encontrado");
    }
}
