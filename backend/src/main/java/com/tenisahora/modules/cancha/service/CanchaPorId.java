package com.tenisahora.modules.cancha.service;

import com.tenisahora.modules.cancha.entity.Cancha;
import com.tenisahora.modules.cancha.repository.CanchaRepository;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Busca la cancha de un pedido y lanza el error que corresponde si no existe o está deshabilitada. */
@Component
@RequiredArgsConstructor
public class CanchaPorId {

    private final CanchaRepository canchaRepository;

    public Cancha existente(Long id) {
        return canchaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(CodigoError.CANCHA_NO_ENCONTRADA, "Cancha no encontrada"));
    }

    /** Para cargar horarios o bloqueos: una cancha deshabilitada no admite novedades. */
    public Cancha activa(Long id) {
        Cancha cancha = existente(id);
        if (!Boolean.TRUE.equals(cancha.getActiva())) {
            throw new ReglaDeNegocioException(CodigoError.CANCHA_INACTIVA, "La cancha está deshabilitada");
        }
        return cancha;
    }
}
