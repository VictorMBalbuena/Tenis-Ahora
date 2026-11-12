package com.tenisahora.config;

import com.tenisahora.shared.exception.CodigoError;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tenisahora.middleware.ControlDePermisosFilter;
import com.tenisahora.middleware.JwtAuthenticationFilter;
import com.tenisahora.shared.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] RUTAS_PUBLICAS = {
            "/api/auth/**",
            // Aviso de Mercado Pago (CU78): no es del front, no lleva JWT. La firma HMAC es su autenticación.
            "/webhooks/mercadopago",
            "/fotos/**",
            "/error",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    /**
     * El control de la matriz de permisos (CU75) es opcional: si no está montado, la seguridad funciona con las
     * reglas por ruta de siempre. Eso deja que las pruebas de un controlador suelto no tengan que levantar el
     * módulo de permisos entero.
     */
    private final ObjectProvider<ControlDePermisosFilter> controlDePermisos;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuracion) throws Exception {
        return configuracion.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(this::autorizarRutas)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                escribirError(request, response, HttpStatus.UNAUTHORIZED, CodigoError.NO_AUTENTICADO, "No autenticado"))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                escribirError(request, response, HttpStatus.FORBIDDEN, CodigoError.NO_AUTORIZADO, "No autorizado")))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // Después del JWT, que es el que deja los permisos de la sesión, y antes de llegar al controlador
        ControlDePermisosFilter control = controlDePermisos.getIfAvailable();
        if (control != null) {
            http.addFilterAfter(control, JwtAuthenticationFilter.class);
        }

        return http.build();
    }

    /** Quién entra a cada ruta. Se corta por ruta antes de leer el cuerpo: sin permiso no se llega ni a validar los datos. */
    private void autorizarRutas(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        autorizarPublicoYPersonal(auth);
        autorizarInstructoresYCanchas(auth);
        autorizarReservasCobrosYTorneos(auth);
        autorizarRutasDeActividades(auth);
        auth.anyRequest().authenticated();
    }

    private void autorizarPublicoYPersonal(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth
                // Única ruta de /api/auth que exige sesión: cierra todos los refresh tokens de la persona, así que
                // tiene que saber quién es por el token, no solo por una cookie.
                .requestMatchers(HttpMethod.POST, "/api/auth/logout-all").authenticated()
                .requestMatchers(RUTAS_PUBLICAS).permitAll()
                .requestMatchers("/api/socios/**", "/api/empleados/**").hasAnyRole("EMPLEADO", "ADMINISTRADOR")
                // Validar un código al reservar es del socio: tiene que ir ANTES de la regla de /api/descuentos/**,
                // porque gana la primera que coincide. El ABM de descuentos sigue siendo solo del administrador.
                .requestMatchers(HttpMethod.GET, "/api/descuentos/codigo/**").hasAnyRole("SOCIO", "EMPLEADO", "ADMINISTRADOR")
                .requestMatchers("/api/administradores/**", "/api/descuentos/**", "/api/reportes/**").hasRole("ADMINISTRADOR")
                // Roles, permisos y administración de cuentas (CU74, CU75, CU76, CU85)
                .requestMatchers("/api/roles/**", "/api/cuentas/**", "/api/permisos/**").hasRole("ADMINISTRADOR")
                .requestMatchers("/api/insumos/**").hasAnyRole("EMPLEADO", "ADMINISTRADOR");
    }

    private void autorizarInstructoresYCanchas(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth
                // La validación del título o la certificación (CU31) es solo del administrador; el resto del CRUD es del personal
                .requestMatchers(HttpMethod.POST, "/api/profesores/*/validacion").hasRole("ADMINISTRADOR")
                .requestMatchers("/api/profesores/**").hasAnyRole("EMPLEADO", "ADMINISTRADOR")
                .requestMatchers(HttpMethod.POST, "/api/entrenadores/*/validacion").hasRole("ADMINISTRADOR")
                .requestMatchers("/api/entrenadores/**").hasAnyRole("EMPLEADO", "ADMINISTRADOR")
                .requestMatchers(HttpMethod.GET, "/api/canchas/*/disponibilidad").hasAnyRole("SOCIO", "EMPLEADO", "ADMINISTRADOR")
                .requestMatchers("/api/canchas/**").hasAnyRole("EMPLEADO", "ADMINISTRADOR");
    }

    private void autorizarReservasCobrosYTorneos(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth
                .requestMatchers(HttpMethod.GET, "/api/reservas/mis-reservas").hasRole("SOCIO")
                .requestMatchers(HttpMethod.GET, "/api/reservas").hasAnyRole("EMPLEADO", "ADMINISTRADOR")
                .requestMatchers(HttpMethod.PATCH, "/api/reservas/*").hasAnyRole("EMPLEADO", "ADMINISTRADOR")
                .requestMatchers(HttpMethod.POST, "/api/reservas/*/inicio", "/api/reservas/*/finalizacion").hasAnyRole("EMPLEADO", "ADMINISTRADOR")
                .requestMatchers("/api/reservas/**").hasAnyRole("SOCIO", "EMPLEADO", "ADMINISTRADOR")
                .requestMatchers(HttpMethod.POST, "/api/cobros/*/anulacion").hasRole("ADMINISTRADOR")
                .requestMatchers("/api/cobros/**").hasAnyRole("SOCIO", "EMPLEADO", "ADMINISTRADOR")
                // Torneos
                .requestMatchers(HttpMethod.POST, "/api/torneos/inscripciones/*/cobros").hasAnyRole("SOCIO", "EMPLEADO", "ADMINISTRADOR")
                .requestMatchers(HttpMethod.POST, "/api/torneos/*/inscripciones").hasAnyRole("SOCIO", "EMPLEADO", "ADMINISTRADOR")
                .requestMatchers(HttpMethod.GET, "/api/torneos/**").hasAnyRole("SOCIO", "EMPLEADO", "ADMINISTRADOR")
                .requestMatchers("/api/torneos/**").hasAnyRole("EMPLEADO", "ADMINISTRADOR");
    }

    /** Clases, entrenamientos y las rutas de /api/actividades que sirven a los dos. */
    private void autorizarRutasDeActividades(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth
                // Clases: lo específico (mis-clases, alta, modificación, baja) se corta por ruta; el resto lo decide @PreAuthorize
                .requestMatchers(HttpMethod.GET, "/api/clases/mis-clases").hasRole("PROFESOR")
                .requestMatchers(HttpMethod.POST, "/api/clases").hasAnyRole("EMPLEADO", "ADMINISTRADOR")
                .requestMatchers(HttpMethod.PATCH, "/api/clases/*").hasAnyRole("EMPLEADO", "ADMINISTRADOR")
                .requestMatchers(HttpMethod.DELETE, "/api/clases/*").hasAnyRole("EMPLEADO", "ADMINISTRADOR")
                .requestMatchers("/api/clases/**").hasAnyRole("SOCIO", "PROFESOR", "EMPLEADO", "ADMINISTRADOR")
                // Entrenamientos (espejo de clases, con el entrenador) y las rutas de /api/actividades que sirven a los dos
                .requestMatchers(HttpMethod.GET, "/api/entrenamientos/mis-entrenamientos").hasRole("ENTRENADOR")
                .requestMatchers(HttpMethod.POST, "/api/entrenamientos").hasAnyRole("EMPLEADO", "ADMINISTRADOR")
                .requestMatchers(HttpMethod.PATCH, "/api/entrenamientos/*").hasAnyRole("EMPLEADO", "ADMINISTRADOR")
                .requestMatchers(HttpMethod.DELETE, "/api/entrenamientos/*").hasAnyRole("EMPLEADO", "ADMINISTRADOR")
                .requestMatchers("/api/entrenamientos/**").hasAnyRole("SOCIO", "ENTRENADOR", "EMPLEADO", "ADMINISTRADOR")
                .requestMatchers("/api/actividades/inscripciones/**").hasAnyRole("SOCIO", "EMPLEADO", "ADMINISTRADOR")
                .requestMatchers("/api/actividades/*/instructor", "/api/actividades/*/instructores-disponibles").hasAnyRole("EMPLEADO", "ADMINISTRADOR")
                .requestMatchers("/api/actividades/**").hasAnyRole("PROFESOR", "ENTRENADOR", "EMPLEADO", "ADMINISTRADOR");
    }

    /** Los errores de seguridad salen con el mismo formato que el resto de la API. */
    private void escribirError(HttpServletRequest request, HttpServletResponse response, HttpStatus estado,
                               CodigoError codigo, String mensaje) throws java.io.IOException {
        response.setStatus(estado.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ErrorResponse.de(codigo, mensaje, request.getRequestURI()));
    }
}
