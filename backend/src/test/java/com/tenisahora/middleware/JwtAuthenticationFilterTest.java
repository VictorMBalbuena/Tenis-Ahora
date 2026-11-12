package com.tenisahora.middleware;

import com.tenisahora.shared.model.Rol;
import com.tenisahora.modules.socio.entity.Socio;
import com.tenisahora.security.JwtService;
import com.tenisahora.security.VerificadorDeSesion;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private static final String SECRETO = "clave-de-test-con-largo-suficiente-para-hs384-0123456789";

    private JwtService jwtService;
    private VerificadorDeSesion verificador;
    private JwtAuthenticationFilter filtro;
    private FilterChain cadena;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRETO, 30);
        verificador = mock(VerificadorDeSesion.class);
        filtro = new JwtAuthenticationFilter(jwtService, verificador);
        cadena = mock(FilterChain.class);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private String tokenDe(Long id, Rol rol) {
        Socio persona = new Socio();
        persona.setId(id);
        return jwtService.generarToken(persona.getId(), rol.name());
    }

    @Test
    void tokenValido_autenticaConIdYRol() throws Exception {
        when(verificador.rolVigente(org.mockito.ArgumentMatchers.eq(3L), org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.of("EMPLEADO"));
        request.addHeader("Authorization", "Bearer " + tokenDe(3L, Rol.EMPLEADO));

        filtro.doFilter(request, response, cadena);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo(3L);
        assertThat(auth.getAuthorities()).extracting("authority").containsExactly("ROLE_EMPLEADO");
        verify(cadena).doFilter(request, response);
    }

    @Test
    void elRolDeLaBaseManda_porSobreElDelToken() throws Exception {
        // El token se emitió cuando era SOCIO, pero hoy es ADMINISTRADOR en la base
        when(verificador.rolVigente(org.mockito.ArgumentMatchers.eq(3L), org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.of("ADMINISTRADOR"));
        request.addHeader("Authorization", "Bearer " + tokenDe(3L, Rol.SOCIO));

        filtro.doFilter(request, response, cadena);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth.getAuthorities()).extracting("authority").containsExactly("ROLE_ADMINISTRADOR");
    }

    @Test
    void personaEliminadoODeshabilitado_noAutentica() throws Exception {
        when(verificador.rolVigente(org.mockito.ArgumentMatchers.eq(3L), org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.empty());
        request.addHeader("Authorization", "Bearer " + tokenDe(3L, Rol.SOCIO));

        filtro.doFilter(request, response, cadena);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(cadena).doFilter(request, response);
    }

    @Test
    void sinEncabezado_noAutenticaYSigueLaCadena() throws Exception {
        filtro.doFilter(request, response, cadena);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(cadena).doFilter(request, response);
    }

    @Test
    void encabezadoSinBearer_noAutentica() throws Exception {
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        filtro.doFilter(request, response, cadena);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(cadena).doFilter(request, response);
    }

    @Test
    void tokenInvalido_noAutenticaYSigueLaCadena() throws Exception {
        request.addHeader("Authorization", "Bearer basura");

        filtro.doFilter(request, response, cadena);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(cadena).doFilter(request, response);
    }

    @Test
    void tokenVencido_noAutentica() throws Exception {
        JwtService servicioVencido = new JwtService(SECRETO, -1);
        Socio persona = new Socio();
        persona.setId(3L);
        request.addHeader("Authorization", "Bearer " + servicioVencido.generarToken(persona.getId(), persona.getRol().name()));

        filtro.doFilter(request, response, cadena);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(cadena).doFilter(request, response);
    }
}
