package com.tenisahora.modules.perfil.controller;

import com.tenisahora.modules.perfil.dto.CambiarPasswordRequest;
import com.tenisahora.modules.perfil.dto.FotoResponse;
import com.tenisahora.modules.perfil.dto.MiPerfilUpdateRequest;
import com.tenisahora.modules.perfil.dto.PreferenciasDeNotificacion;
import com.tenisahora.modules.perfil.service.MiPerfilService;
import com.tenisahora.modules.perfil.service.PreferenciasDeNotificacionService;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.shared.dto.MiPerfilResponse;
import com.tenisahora.shared.seguridad.ActorDeSesion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Perfil y configuración de la persona autenticada: todo cuelga de /me y nunca lleva id, el backend sabe quién es
 * por el token. Sin allowedRoles a propósito: cualquiera que inicie sesión entra a lo suyo.
 */
@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
@Tag(name = "Mi perfil", description = "Datos propios, foto, contraseña y preferencias de notificaciones de la persona autenticada.")
@SecurityRequirement(name = "bearerAuth")
public class MiPerfilController {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    private final MiPerfilService servicio;
    private final PreferenciasDeNotificacionService preferencias;

    @GetMapping(produces = JSON)
    @Operation(summary = "Consultar mi perfil", description = "Datos de Persona más el rol y, si es socio, su número.")
    public ResponseEntity<MiPerfilResponse> consultar(Authentication authentication) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servicio.consultar(actor(authentication)));
    }

    @PatchMapping(consumes = JSON, produces = JSON)
    @Operation(summary = "Modificar mi perfil", description = "Parcial: solo se cambian los campos enviados. El DNI y el rol no se pueden tocar.")
    @ApiResponse(responseCode = "409", description = "El correo ya pertenece a otra persona")
    public ResponseEntity<MiPerfilResponse> modificar(@Valid @RequestBody MiPerfilUpdateRequest request,
                                                      Authentication authentication) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(servicio.actualizar(actor(authentication), request));
    }

    @PutMapping(value = "/password", consumes = JSON)
    @Operation(summary = "Cambiar mi contraseña", description = "Pide la actual para que no la cambie quien se haya dejado la sesión abierta.")
    @ApiResponse(responseCode = "204", description = "Contraseña cambiada")
    @ApiResponse(responseCode = "422", description = "La contraseña actual no es correcta, o la nueva es igual a la actual")
    public ResponseEntity<Void> cambiarPassword(@Valid @RequestBody CambiarPasswordRequest request, Authentication authentication) {
        servicio.cambiarPassword(actor(authentication), request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/foto", produces = JSON)
    @Operation(summary = "Cambiar mi foto", description = "multipart/form-data con un solo campo, archivo: JPG, PNG o WebP, hasta 2 MB.")
    @ApiResponse(responseCode = "422", description = "La imagen no tiene un formato válido o pesa más de 2 MB")
    public ResponseEntity<FotoResponse> subirFoto(@RequestParam("archivo") MultipartFile archivo, Authentication authentication) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(servicio.subirFoto(actor(authentication), archivo));
    }

    @DeleteMapping("/foto")
    @Operation(summary = "Quitar mi foto", description = "Vuelve a la inicial del nombre: deja fotoUrl en null.")
    @ApiResponse(responseCode = "204", description = "Foto eliminada")
    public ResponseEntity<Void> quitarFoto(Authentication authentication) {
        servicio.quitarFoto(actor(authentication));
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/preferencias", produces = JSON)
    @Operation(summary = "Consultar mis preferencias de notificaciones")
    public ResponseEntity<PreferenciasDeNotificacion> consultarPreferencias(Authentication authentication) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(preferencias.obtener(actor(authentication).id()));
    }

    @PutMapping(value = "/preferencias", consumes = JSON, produces = JSON)
    @Operation(summary = "Guardar mis preferencias de notificaciones", description = "Recibe los tres booleanos y devuelve cómo quedaron.")
    public ResponseEntity<PreferenciasDeNotificacion> guardarPreferencias(@Valid @RequestBody PreferenciasDeNotificacion request,
                                                                          Authentication authentication) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(preferencias.guardar(actor(authentication).id(), request));
    }

    private Actor actor(Authentication authentication) {
        return ActorDeSesion.de(authentication);
    }
}
