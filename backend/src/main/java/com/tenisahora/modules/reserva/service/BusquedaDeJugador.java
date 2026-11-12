package com.tenisahora.modules.reserva.service;

import com.tenisahora.modules.reserva.dto.JugadorEncontradoResponse;
import com.tenisahora.modules.socio.repository.SocioRepository;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Solo por DNI exacto y solo socios activos: un socio puede sumar a otro a su reserva, no recorrer el padrón. */
@Service
@RequiredArgsConstructor
public class BusquedaDeJugador {

    private final SocioRepository socioRepository;

    @Transactional(readOnly = true)
    public JugadorEncontradoResponse porDni(String dni) {
        return socioRepository.findByDniAndActivoTrue(dni)
                .map(socio -> new JugadorEncontradoResponse(socio.getId(), socio.getNombre(), socio.getApellido()))
                .orElseThrow(() -> new RecursoNoEncontradoException(CodigoError.PERSONA_NO_ENCONTRADA, "No hay un socio activo con ese DNI"));
    }
}
