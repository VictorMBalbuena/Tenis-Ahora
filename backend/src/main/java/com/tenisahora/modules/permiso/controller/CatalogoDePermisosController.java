package com.tenisahora.modules.permiso.controller;

import com.tenisahora.modules.permiso.dto.CatalogoDePermisosResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Qué se puede marcar en la matriz de permisos. La pantalla arma la grilla con esto (CU75). */
@RestController
@RequestMapping("/api/permisos")
@PreAuthorize("hasRole('ADMINISTRADOR')")
@Tag(name = "Roles y permisos", description = "Creación de roles y configuración de permisos por rol (CU75, CU85)")
@SecurityRequirement(name = "bearerAuth")
public class CatalogoDePermisosController {

    @GetMapping(value = "/catalogo", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Catálogo de permisos (CU75)",
            description = "Los módulos, las cuatro acciones y los permisos especiales que nombra RF75.")
    public ResponseEntity<CatalogoDePermisosResponse> catalogo() {
        return ResponseEntity.ok(CatalogoDePermisosResponse.armar());
    }
}
