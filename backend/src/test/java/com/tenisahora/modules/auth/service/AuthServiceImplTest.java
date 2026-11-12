package com.tenisahora.modules.auth.service;

import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.modules.auth.dto.LoginRequest;
import com.tenisahora.modules.auth.entity.RefreshToken;
import com.tenisahora.modules.auth.repository.RefreshTokenRepository;
import com.tenisahora.shared.model.Rol;
import com.tenisahora.shared.cuenta.CuentaDeAcceso;
import com.tenisahora.shared.cuenta.DirectorioDeCuentas;
import com.tenisahora.security.JwtService;
import com.tenisahora.shared.exception.DemasiadosIntentosException;
import com.tenisahora.shared.util.HashUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private DirectorioDeCuentas directorio;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private BloqueoDeLogin bloqueoDeLogin;

    @InjectMocks
    private AuthServiceImpl authService;

    private CuentaDeAcceso persona;

    @BeforeEach
    void setUp() {
        persona = cuenta(true);
    }

    private static CuentaDeAcceso cuenta(boolean activa) {
        return new CuentaDeAcceso(3L, "socio@tenisahora.com", "hash", Rol.SOCIO, activa);
    }

    /** La cuenta que el directorio encuentra para el id y el rol del refresh token. */
    private void conLaCuenta(CuentaDeAcceso cuenta) {
        when(directorio.porId(3L, Rol.SOCIO)).thenReturn(Optional.of(cuenta));
    }

    private static String hash(String valor) {
        return HashUtils.sha256Hex(valor);
    }

    private RefreshToken refreshToken(String valor, boolean revocado, LocalDateTime vence) {
        RefreshToken rt = new RefreshToken();
        rt.setToken(valor);
        rt.setPersonaId(persona.id());
        rt.setRol(persona.rol().name());
        rt.setRevocado(revocado);
        rt.setFechaExpiracion(vence);
        return rt;
    }

    // ---------- login (CU71) ----------

    @Test
    void login_credencialesCorrectas_devuelveTokens() {
        when(directorio.porEmail("socio@tenisahora.com")).thenReturn(Optional.of(persona));
        when(jwtService.generarToken(persona.id(), persona.rol().name())).thenReturn("access-token");

        ResultadoDeSesion sesion = authService.login(new LoginRequest("socio@tenisahora.com", "TenisAhora2026!"));

        assertThat(sesion.accessToken()).isEqualTo("access-token");
        assertThat(sesion.refreshToken()).isNotBlank();
        assertThat(sesion.duracionDelRefresh()).isEqualTo(Duration.ofDays(7));
        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken("socio@tenisahora.com", "TenisAhora2026!"));
    }

    @Test
    void login_cuentaElIntentoAntesDeVerificarYLoLimpiaSiSaleBien() {
        when(directorio.porEmail("Socio@TenisAhora.com ")).thenReturn(Optional.of(persona));

        authService.login(new LoginRequest("Socio@TenisAhora.com ", "TenisAhora2026!"));

        InOrder orden = inOrder(bloqueoDeLogin, authenticationManager);
        // La clave del bloqueo no distingue mayúsculas ni espacios
        orden.verify(bloqueoDeLogin).iniciarIntento("socio@tenisahora.com");
        orden.verify(authenticationManager).authenticate(any());
        orden.verify(bloqueoDeLogin).intentoExitoso("socio@tenisahora.com");
    }

    @Test
    void login_cuentaBloqueada_nisiquieraVerificaLaContrasenia() {
        doThrow(new DemasiadosIntentosException(CodigoError.LOGIN_BLOQUEADO, "Bloqueada", Duration.ofMinutes(15)))
                .when(bloqueoDeLogin).iniciarIntento("socio@tenisahora.com");

        assertThatThrownBy(() -> authService.login(new LoginRequest("socio@tenisahora.com", "TenisAhora2026!")))
                .isInstanceOf(DemasiadosIntentosException.class);
        verify(authenticationManager, never()).authenticate(any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void login_guardaRefreshTokenVigentePor7Dias() {
        when(directorio.porEmail("socio@tenisahora.com")).thenReturn(Optional.of(persona));

        ResultadoDeSesion sesion = authService.login(new LoginRequest("socio@tenisahora.com", "TenisAhora2026!"));

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken guardado = captor.getValue();
        assertThat(guardado.getToken()).isEqualTo(hash(sesion.refreshToken()));
        assertThat(guardado.getToken()).isNotEqualTo(sesion.refreshToken());
        assertThat(guardado.getPersonaId()).isEqualTo(persona.id());
        assertThat(guardado.getRol()).isEqualTo("SOCIO");
        assertThat(guardado.getRevocado()).isFalse();
        assertThat(guardado.getFechaExpiracion()).isCloseTo(LocalDateTime.now().plusDays(7), within(1, java.time.temporal.ChronoUnit.MINUTES));
    }

    @Test
    void login_cadaLoginGeneraUnRefreshTokenDistinto() {
        when(directorio.porEmail("socio@tenisahora.com")).thenReturn(Optional.of(persona));
        LoginRequest request = new LoginRequest("socio@tenisahora.com", "TenisAhora2026!");

        String primero = authService.login(request).refreshToken();
        String segundo = authService.login(request).refreshToken();

        assertThat(primero).isNotEqualTo(segundo);
    }

    @Test
    void login_credencialesIncorrectas_lanzaExcepcionYNoGeneraTokensNiLimpiaElContador() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("socio@tenisahora.com", "mala")))
                .isInstanceOf(BadCredentialsException.class);
        verify(jwtService, never()).generarToken(any(), any());
        verify(refreshTokenRepository, never()).save(any());
        verify(bloqueoDeLogin, never()).intentoExitoso(any());
        verify(bloqueoDeLogin, never()).descontarIntento(any());
    }

    @Test
    void login_cuentaDeshabilitada_cuentaComoIntentoFallido() {
        when(authenticationManager.authenticate(any())).thenThrow(new DisabledException("User is disabled"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("socio@tenisahora.com", "TenisAhora2026!")))
                .isInstanceOf(DisabledException.class);
        verify(refreshTokenRepository, never()).save(any());
        verify(bloqueoDeLogin, never()).intentoExitoso(any());
    }

    @Test
    void login_siFallaAlgoNuestro_elIntentoNoCuentaContraLaPersona() {
        when(authenticationManager.authenticate(any())).thenThrow(new IllegalStateException("la base no responde"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("socio@tenisahora.com", "TenisAhora2026!")))
                .isInstanceOf(IllegalStateException.class);
        verify(bloqueoDeLogin).descontarIntento("socio@tenisahora.com");
    }

    // ---------- refresh ----------

    @Test
    void refresh_tokenVigente_loRevocaYDevuelveTokensNuevos() {
        conLaCuenta(persona);
        when(refreshTokenRepository.findByToken(hash("rt-1")))
                .thenReturn(Optional.of(refreshToken("rt-1", false, LocalDateTime.now().plusDays(1))));
        when(refreshTokenRepository.revocarSiVigente(hash("rt-1"))).thenReturn(1);
        when(jwtService.generarToken(persona.id(), persona.rol().name())).thenReturn("access-nuevo");

        ResultadoDeSesion sesion = authService.refresh("rt-1");

        assertThat(sesion.accessToken()).isEqualTo("access-nuevo");
        assertThat(sesion.refreshToken()).isNotEqualTo("rt-1");
        verify(refreshTokenRepository).revocarSiVigente(hash("rt-1"));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void refresh_tokenInexistente_lanzaExcepcion() {
        when(refreshTokenRepository.findByToken(hash("inventado"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("inventado")).isInstanceOf(BadCredentialsException.class);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void refresh_tokenRevocado_lanzaExcepcion() {
        when(refreshTokenRepository.findByToken(hash("rt-1")))
                .thenReturn(Optional.of(refreshToken("rt-1", true, LocalDateTime.now().plusDays(1))));

        assertThatThrownBy(() -> authService.refresh("rt-1")).isInstanceOf(BadCredentialsException.class);
        verify(refreshTokenRepository, never()).revocarSiVigente(any());
    }

    @Test
    void refresh_tokenVencido_lanzaExcepcion() {
        when(refreshTokenRepository.findByToken(hash("rt-1")))
                .thenReturn(Optional.of(refreshToken("rt-1", false, LocalDateTime.now().minusMinutes(1))));

        assertThatThrownBy(() -> authService.refresh("rt-1")).isInstanceOf(BadCredentialsException.class);
        verify(refreshTokenRepository, never()).revocarSiVigente(any());
    }

    @Test
    void refresh_personaDeshabilitada_lanzaExcepcion() {
        conLaCuenta(cuenta(false));
        when(refreshTokenRepository.findByToken(hash("rt-1")))
                .thenReturn(Optional.of(refreshToken("rt-1", false, LocalDateTime.now().plusDays(1))));

        assertThatThrownBy(() -> authService.refresh("rt-1")).isInstanceOf(BadCredentialsException.class);
        verify(refreshTokenRepository, never()).revocarSiVigente(any());
    }

    @Test
    void refresh_dosPedidosConElMismoToken_elQuePierdeLaCarreraEsRechazado() {
        conLaCuenta(persona);
        when(refreshTokenRepository.findByToken(hash("rt-1")))
                .thenReturn(Optional.of(refreshToken("rt-1", false, LocalDateTime.now().plusDays(1))));
        // Otro request ya lo revocó entre la lectura y el update
        when(refreshTokenRepository.revocarSiVigente(hash("rt-1"))).thenReturn(0);

        assertThatThrownBy(() -> authService.refresh("rt-1")).isInstanceOf(BadCredentialsException.class);
        verify(jwtService, never()).generarToken(any(), any());
        verify(refreshTokenRepository, never()).save(any());
    }

    // ---------- logout (CU72) ----------

    @Test
    void logout_revocaElRefreshToken() {
        authService.logout("rt-1");

        verify(refreshTokenRepository).revocarSiVigente(hash("rt-1"));
    }

    @Test
    void logout_tokenInexistente_noFalla() {
        when(refreshTokenRepository.revocarSiVigente(hash("inventado"))).thenReturn(0);

        authService.logout("inventado");

        verify(refreshTokenRepository).revocarSiVigente(hash("inventado"));
    }
}
