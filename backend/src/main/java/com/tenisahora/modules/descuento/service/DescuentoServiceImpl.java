package com.tenisahora.modules.descuento.service;

import com.tenisahora.infrastructure.pdf.PdfService;
import com.tenisahora.modules.descuento.dto.DescuentoRequest;
import com.tenisahora.modules.descuento.dto.DescuentoResponse;
import com.tenisahora.modules.descuento.entity.Descuento;
import com.tenisahora.modules.descuento.entity.TipoDescuento;
import com.tenisahora.modules.descuento.model.CodigoDeDescuento;
import com.tenisahora.modules.descuento.repository.CobroDescuentoRepository;
import com.tenisahora.modules.descuento.repository.DescuentoRepository;
import com.tenisahora.modules.descuento.repository.DescuentoSpecs;
import com.tenisahora.shared.dto.PageResponse;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.ConflictoException;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import com.tenisahora.shared.exception.SolicitudInvalidaException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DescuentoServiceImpl implements DescuentoService {

    private final DescuentoRepository descuentoRepository;
    private final CobroDescuentoRepository cobroDescuentoRepository;
    private final PdfService pdfService;

    @Override
    @Transactional
    public DescuentoResponse registrar(DescuentoRequest request) {
        validarBeneficio(request);
        String codigo = CodigoDeDescuento.exigir(request.codigo());
        if (descuentoRepository.existsByCodigoIgnoreCase(codigo)) {
            throw new ConflictoException(CodigoError.DESCUENTO_DUPLICADO,
                    "Ya existe un descuento con el código " + codigo);
        }

        boolean activo = Boolean.TRUE.equals(request.activo());
        Descuento descuento = new Descuento();
        descuento.setCodigo(codigo);
        descuento.setTipo(request.tipo());
        descuento.setPorcentaje(request.porcentaje());
        descuento.setMontoFijo(request.montoFijo());
        descuento.setCondiciones(request.condiciones());
        descuento.setActivo(activo);

        Descuento guardado = descuentoRepository.save(descuento);
        return mapear(guardado, 0L);
    }

    @Override
    public DescuentoResponse consultar(Long id) {
        Descuento descuento = obtenerO404(id);
        long vecesAplicado = cobroDescuentoRepository.countByDescuentoId(id);
        return mapear(descuento, vecesAplicado);
    }

    @Override
    public PageResponse<DescuentoResponse> buscar(TipoDescuento tipo, Boolean activo, String busqueda, int page, int size) {
        List<Specification<Descuento>> filtros = new ArrayList<>();
        if (tipo != null) {
            filtros.add(DescuentoSpecs.porTipo(tipo));
        }
        if (activo != null) {
            filtros.add(DescuentoSpecs.activo(activo));
        }
        if (busqueda != null && !busqueda.isBlank()) {
            filtros.add(DescuentoSpecs.condicionesContiene(busqueda));
        }

        Page<Descuento> resultado = descuentoRepository.findAll(Specification.allOf(filtros),
                PageRequest.of(page, size, Sort.by("id").ascending()));
        return PageResponse.de(resultado, d -> mapear(d, cobroDescuentoRepository.countByDescuentoId(d.getId())));
    }

    @Override
    @Transactional
    public DescuentoResponse modificar(Long id, DescuentoRequest request) {
        Descuento descuento = obtenerO404(id);
        validarBeneficio(request);
        String codigo = CodigoDeDescuento.exigir(request.codigo());
        if (descuentoRepository.existsByCodigoIgnoreCaseAndIdNot(codigo, id)) {
            throw new ConflictoException(CodigoError.DESCUENTO_DUPLICADO,
                    "Ya existe un descuento con el código " + codigo);
        }

        boolean activo = Boolean.TRUE.equals(request.activo());
        descuento.setCodigo(codigo);
        descuento.setTipo(request.tipo());
        descuento.setPorcentaje(request.porcentaje());
        descuento.setMontoFijo(request.montoFijo());
        descuento.setCondiciones(request.condiciones());
        descuento.setActivo(activo);

        Descuento guardado = descuentoRepository.save(descuento);
        long vecesAplicado = cobroDescuentoRepository.countByDescuentoId(id);
        return mapear(guardado, vecesAplicado);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Descuento descuento = obtenerO404(id);
        if (cobroDescuentoRepository.existsByDescuentoId(id)) {
            throw new ConflictoException(CodigoError.EN_USO,
                    "No se puede eliminar el descuento porque ya fue aplicado a cobros");
        }
        descuentoRepository.delete(descuento);
    }

    @Override
    public byte[] imprimir(Long id) {
        Descuento descuento = obtenerO404(id);
        long vecesAplicado = cobroDescuentoRepository.countByDescuentoId(id);

        String beneficio = descuento.getPorcentaje() != null
                ? descuento.getPorcentaje() + "%"
                : "$" + descuento.getMontoFijo();

        List<PdfService.CampoFicha> campos = List.of(
                new PdfService.CampoFicha("Código", descuento.getCodigo()),
                new PdfService.CampoFicha("Tipo", descuento.getTipo().name()),
                new PdfService.CampoFicha("Beneficio", beneficio),
                new PdfService.CampoFicha("Condiciones", descuento.getCondiciones()),
                new PdfService.CampoFicha("Estado", Boolean.TRUE.equals(descuento.getActivo()) ? "Activo" : "Inactivo"),
                new PdfService.CampoFicha("Veces aplicado", String.valueOf(vecesAplicado))
        );

        return pdfService.generarFicha("Ficha de Descuento", campos);
    }

    private void validarBeneficio(DescuentoRequest request) {
        BigDecimal porcentaje = request.porcentaje();
        BigDecimal montoFijo = request.montoFijo();

        if ((porcentaje != null && montoFijo != null) || (porcentaje == null && montoFijo == null)) {
            throw new SolicitudInvalidaException(CodigoError.BENEFICIO_INVALIDO,
                    "Debe especificar exactamente uno: porcentaje o monto fijo");
        }

        if (porcentaje != null) {
            if (porcentaje.compareTo(BigDecimal.ZERO) <= 0 || porcentaje.compareTo(new BigDecimal("100")) > 0) {
                throw new SolicitudInvalidaException(CodigoError.BENEFICIO_INVALIDO,
                        "El porcentaje debe estar entre 0.01 y 100");
            }
        }

        if (montoFijo != null) {
            if (montoFijo.compareTo(BigDecimal.ZERO) <= 0) {
                throw new SolicitudInvalidaException(CodigoError.BENEFICIO_INVALIDO,
                        "El monto fijo debe ser mayor a 0");
            }
        }
    }

    private Descuento obtenerO404(Long id) {
        return descuentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(CodigoError.NO_ENCONTRADO, "Descuento no encontrado"));
    }

    private DescuentoResponse mapear(Descuento d, long vecesAplicado) {
        return new DescuentoResponse(
                d.getId(),
                d.getCodigo(),
                d.getTipo(),
                d.getPorcentaje(),
                d.getMontoFijo(),
                d.getCondiciones(),
                d.getActivo(),
                vecesAplicado
        );
    }
}
