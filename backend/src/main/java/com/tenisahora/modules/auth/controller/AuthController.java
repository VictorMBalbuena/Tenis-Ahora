package com.tenisahora.modules.auth.controller;

import com.tenisahora.modules.auth.dto.LoginRequest;
import com.tenisahora.modules.auth.dto.LoginResponse;
import com.tenisahora.modules.auth.service.AuthService;
import com.tenisahora.modules.auth.service.ResultadoDeSesion;
import com.tenisahora.shared.seguridad.ActorDeSesion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Los errores comunes (400, 429) los agrega OpenApiConfig: acá solo se describe lo propio de cada endpoint. */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Inicio y cierre de sesión (CU71 y CU72). El access token viaja en el cuerpo; "
        + "el refresh token, en una cookie httpOnly que el navegador manda solo.")
public class AuthController {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    private final AuthService authService;
    private final CookieDeSesion cookieDeSesion;

    @PostMapping(value = "/login", consumes = JSON, produces = JSON)
    @Operation(summary = "Iniciar sesión (CU71)",
            description = "RF71. Devuelve el access token y deja el refresh token en la cookie httpOnly refresh_token. "
                    + "Tras 5 intentos fallidos seguidos con el mismo correo el ingreso se bloquea 15 minutos (A3), aun con la "
                    + "contraseña correcta.")
    @ApiResponse(responseCode = "200", description = "Sesión iniciada")
    @ApiResponse(responseCode = "401", description = "Credenciales inválidas (contraseña incorrecta, cuenta inexistente o deshabilitada: mismo mensaje)")
    @ApiResponse(responseCode = "429", description = "Cuenta bloqueada por intentos fallidos, o demasiados pedidos desde la misma IP. Retry-After dice cuántos segundos esperar")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return conSesion(authService.login(request));
    }

    @PostMapping(value = "/refresh", consumes = JSON, produces = JSON)
    @Operation(summary = "Renovar la sesión",
            description = "Exige la cookie refresh_token. Entrega un access token nuevo y rota la cookie (el refresh token es de un solo uso). "
                    + "Se pide con Content-Type application/json, así un formulario de otro sitio no puede dispararlo.")
    @ApiResponse(responseCode = "200", description = "Sesión renovada")
    @ApiResponse(responseCode = "401", description = "Falta la cookie, o el refresh token es inválido, venció, ya se usó o la cuenta se deshabilitó")
    public ResponseEntity<LoginResponse> refresh(
            @CookieValue(name = CookieDeSesion.NOMBRE, required = false) String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BadCredentialsException("Falta la cookie del refresh token");
        }
        return conSesion(authService.refresh(refreshToken));
    }

    @PostMapping(value = "/logout", consumes = JSON)
    @Operation(summary = "Cerrar sesión (CU72)",
            description = "RF72. Revoca el refresh token de la cookie y la borra del navegador. Siempre responde 204.")
    @ApiResponse(responseCode = "204", description = "Sesión cerrada")
    public ResponseEntity<Void> logout(
            @CookieValue(name = CookieDeSesion.NOMBRE, required = false) String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            authService.logout(refreshToken);
        }
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieDeSesion.borrar().toString())
                .build();
    }

    @PostMapping(value = "/logout-all")
    @Operation(summary = "Cerrar todas mis sesiones", description = "Revoca todos los refresh tokens de la persona, no solo el de esta pestaña. "
            + "Es para cuando se perdió el teléfono. A diferencia del resto de /api/auth, exige estar autenticada.")
    @ApiResponse(responseCode = "204", description = "Sesiones cerradas")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> logoutAll(Authentication authentication) {
        authService.revocarTodasLasSesiones(ActorDeSesion.de(authentication).id());
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieDeSesion.borrar().toString())
                .build();
    }

    private ResponseEntity<LoginResponse> conSesion(ResultadoDeSesion sesion) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieDeSesion.crear(sesion.refreshToken(), sesion.duracionDelRefresh()).toString())
                .cacheControl(CacheControl.noStore())
                .body(new LoginResponse(sesion.accessToken()));
    }
}
