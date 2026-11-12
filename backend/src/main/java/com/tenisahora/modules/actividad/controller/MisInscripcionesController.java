package com.tenisahora.modules.actividad.controller;

import com.tenisahora.modules.actividad.dto.BajaDeInscripcionResponse;
import com.tenisahora.modules.actividad.dto.InscripcionDelSocioResponse;
import com.tenisahora.modules.actividad.service.ConsultaDeInscripciones;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/actividades/inscripciones", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
@Tag(name = "Inscripciones a actividades", description = "Situación de pago e historial de las inscripciones a clases y entrenamientos.")
@SecurityRequirement(name = "bearerAuth")
public class MisInscripcionesController {

    private final ConsultaDeInscripciones consulta;

    @GetMapping("/mias")
    @PreAuthorize("hasRole('SOCIO')")
    @Operation(summary = "Mis inscripciones", description = "Todas, también las canceladas: es el historial del socio.")
    public List<InscripcionDelSocioResponse> mias(Authentication autenticacion) {
        return consulta.delSocio(ActorDeSesion.de(autenticacion).id());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SOCIO', 'EMPLEADO', 'ADMINISTRADOR')")
    @Operation(summary = "Ver una inscripción con su seña y su saldo")
    public InscripcionDelSocioResponse detalle(@PathVariable @Positive Long id, Authentication autenticacion) {
        return consulta.detalle(id, ActorDeSesion.de(autenticacion));
    }

    @GetMapping("/{id}/baja")
    @PreAuthorize("hasAnyRole('SOCIO', 'EMPLEADO', 'ADMINISTRADOR')")
    @Operation(summary = "Previsualizar la baja", description = "Qué se devuelve o se pierde si se da de baja ahora.")
    public BajaDeInscripcionResponse baja(@PathVariable @Positive Long id, Authentication autenticacion) {
        return consulta.baja(id, ActorDeSesion.de(autenticacion));
    }
}
