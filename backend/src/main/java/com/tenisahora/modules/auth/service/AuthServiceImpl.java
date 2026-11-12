package com.tenisahora.modules.auth.service;

import com.tenisahora.modules.auth.dto.LoginRequest;
import com.tenisahora.modules.auth.entity.RefreshToken;
import com.tenisahora.modules.auth.repository.RefreshTokenRepository;
import com.tenisahora.shared.cuenta.CuentaDeAcceso;
import com.tenisahora.shared.cuenta.DirectorioDeCuentas;
import com.tenisahora.shared.model.Rol;
import com.tenisahora.security.JwtService;
import com.tenisahora.shared.util.HashUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Duration REFRESH_VALIDEZ = Duration.ofDays(7);
    private static final int REFRESH_BYTES_ALEATORIOS = 32;
    private static final SecureRandom ALEATORIO_SEGURO = new SecureRandom();

    private final AuthenticationManager authenticationManager;
    private final DirectorioDeCuentas directorio;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final BloqueoDeLogin bloqueoDeLogin;

    @Override
    public ResultadoDeSesion login(LoginRequest request) {
        String cuenta = BloqueoDeLogin.claveDe(request.email());
        // Cuenta el intento y corta acá si la cuenta está bloqueada (aunque la contraseña sea la correcta)
        int intento = bloqueoDeLogin.iniciarIntento(cuenta);
        verificarCredenciales(request, cuenta, intento);
        bloqueoDeLogin.intentoExitoso(cuenta);

        CuentaDeAcceso cuentaDeAcceso = directorio.porEmail(request.email())
                .orElseThrow(() -> new UsernameNotFoundException("Persona no encontrada: " + request.email()));
        return generarSesion(cuentaDeAcceso);
    }

    @Override
    @Transactional
    public ResultadoDeSesion refresh(String refreshTokenValor) {
        String hash = HashUtils.sha256Hex(refreshTokenValor);

        RefreshToken refreshToken = refreshTokenRepository.findByToken(hash)
                .filter(rt -> !rt.getRevocado())
                .filter(rt -> rt.getFechaExpiracion().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new BadCredentialsException("Refresh token invalido o vencido"));
        CuentaDeAcceso cuenta = cuentaVigenteDe(refreshToken);

        // Update atomico (revocado = false -> true): si dos requests llegan con el mismo
        // token, solo uno gana la carrera y el otro queda rechazado en vez de generar
        // dos pares de tokens validos a partir de un mismo refresh token.
        if (refreshTokenRepository.revocarSiVigente(hash) == 0) {
            throw new BadCredentialsException("Refresh token invalido o vencido");
        }

        return generarSesion(cuenta);
    }

    @Override
    @Transactional
    public void logout(String refreshTokenValor) {
        refreshTokenRepository.revocarSiVigente(HashUtils.sha256Hex(refreshTokenValor));
    }

    @Override
    @Transactional
    public void revocarTodasLasSesiones(Long personaId) {
        refreshTokenRepository.revocarTodosDeLaPersona(personaId);
    }

    private void verificarCredenciales(LoginRequest request, String cuenta, int intento) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (AuthenticationException e) {
            registrarFallo(cuenta, intento);
            throw e;
        } catch (RuntimeException e) {
            // Falló algo nuestro (por ejemplo la base): ese intento no tiene que contar contra la persona
            bloqueoDeLogin.descontarIntento(cuenta);
            throw e;
        }
    }

    /** Deja constancia del intento fallido sin escribir el correo ni la contraseña: solo un código corto de la cuenta. */
    private void registrarFallo(String cuenta, int intento) {
        String huella = BloqueoDeLogin.huellaDe(cuenta);
        log.warn("Login fallido (cuenta #{}, intento {} de {})", huella, intento, bloqueoDeLogin.maximoDeIntentos());
        if (intento >= bloqueoDeLogin.maximoDeIntentos()) {
            log.warn("Cuenta #{} bloqueada por {} minutos tras {} intentos fallidos seguidos",
                    huella, bloqueoDeLogin.minutosDeBloqueo(), intento);
        }
    }

    private CuentaDeAcceso cuentaVigenteDe(RefreshToken refreshToken) {
        return directorio.porId(refreshToken.getPersonaId(), Rol.valueOf(refreshToken.getRol()))
                .filter(CuentaDeAcceso::activa)
                .orElseThrow(() -> new BadCredentialsException("Refresh token invalido o vencido"));
    }

    private ResultadoDeSesion generarSesion(CuentaDeAcceso cuenta) {
        String accessToken = jwtService.generarToken(cuenta.id(), cuenta.rol().name());

        // Al cliente se le entrega el valor; en la base solo queda su hash
        String refreshTokenValor = generarValorAleatorio();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setPersonaId(cuenta.id());
        refreshToken.setRol(cuenta.rol().name());
        refreshToken.setToken(HashUtils.sha256Hex(refreshTokenValor));
        refreshToken.setFechaExpiracion(LocalDateTime.now().plus(REFRESH_VALIDEZ));
        refreshTokenRepository.save(refreshToken);

        return new ResultadoDeSesion(accessToken, refreshTokenValor, REFRESH_VALIDEZ);
    }

    private String generarValorAleatorio() {
        byte[] bytes = new byte[REFRESH_BYTES_ALEATORIOS];
        ALEATORIO_SEGURO.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
