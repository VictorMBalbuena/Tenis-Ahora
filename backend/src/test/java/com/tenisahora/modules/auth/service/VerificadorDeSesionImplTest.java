package com.tenisahora.modules.auth.service;

import com.tenisahora.shared.cuenta.CuentaDeAcceso;
import com.tenisahora.shared.cuenta.DirectorioDeCuentas;
import com.tenisahora.shared.model.Rol;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificadorDeSesionImplTest {

    @Mock
    private DirectorioDeCuentas directorio;

    @InjectMocks
    private VerificadorDeSesionImpl verificador;

    private CuentaDeAcceso cuenta(boolean activa) {
        return new CuentaDeAcceso(3L, "e@tenisahora.com", "hash", Rol.EMPLEADO, activa);
    }

    @Test
    void cuentaActiva_devuelveElRolDeLaBase() {
        when(directorio.porId(3L, Rol.EMPLEADO)).thenReturn(Optional.of(cuenta(true)));

        assertThat(verificador.rolVigente(3L, "EMPLEADO")).contains("EMPLEADO");
    }

    @Test
    void cuentaInexistente_devuelveVacio() {
        when(directorio.porId(3L, Rol.EMPLEADO)).thenReturn(Optional.empty());

        assertThat(verificador.rolVigente(3L, "EMPLEADO")).isEmpty();
    }

    @Test
    void cuentaInactiva_devuelveVacio() {
        when(directorio.porId(3L, Rol.EMPLEADO)).thenReturn(Optional.of(cuenta(false)));

        assertThat(verificador.rolVigente(3L, "EMPLEADO")).isEmpty();
    }

    @Test
    void rolDesconocidoEnElToken_devuelveVacioSinConsultarLaBase() {
        assertThat(verificador.rolVigente(3L, "INSTRUCTOR")).isEmpty();
        assertThat(verificador.rolVigente(3L, null)).isEmpty();

        verifyNoInteractions(directorio);
    }
}
