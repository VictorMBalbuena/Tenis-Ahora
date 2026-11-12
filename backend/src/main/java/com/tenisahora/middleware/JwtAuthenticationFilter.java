package com.tenisahora.middleware;

import com.tenisahora.modules.permiso.model.CodigoDePermiso;
import com.tenisahora.security.ConsultaDePermisos;
import com.tenisahora.security.JwtService;
import com.tenisahora.security.VerificadorDeSesion;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIJO_BEARER = "Bearer ";

    private final JwtService jwtService;
    private final VerificadorDeSesion verificadorDeSesion;

    /**
     * El módulo de permisos es opcional a propósito: así este filtro se puede montar solo (por ejemplo en una
     * prueba de un controlador) sin arrastrar todo el módulo. Sin él, la sesión lleva el rol y nada más, que es
     * exactamente como funcionaba antes de que existiera la matriz de permisos.
     */
    private final ObjectProvider<ConsultaDePermisos> consultaDePermisos;

    @Autowired
    public JwtAuthenticationFilter(JwtService jwtService, VerificadorDeSesion verificadorDeSesion,
                                   ObjectProvider<ConsultaDePermisos> consultaDePermisos) {
        this.jwtService = jwtService;
        this.verificadorDeSesion = verificadorDeSesion;
        this.consultaDePermisos = consultaDePermisos;
    }

    /** Sin módulo de permisos: la sesión lleva solo el rol, como antes de que existiera la matriz. */
    public JwtAuthenticationFilter(JwtService jwtService, VerificadorDeSesion verificadorDeSesion) {
        this(jwtService, verificadorDeSesion, null);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        String encabezado = request.getHeader("Authorization");

        if (encabezado != null && encabezado.startsWith(PREFIJO_BEARER)) {
            try {
                Claims claims = jwtService.parseClaims(encabezado.substring(PREFIJO_BEARER.length()));
                Long personaId = jwtService.extraerId(claims);

                // El rol que manda es el de la base, no el del token: si la cuenta se eliminó, se deshabilitó o
                // cambió de rol, el token deja de servir enseguida y no cuando vence.
                Optional<String> rolVigente = verificadorDeSesion.rolVigente(personaId, jwtService.extraerRol(claims));
                if (rolVigente.isPresent()) {
                    var authentication = new UsernamePasswordAuthenticationToken(personaId, null,
                            autoridadesDe(personaId, rolVigente.get()));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    SecurityContextHolder.clearContext();
                }
            } catch (JwtException | IllegalArgumentException e) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * El rol, que es lo que miran las reglas por ruta, y además los permisos configurados para ese rol (CU75),
     * que es lo que mira {@link ControlDePermisosFilter}. Los permisos se leen en cada pedido, igual que el rol,
     * para que un cambio en la matriz valga enseguida y no al vencer el token.
     */
    private List<GrantedAuthority> autoridadesDe(Long personaId, String rol) {
        List<GrantedAuthority> autoridades = new ArrayList<>();
        autoridades.add(new SimpleGrantedAuthority("ROLE_" + rol));

        ConsultaDePermisos consulta = consultaDePermisos == null ? null : consultaDePermisos.getIfAvailable();
        if (consulta != null) {
            consulta.permisosDe(personaId, rol).stream()
                    .map(CodigoDePermiso::comoAutoridad)
                    .map(SimpleGrantedAuthority::new)
                    .forEach(autoridades::add);
        }
        return autoridades;
    }
}
