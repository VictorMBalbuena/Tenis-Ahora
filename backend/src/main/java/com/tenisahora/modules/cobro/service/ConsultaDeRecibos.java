package com.tenisahora.modules.cobro.service;

import com.tenisahora.infrastructure.pdf.PdfService;
import com.tenisahora.infrastructure.pdf.PdfService.CampoFicha;
import com.tenisahora.modules.cobro.dto.ReciboResponse;
import com.tenisahora.modules.cobro.entity.Recibo;
import com.tenisahora.modules.cobro.mapper.CobroMapper;
import com.tenisahora.modules.cobro.repository.ReciboRepository;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

/** Consulta e impresión de un recibo por su propio id (CU59, CU61). */
@Service
@RequiredArgsConstructor
public class ConsultaDeRecibos {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ReciboRepository reciboRepository;
    private final CobroMapper cobroMapper;
    private final PdfService pdfService;

    @Transactional(readOnly = true)
    public ReciboResponse consultar(Long id) {
        return cobroMapper.aRespuesta(buscar(id));
    }

    @Transactional(readOnly = true)
    public byte[] imprimir(Long id) {
        return pdfService.generarFicha("Recibo de pago", fichaDe(buscar(id)));
    }

    private Recibo buscar(Long id) {
        return reciboRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(CodigoError.RECIBO_NO_ENCONTRADO, "No existe ese recibo"));
    }

    private List<CampoFicha> fichaDe(Recibo recibo) {
        return List.of(
                new CampoFicha("Número de recibo", recibo.getNumeroRecibo()),
                new CampoFicha("Cobro", recibo.getCobro().getCodigo()),
                new CampoFicha("Estado", recibo.getEstado().toString()),
                new CampoFicha("Concepto", recibo.getConcepto()),
                new CampoFicha("Medio de pago", recibo.getMedioPago().toString()),
                new CampoFicha("Número de operación", recibo.getNumeroOperacion()),
                new CampoFicha("Monto", recibo.getMonto().toString()),
                new CampoFicha("Detalle", recibo.getObservaciones()),
                new CampoFicha("Fecha de emisión", recibo.getFechaEmision().format(FORMATO_FECHA)));
    }
}
