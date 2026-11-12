package com.tenisahora.modules.torneo.controller;

import com.tenisahora.modules.socio.repository.SocioRepository;
import com.tenisahora.modules.torneo.dto.CompaneroEncontradoResponse;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Buscar al compañero de pareja por DNI para inscribirlo a un torneo (CU47).
 *
 * <p>Vive en el módulo de torneos y no reusa el buscador de reservas a propósito: así quien se inscribe
 * necesita permiso sobre Torneos, que es lo que ya está haciendo, y no sobre Reservas, que no tiene nada que
 * ver. El DNI es lo único que una persona sabe de otra; el id es interno y no se le pide a nadie.
 */
@RestController
@RequestMapping("/api/torneos/companeros")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAnyRole('SOCIO', 'EMPLEADO', 'ADMINISTRADOR')")
@Tag(name = "Torneos", description = "Búsqueda del compañero de pareja para inscribirse a un torneo.")
@SecurityRequirement(name = "bearerAuth")
public class CompaneroDeTorneoController {

    private final SocioRepository socioRepository;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Buscar un socio activo por DNI exacto (CU47)")
    @ApiResponse(responseCode = "404", description = "No hay un socio activo con ese DNI")
    public CompaneroEncontradoResponse buscar(
            @RequestParam @Pattern(regexp = "\\d{7,8}", message = "El DNI debe tener 7 u 8 números") String dni) {

        return socioRepository.findByDniAndActivoTrue(dni)
                .map(socio -> new CompaneroEncontradoResponse(socio.getId(), socio.getNombre(), socio.getApellido()))
                .orElseThrow(() -> new RecursoNoEncontradoException(CodigoError.PERSONA_NO_ENCONTRADA,
                        "No hay un socio activo con ese DNI"));
    }
}
