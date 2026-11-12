package com.tenisahora.modules.reserva.controller;

import com.tenisahora.shared.seguridad.ActorDeSesion;
import com.tenisahora.modules.reserva.dto.ReservaRequest;
import com.tenisahora.modules.reserva.dto.ReservaResponse;
import com.tenisahora.modules.reserva.dto.ReservaUpdateRequest;
import com.tenisahora.modules.reserva.entity.EstadoReserva;
import com.tenisahora.modules.reserva.service.ReservaService;
import com.tenisahora.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;

/** Los errores comunes (400, 401, 403, 404) los agrega OpenApiConfig: acá solo se describe lo propio de cada endpoint. */
@RestController
@RequestMapping("/api/reservas")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAnyRole('SOCIO', 'EMPLEADO', 'ADMINISTRADOR')")
@Tag(name = "Reservas", description = "Alta, consulta y modificación de reservas de cancha (CU16, CU17, consultar mis reservas).")
@SecurityRequirement(name = "bearerAuth")
public class ReservaController {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    private final ReservaService reservaService;

    @PostMapping(consumes = JSON, produces = JSON)
    @Operation(summary = "Registrar reserva (CU16)",
            description = "RF16. Nace pendiente de pago y retiene el horario hasta que se pague la seña o venza. Un socio reserva para sí mismo; "
                    + "el personal indica el socio titular. Validaciones: hasta 30 días de antelación, 1 o 2 horas, 2 o 4 jugadores, "
                    + "horario libre y sin mantenimiento. El total y la seña los calcula el servidor.")
    @ApiResponse(responseCode = "201", description = "Reserva creada. El encabezado Location apunta a su consulta")
    @ApiResponse(responseCode = "409", description = "El horario ya está reservado o la cancha está en mantenimiento")
    @ApiResponse(responseCode = "422", description = "Más de 30 días de antelación, fecha pasada, fuera del horario de la cancha, cancha o titular inválidos")
    public ResponseEntity<ReservaResponse> registrar(@Valid @RequestBody ReservaRequest request, Authentication autenticacion) {
        ReservaResponse creada = reservaService.registrar(request, ActorDeSesion.de(autenticacion));
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(creada.id()).toUri();
        return ResponseEntity.created(ubicacion).cacheControl(CacheControl.noStore()).body(creada);
    }

    @GetMapping(value = "/mis-reservas", produces = JSON)
    @PreAuthorize("hasRole('SOCIO')")
    @Operation(summary = "Consultar mis reservas",
            description = "Las reservas donde el socio es titular o jugador registrado. proximas=true las de hoy en adelante, "
                    + "proximas=false las anteriores; sin el parámetro, todas, las más recientes primero.")
    public ResponseEntity<PageResponse<ReservaResponse>> misReservas(
            @Parameter(description = "true: próximas; false: pasadas") @RequestParam(required = false) Boolean proximas,
            @Parameter(description = "Filtra por estado") @RequestParam(required = false) EstadoReserva estado,
            @Parameter(description = "Página, desde 0") @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página no puede ser negativa") int page,
            @Parameter(description = "Tamaño de página, de 1 a 50") @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "El tamaño mínimo es 1") @Max(value = 50, message = "El tamaño máximo es 50") int size,
            Authentication autenticacion) {
        return sinCache(reservaService.misReservas(proximas, estado, page, size, ActorDeSesion.de(autenticacion)));
    }

    @GetMapping(produces = JSON)
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR')")
    @Operation(summary = "Buscar reservas (CU17)",
            description = "RF17. Por documento del titular o número de reserva (R-000123), con filtros por estado, fecha y cancha. "
                    + "Sin criterio lista todas, las más recientes primero. Solo personal.")
    public ResponseEntity<PageResponse<ReservaResponse>> buscar(
            @Parameter(description = "Documento del titular", example = "30123456") @RequestParam(required = false) @Size(max = 30) String documento,
            @Parameter(description = "Número de reserva", example = "R-000123") @RequestParam(required = false) @Size(max = 30) String numero,
            @RequestParam(required = false) EstadoReserva estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) @Positive Long canchaId,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página no puede ser negativa") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "El tamaño mínimo es 1") @Max(value = 50, message = "El tamaño máximo es 50") int size) {
        return sinCache(reservaService.buscar(documento, numero, estado, fecha, canchaId, page, size));
    }

    @GetMapping(value = "/{id}", produces = JSON)
    @Operation(summary = "Consultar reserva", description = "El personal ve cualquiera; un socio solo las suyas: una ajena responde 404.")
    public ResponseEntity<ReservaResponse> consultar(@PathVariable @Positive Long id, Authentication autenticacion) {
        return sinCache(reservaService.consultar(id, ActorDeSesion.de(autenticacion)));
    }

    @PatchMapping(value = "/{id}", consumes = JSON, produces = JSON)
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR')")
    @Operation(summary = "Modificar reserva (CU17)",
            description = "RF17. Parcial: cambia cancha, fecha u horario, verifica la disponibilidad y recalcula el monto. Solo personal.")
    @ApiResponse(responseCode = "200", description = "Reserva modificada")
    @ApiResponse(responseCode = "409", description = "El nuevo horario ya está reservado o la cancha está en mantenimiento")
    @ApiResponse(responseCode = "422", description = "La reserva está cancelada o finalizada, o el nuevo turno incumple alguna regla")
    public ResponseEntity<ReservaResponse> modificar(@PathVariable @Positive Long id, @Valid @RequestBody ReservaUpdateRequest request,
                                                     Authentication autenticacion) {
        return sinCache(reservaService.modificar(id, request, ActorDeSesion.de(autenticacion)));
    }

    /** Las reservas tienen datos personales: no se guardan en cachés. */
    private <T> ResponseEntity<T> sinCache(T cuerpo) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(cuerpo);
    }
}
