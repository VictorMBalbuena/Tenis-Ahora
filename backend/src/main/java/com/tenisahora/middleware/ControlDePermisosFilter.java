package com.tenisahora.middleware;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tenisahora.modules.permiso.model.AccionSobreModulo;
import com.tenisahora.modules.permiso.model.CodigoDePermiso;
import com.tenisahora.modules.permiso.model.ModuloDelSistema;
import com.tenisahora.modules.permiso.model.PermisoEspecial;
import com.tenisahora.shared.dto.ErrorResponse;
import com.tenisahora.security.ConsultaDePermisos;
import com.tenisahora.shared.exception.CodigoError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Hace valer la matriz de permisos (CU75). Corre después del filtro JWT, que ya dejó en la sesión las autoridades
 * {@code PERM_...} del rol de la persona.
 *
 * <p>Se suma a las reglas por ruta de {@code SecurityConfig}, no las reemplaza: este filtro solo puede <b>negar</b>
 * lo que la matriz no permite, nunca habilitar algo que la ruta ya cortaba. Por eso activarlo con la matriz que
 * trae el sistema no le cambia el acceso a nadie: recién empieza a notarse cuando un administrador destilda algo.
 *
 * <p>Un pedido que no cae en ningún módulo (por ejemplo {@code /api/me} o {@code /api/auth}) pasa sin que este
 * filtro opine: esos no son permisos configurables, son de cualquiera que tenga sesión.
 */
@Component
public class ControlDePermisosFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    /**
     * Sin el módulo de permisos no hay matriz que hacer valer, y este filtro no opina. Es lo que pasa en una
     * prueba de un controlador suelto, donde la seguridad se comprueba con las reglas por ruta.
     */
    private final ObjectProvider<ConsultaDePermisos> consultaDePermisos;

    public ControlDePermisosFilter(ObjectMapper objectMapper, ObjectProvider<ConsultaDePermisos> consultaDePermisos) {
        this.objectMapper = objectMapper;
        this.consultaDePermisos = consultaDePermisos;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();

        if (autenticacion == null || !autenticacion.isAuthenticated() || consultaDePermisos.getIfAvailable() == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String ruta = request.getRequestURI();
        String metodo = request.getMethod();
        Optional<String> exigido = permisoQueExige(metodo, ruta);

        if (exigido.isPresent() && !tiene(autenticacion, exigido.get())) {
            negar(request, response, exigido.get());
            return;
        }
        filterChain.doFilter(request, response);
    }

    /** Qué permiso hace falta para este pedido, si es que cae en algún módulo. */
    private Optional<String> permisoQueExige(String metodo, String ruta) {
        // Los permisos especiales mandan: anular un cobro no es "eliminar en el módulo de cobros", es su propio permiso
        Optional<PermisoEspecial> especial = Arrays.stream(PermisoEspecial.values())
                .filter(permiso -> permiso.loEjerce(metodo, ruta))
                .findFirst();
        if (especial.isPresent()) {
            return especial.map(CodigoDePermiso::de);
        }

        return Arrays.stream(ModuloDelSistema.values())
                .filter(modulo -> modulo.cubre(ruta))
                .findFirst()
                .map(modulo -> CodigoDePermiso.de(modulo, AccionSobreModulo.deMetodo(metodo)));
    }

    private boolean tiene(Authentication autenticacion, String codigo) {
        Set<String> autoridades = autenticacion.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        return autoridades.contains(CodigoDePermiso.comoAutoridad(codigo));
    }

    private void negar(HttpServletRequest request, HttpServletResponse response, String codigo) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ErrorResponse.de(CodigoError.NO_AUTORIZADO,
                "Tu rol no tiene el permiso necesario para esta acción (" + codigo + ")", request.getRequestURI()));
    }
}
