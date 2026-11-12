package com.tenisahora.modules.cobro.controller;

import com.tenisahora.modules.cobro.dto.CobroResponse;
import com.tenisahora.modules.cobro.service.CobroService;
import com.tenisahora.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cobros/mios")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('SOCIO')")
@Tag(name = "Cobros", description = "El registro de cobros del socio que inició sesión.")
@SecurityRequirement(name = "bearerAuth")
public class MisCobrosController {

    private final CobroService cobroService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Ver mis cobros",
            description = "Los de las reservas donde el socio es titular, sus inscripciones y sus torneos. El socio sale siempre de la sesión.")
    public ResponseEntity<PageResponse<CobroResponse>> mios(
            @Parameter(description = "Página, desde 0") @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página no puede ser negativa") int page,
            @Parameter(description = "Tamaño de página, de 1 a 50") @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "El tamaño mínimo es 1") @Max(value = 50, message = "El tamaño máximo es 50") int size,
            Authentication autenticacion) {
        PageResponse<CobroResponse> pagina = cobroService.delSocio(ActorDeSesion.de(autenticacion).id(), page, size);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(pagina);
    }
}
