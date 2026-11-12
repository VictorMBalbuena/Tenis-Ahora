package com.tenisahora.modules.auth.service;

import com.tenisahora.shared.cuenta.CuentaDeAcceso;
import com.tenisahora.shared.cuenta.DirectorioDeCuentas;
import com.tenisahora.shared.model.Rol;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonaDetailsServiceTest {

    @Mock
    private DirectorioDeCuentas directorio;

    @InjectMocks
    private PersonaDetailsService personaDetailsService;

    private CuentaDeAcceso cuenta(boolean activa) {
        return new CuentaDeAcceso(1L, "socio@tenisahora.com", "$2a$10$hash", Rol.SOCIO, activa);
    }

    @Test
    void personaActivo_devuelveDatosYRol() {
        when(directorio.porEmail("socio@tenisahora.com")).thenReturn(Optional.of(cuenta(true)));

        UserDetails detalles = personaDetailsService.loadUserByUsername("socio@tenisahora.com");

        assertThat(detalles.getUsername()).isEqualTo("socio@tenisahora.com");
        assertThat(detalles.getPassword()).isEqualTo("$2a$10$hash");
        assertThat(detalles.getAuthorities()).extracting("authority").containsExactly("ROLE_SOCIO");
        assertThat(detalles.isEnabled()).isTrue();
    }

    @Test
    void personaInactivo_quedaDeshabilitado() {
        when(directorio.porEmail("socio@tenisahora.com")).thenReturn(Optional.of(cuenta(false)));

        UserDetails detalles = personaDetailsService.loadUserByUsername("socio@tenisahora.com");

        assertThat(detalles.isEnabled()).isFalse();
    }

    @Test
    void emailInexistente_lanzaExcepcion() {
        when(directorio.porEmail("noexiste@tenisahora.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> personaDetailsService.loadUserByUsername("noexiste@tenisahora.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
