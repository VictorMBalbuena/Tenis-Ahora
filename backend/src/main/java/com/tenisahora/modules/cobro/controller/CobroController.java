package com.tenisahora.modules.cobro.controller;

import com.tenisahora.modules.cobro.dto.AnulacionRequest;
import com.tenisahora.modules.cobro.dto.CobroResponse;
import com.tenisahora.modules.cobro.dto.QrResponse;
import com.tenisahora.modules.cobro.dto.ReciboResponse;
import com.tenisahora.modules.cobro.service.AnulacionDeCobros;
import com.tenisahora.modules.cobro.service.CobroService;
import com.tenisahora.modules.cobro.service.QrDeCobro;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cobros/{id}")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAnyRole('SOCIO', 'EMPLEADO', 'ADMINISTRADOR')")
@Tag(name = "Cobros", description = "Cobro de la seña de una reserva, QR de Mercado Pago y recibo (CU51, CU57, CU77, CU88).")
@SecurityRequirement(name = "bearerAuth")
public class CobroController {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    private final CobroService cobroService;
    private final QrDeCobro qrDeCobro;
    private final AnulacionDeCobros anulacionDeCobros;

    @GetMapping(produces = JSON)
    @Operation(summary = "Ver un cobro", description = "El personal ve cualquiera; un socio solo los de una reserva donde juega.")
    public ResponseEntity<CobroResponse> consultar(@PathVariable @Positive Long id, Authentication autenticacion) {
        return sinCache(cobroService.consultar(id, ActorDeSesion.de(autenticacion)));
    }

    @GetMapping(value = "/recibo", produces = JSON)
    @Operation(summary = "Ver el recibo de un cobro (CU57)")
    @ApiResponse(responseCode = "404", description = "El cobro todavía no tiene recibo emitido")
    public ResponseEntity<ReciboResponse> recibo(@PathVariable @Positive Long id, Authentication autenticacion) {
        return sinCache(cobroService.recibo(id, ActorDeSesion.de(autenticacion)));
    }

    @GetMapping(value = "/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Comprobante del cobro en PDF (CU56)")
    public ResponseEntity<byte[]> pdf(@PathVariable @Positive Long id, Authentication autenticacion) {
        byte[] pdf = cobroService.imprimir(id, ActorDeSesion.de(autenticacion));
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("cobro-" + id + ".pdf").build().toString())
                .body(pdf);
    }

    @PostMapping(value = "/qr", produces = JSON)
    @Operation(summary = "Generar el QR de pago (CU77, CU88)",
            description = "Para un cobro con medio Mercado Pago, pendiente, vencido o rechazado: pide una orden nueva a la pasarela.")
    @ApiResponse(responseCode = "422", description = "El cobro no es de Mercado Pago o ya está pagado")
    @ApiResponse(responseCode = "503", description = "La pasarela de pagos no contestó")
    public ResponseEntity<QrResponse> qr(@PathVariable @Positive Long id, Authentication autenticacion) {
        return sinCache(qrDeCobro.generar(id, ActorDeSesion.de(autenticacion)));
    }

    @PostMapping(value = "/verificacion", produces = JSON)
    @Operation(summary = "Verificar el pago del QR", description = "Consulta a Mercado Pago y acredita el cobro si ya se pagó. Lo usa la pantalla del QR mientras espera.")
    @ApiResponse(responseCode = "503", description = "La pasarela de pagos no contestó")
    public ResponseEntity<CobroResponse> verificar(@PathVariable @Positive Long id, Authentication autenticacion) {
        return sinCache(qrDeCobro.verificar(id, ActorDeSesion.de(autenticacion)));
    }

    @PostMapping(value = "/anulacion", consumes = JSON, produces = JSON)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Anular un cobro (CU55)", description = "Nunca se borra: queda con motivo, fecha y responsable, y su recibo queda anulado. Si era la seña de una reserva confirmada, la reserva vuelve a pendiente.")
    @ApiResponse(responseCode = "422", description = "El cobro ya no se puede anular")
    public ResponseEntity<CobroResponse> anular(@PathVariable @Positive Long id, @Valid @RequestBody AnulacionRequest request, Authentication autenticacion) {
        return sinCache(anulacionDeCobros.anular(id, request.motivo(), ActorDeSesion.de(autenticacion)));
    }

    /** Son datos de dinero y de una persona: nunca se cachean. */
    private <T> ResponseEntity<T> sinCache(T cuerpo) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(cuerpo);
    }
}
