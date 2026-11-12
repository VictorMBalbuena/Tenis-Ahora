package com.tenisahora.middleware;

import com.tenisahora.shared.exception.CodigoError;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tenisahora.security.LimitadorDeIntentos;
import com.tenisahora.shared.dto.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * Límite de pedidos por IP en los endpoints públicos que se prestan al abuso: login, registro y renovación.
 * Corre después de la cadena de seguridad (así el 429 ya lleva los encabezados de CORS y el front puede leerlo) y
 * antes del controller (así un pedido bloqueado no gasta la verificación de la contraseña).
 *
 * <p>La IP sale de {@code request.getRemoteAddr()}. Detrás de un proxy (Render) hay que activar
 * {@code server.forward-headers-strategy=native} para que sea la de la persona y no la del proxy.
 */
@Slf4j
@Component
public class LimitePorIpFilter extends OncePerRequestFilter {

    private record Regla(String nombre, int maximo, Duration ventana) {
    }

    private final LimitadorDeIntentos limitador;
    private final ObjectMapper objectMapper;
    private final Map<String, Regla> reglasPorRuta;

    public LimitePorIpFilter(LimitadorDeIntentos limitador,
                             ObjectMapper objectMapper,
                             @Value("${app.rate-limit.login.max:60}") int loginMaximo,
                             @Value("${app.rate-limit.login.ventana-minutos:15}") int loginMinutos,
                             @Value("${app.rate-limit.registro.max:10}") int registroMaximo,
                             @Value("${app.rate-limit.registro.ventana-minutos:60}") int registroMinutos,
                             @Value("${app.rate-limit.refresh.max:120}") int refreshMaximo,
                             @Value("${app.rate-limit.refresh.ventana-minutos:15}") int refreshMinutos) {
        this.limitador = limitador;
        this.objectMapper = objectMapper;
        this.reglasPorRuta = Map.of(
                "/api/auth/login", new Regla("login", loginMaximo, Duration.ofMinutes(loginMinutos)),
                "/api/auth/register", new Regla("registro", registroMaximo, Duration.ofMinutes(registroMinutos)),
                "/api/auth/refresh", new Regla("refresh", refreshMaximo, Duration.ofMinutes(refreshMinutos)));
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !HttpMethod.POST.matches(request.getMethod()) || !reglasPorRuta.containsKey(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        Regla regla = reglasPorRuta.get(request.getRequestURI());
        String ip = request.getRemoteAddr();

        Optional<Duration> espera = limitador.intentar(regla.nombre() + "|" + ip, regla.maximo(), regla.ventana());
        if (espera.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        long segundos = Math.max(1, espera.get().toSeconds() + (espera.get().toNanosPart() > 0 ? 1 : 0));
        log.warn("Límite de pedidos superado en {} desde la IP {} (máximo {} cada {} minutos)",
                request.getRequestURI(), ip, regla.maximo(), regla.ventana().toMinutes());

        response.setStatus(429);
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(segundos));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ErrorResponse.de(CodigoError.LIMITE_DE_PEDIDOS_SUPERADO,
                "Hiciste demasiados pedidos. Esperá " + ((segundos + 59) / 60) + " minuto(s) antes de volver a intentar.",
                request.getRequestURI()));
    }
}
