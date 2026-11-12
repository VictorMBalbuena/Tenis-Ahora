package com.tenisahora.modules.reporte.controller;

import com.tenisahora.modules.reporte.dto.ReporteAsistenciasResponse;
import com.tenisahora.modules.reporte.dto.ReporteIngresosResponse;
import com.tenisahora.modules.reporte.dto.ReporteReservasResponse;
import com.tenisahora.modules.reporte.service.ReporteAsistenciasService;
import com.tenisahora.modules.reporte.service.ReporteIngresosService;
import com.tenisahora.modules.reporte.service.ReporteReservasService;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.SolicitudInvalidaException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRADOR')")
@Tag(name = "Reportes", description = "Reportes de ingresos, asistencias y reservas (FRONT 15).")
@SecurityRequirement(name = "bearerAuth")
public class ReporteController {

    private final ReporteReservasService reservas;
    private final ReporteAsistenciasService asistencias;
    private final ReporteIngresosService ingresos;

    @GetMapping("/reservas")
    @Operation(summary = "Reporte de reservas por cancha, estado, ocupación y cancelaciones")
    public ResponseEntity<ReporteReservasResponse> reservas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String cancha) {
        validarRango(desde, hasta);
        return sinCache(reservas.generar(desde, hasta, cancha));
    }

    @GetMapping("/asistencias")
    @Operation(summary = "Reporte de asistencias a clases y entrenamientos")
    public ResponseEntity<ReporteAsistenciasResponse> asistencias(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String actividad) {
        validarRango(desde, hasta);
        return sinCache(asistencias.generar(desde, hasta, actividad));
    }

    @GetMapping("/ingresos")
    @Operation(summary = "Reporte de ingresos por concepto, medio de pago y descuentos")
    public ResponseEntity<ReporteIngresosResponse> ingresos(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String concepto) {
        validarRango(desde, hasta);
        return sinCache(ingresos.generar(desde, hasta, concepto));
    }

    private void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde.isAfter(hasta)) {
            throw new SolicitudInvalidaException(CodigoError.RANGO_DE_CONSULTA_INVALIDO, "La fecha 'desde' no puede ser posterior a 'hasta'");
        }
    }

    private <T> ResponseEntity<T> sinCache(T cuerpo) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(cuerpo);
    }
}
