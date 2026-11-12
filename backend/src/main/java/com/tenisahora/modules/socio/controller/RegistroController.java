package com.tenisahora.modules.socio.controller;

import com.tenisahora.modules.socio.dto.RegistroSocioRequest;
import com.tenisahora.modules.socio.dto.SocioResponse;
import com.tenisahora.modules.socio.service.SocioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Autorregistro de socios. Vive bajo /api/auth, que es público, igual que el login. */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Registro", description = "Autorregistro de socios (CU74). No pide sesión.")
public class RegistroController {

    private final SocioService socioService;

    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Registrarse como socio (CU74)",
            description = "RF73. Crea la cuenta como socio (cualquier rol que se envíe se ignora). No inicia sesión: después hay que usar el login.")
    @ApiResponse(responseCode = "201", description = "Cuenta creada")
    @ApiResponse(responseCode = "409", description = "El DNI o el correo ya están registrados")
    @ApiResponse(responseCode = "422", description = "La contraseña supera los 72 bytes")
    public ResponseEntity<SocioResponse> registrarse(@Valid @RequestBody RegistroSocioRequest request) {
        return ResponseEntity.status(201).cacheControl(CacheControl.noStore())
                .body(socioService.registrarse(request));
    }
}
