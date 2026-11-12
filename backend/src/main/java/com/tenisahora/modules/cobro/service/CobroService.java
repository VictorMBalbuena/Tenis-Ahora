package com.tenisahora.modules.cobro.service;

import com.tenisahora.modules.cobro.dto.CobroRequest;
import com.tenisahora.modules.cobro.dto.CobroResponse;
import com.tenisahora.modules.cobro.dto.ReciboResponse;
import com.tenisahora.modules.cobro.entity.ConceptoCobro;
import com.tenisahora.modules.cobro.entity.MedioPago;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.shared.dto.PageResponse;

import java.time.LocalDate;

public interface CobroService {

    /** CU51: cobra la seña de la reserva con el medio elegido. Si queda pagada, emite el recibo y confirma la reserva. */
    CobroResponse registrar(Long reservaId, CobroRequest request, Actor actor);

    /** Un cobro que el actor puede ver: el personal cualquiera, un socio solo los de una reserva donde juega. */
    CobroResponse consultar(Long id, Actor actor);

    /** CU57: el recibo ya emitido de un cobro pagado. 404 si todavía no se emitió. */
    ReciboResponse recibo(Long id, Actor actor);

    /**
     * CU54: listado paginado de cobros, solo personal.
     *
     * {@code busqueda} acepta el código del cobro (C-000123) o el número de la reserva (R-000123); cualquier otro
     * formato responde 400. Buscar por torneo o por número de recibo no está soportado todavía.
     */
    PageResponse<CobroResponse> buscar(String busqueda, ConceptoCobro concepto, MedioPago medioPago, LocalDate desde,
                                        LocalDate hasta, int page, int size);

    /** El registro de cobros de un socio, los más recientes primero. */
    PageResponse<CobroResponse> delSocio(Long socioId, int page, int size);

    /** CU56: comprobante del cobro en PDF. */
    byte[] imprimir(Long id, Actor actor);

    /**
     * CU59: listado paginado de recibos, solo personal.
     *
     * {@code busqueda} acepta el número de recibo (R-000123), que es el id del cobro que lo originó.
     */
    PageResponse<ReciboResponse> buscarRecibos(String busqueda, LocalDate desde, LocalDate hasta, int page, int size);
}
