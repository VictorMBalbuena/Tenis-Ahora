package com.tenisahora.modules.auth.service;

import com.tenisahora.security.VerificadorDeSesion;
import com.tenisahora.shared.cuenta.CuentaDeAcceso;
import com.tenisahora.shared.cuenta.DirectorioDeCuentas;
import com.tenisahora.shared.model.Rol;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VerificadorDeSesionImpl implements VerificadorDeSesion {

    private final DirectorioDeCuentas directorio;

    @Override
    @Transactional(readOnly = true)
    public Optional<String> rolVigente(Long personaId, String rolDelToken) {
        return rolConocido(rolDelToken)
                .flatMap(rol -> directorio.porId(personaId, rol))
                .filter(CuentaDeAcceso::activa)
                .map(cuenta -> cuenta.rol().name());
    }

    private Optional<Rol> rolConocido(String rol) {
        try {
            return Optional.of(Rol.valueOf(rol));
        } catch (IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }
}
