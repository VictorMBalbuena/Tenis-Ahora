package com.tenisahora.modules.cobro.service;

import com.tenisahora.infrastructure.pdf.PdfService;
import com.tenisahora.infrastructure.pdf.PdfService.CampoFicha;
import com.tenisahora.modules.cobro.dto.CobroRequest;
import com.tenisahora.modules.cobro.dto.CobroResponse;
import com.tenisahora.modules.cobro.dto.ReciboResponse;
import com.tenisahora.modules.cobro.entity.Cobro;
import com.tenisahora.modules.cobro.entity.ConceptoCobro;
import com.tenisahora.modules.cobro.entity.EstadoCobro;
import com.tenisahora.modules.cobro.entity.MedioPago;
import com.tenisahora.modules.cobro.entity.Recibo;
import com.tenisahora.modules.cobro.mapper.CobroMapper;
import com.tenisahora.modules.cobro.repository.CobroRepository;
import com.tenisahora.modules.cobro.repository.CobroSpecs;
import com.tenisahora.modules.cobro.repository.ReciboRepository;
import com.tenisahora.modules.cobro.repository.ReciboSpecs;
import com.tenisahora.modules.cobro.service.pago.DatosDeCobro;
import com.tenisahora.modules.cobro.service.pago.FormasDeCobro;
import com.tenisahora.modules.cobro.service.pago.ResultadoDeCobro;
import com.tenisahora.modules.reserva.entity.Reserva;
import com.tenisahora.modules.reserva.mapper.ReservaMapper;
import com.tenisahora.modules.reserva.service.AccesoAReservas;
import com.tenisahora.modules.reserva.service.ConfirmacionDeReserva;
import com.tenisahora.modules.reserva.service.PagosDeReserva;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.shared.dto.PageResponse;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.ConflictoException;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import com.tenisahora.shared.exception.SolicitudInvalidaException;
import com.tenisahora.shared.util.Textos;
import com.tenisahora.shared.model.Rol;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.function.Function;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class CobroServiceImpl implements CobroService {

    private final CobroRepository cobroRepository;
    private final ReciboRepository reciboRepository;
    private final AccesoAReservas accesoAReservas;
    private final AccesoACobros accesoACobros;
    private final CobroPorId cobroPorId;
    private final CalculadoraDeCobro calculadoraDeCobro;
    private final FormasDeCobro formasDeCobro;
    private final EmisionDeRecibos emisionDeRecibos;
    private final ConfirmacionDeReserva confirmacionDeReserva;
    private final ReemplazoDeQr reemplazoDeQr;
    private final PagosDeReserva pagosDeReserva;
    private final CobroMapper cobroMapper;
    private final PdfService pdfService;

    private static final Pattern CODIGO_DE_COBRO = Pattern.compile("(?i)^C-?0*(\\d{1,18})$");
    /** Mismo formato R-000123 que el número de reserva: el número de recibo es el id del cobro que lo originó. */
    private static final Pattern NUMERO_DE_RESERVA = Pattern.compile("(?i)^R-?0*(\\d{1,18})$");
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    @Transactional
    public CobroResponse registrar(Long reservaId, CobroRequest request, Actor actor) {
        Reserva reserva = accesoAReservas.paraGestionar(reservaId, actor, true);
        asegurarCobrable(reserva);
        asegurarMedioPermitido(request.medioPago(), actor);
        Optional<Cobro> qrEnCurso = cobroRepository.findFirstByReservaIdAndEstadoAndMedioPago(reservaId, EstadoCobro.PENDIENTE, MedioPago.QR);
        if (qrEnCurso.isPresent() && request.medioPago() == MedioPago.QR) {
            return cobroMapper.aRespuesta(qrEnCurso.get());
        }
        ConceptoCobro concepto = reserva.getEstado().puedeCobrarseSenia() ? ConceptoCobro.SEÑA : ConceptoCobro.SALDO;
        qrEnCurso.ifPresent(qr -> reemplazoDeQr.reemplazar(qr, actor));
        reemplazoDeQr.reemplazarRechazados(cobroRepository.findByReservaIdAndEstado(reservaId, EstadoCobro.RECHAZADO), actor);
        BigDecimal montoBase = montoBaseVigente(concepto, reserva);
        Cobro cobro = nuevo(reserva, request.medioPago(), concepto, actor);
        cobroRepository.saveAndFlush(cobro); // para tener id: lo necesitan el código y los descuentos
        completarMontos(cobro, montoBase, reservaId);

        return cobrarYAsentar(cobro, request, reservaId, concepto);
    }

    private CobroResponse cobrarYAsentar(Cobro cobro, CobroRequest request, Long reservaId, ConceptoCobro concepto) {
        ResultadoDeCobro resultado = cobrar(cobro, request);
        cobroRepository.saveAndFlush(cobro);
        if (cobro.getEstado() == EstadoCobro.PAGADO) {
            emisionDeRecibos.emitir(cobro);
            if (concepto == ConceptoCobro.SEÑA) {
                confirmacionDeReserva.confirmar(reservaId);
            }
        }
        log.info("Cobro {} registrado para la reserva {} con medio {}: queda {}", cobro.getCodigo(), reservaId,
                request.medioPago(), cobro.getEstado());
        return cobroMapper.aRespuesta(cobro, resultado.qrData());
    }

    @Override
    @Transactional(readOnly = true)
    public CobroResponse consultar(Long id, Actor actor) {
        Cobro cobro = accesoACobros.visible(cobroPorId.buscar(id), actor);
        return cobroMapper.aDetalle(cobro, reciboRepository.findByCobroId(cobro.getId()).orElse(null));
    }

    @Override
    @Transactional(readOnly = true)
    public ReciboResponse recibo(Long id, Actor actor) {
        Cobro cobro = accesoACobros.visible(cobroPorId.buscar(id), actor);
        Recibo recibo = reciboRepository.findByCobroId(cobro.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException(CodigoError.RECIBO_NO_EMITIDO, "El cobro todavía no tiene recibo"));
        return cobroMapper.aRespuesta(recibo);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CobroResponse> buscar(String busqueda, ConceptoCobro concepto, MedioPago medioPago,
                                               LocalDate desde, LocalDate hasta, int page, int size) {
        asegurarRangoDeFechas(desde, hasta);
        List<Specification<Cobro>> filtros = new ArrayList<>();
        if (hayTexto(busqueda)) {
            filtros.add(filtroDeBusqueda(busqueda));
        }
        if (concepto != null) {
            filtros.add(CobroSpecs.deConcepto(concepto));
        }
        if (medioPago != null) {
            filtros.add(CobroSpecs.deMedioPago(medioPago));
        }
        if (desde != null) {
            filtros.add(CobroSpecs.desde(desde.atStartOfDay()));
        }
        if (hasta != null) {
            filtros.add(CobroSpecs.hasta(hasta.plusDays(1).atStartOfDay()));
        }
        Page<Cobro> cobros = cobroRepository.findAll(Specification.allOf(filtros),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
        return conRecibos(cobros);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CobroResponse> delSocio(Long socioId, int page, int size) {
        return conRecibos(cobroRepository.delSocio(socioId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"))));
    }

    private PageResponse<CobroResponse> conRecibos(Page<Cobro> cobros) {
        Map<Long, Recibo> recibos = reciboRepository.findByCobroIdIn(cobros.map(Cobro::getId).getContent()).stream()
                .collect(Collectors.toMap(recibo -> recibo.getCobro().getId(), Function.identity()));
        return PageResponse.de(cobros, cobro -> cobroMapper.aDetalle(cobro, recibos.get(cobro.getId())));
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] imprimir(Long id, Actor actor) {
        Cobro cobro = accesoACobros.visible(cobroPorId.buscar(id), actor);
        String titulo = actor.rol() == Rol.SOCIO ? "Comprobante de pago" : "Comprobante de cobro";
        return pdfService.generarFicha(titulo, fichaDe(cobro));
    }

    private List<CampoFicha> fichaDe(Cobro cobro) {
        List<CampoFicha> campos = new ArrayList<>();
        campos.add(new CampoFicha("Código", cobro.getCodigo()));
        campos.add(new CampoFicha("Reserva", cobro.getReserva() != null ? ReservaMapper.numeroDe(cobro.getReserva().getId()) : null));
        campos.add(new CampoFicha("Concepto", cobro.getConcepto().toString()));
        campos.add(new CampoFicha("Estado", cobro.getEstado().toString()));
        campos.add(new CampoFicha("Medio de pago", cobro.getMedioPago().toString()));
        campos.add(new CampoFicha("Monto base", cobro.getMontoBase().toString()));
        campos.add(new CampoFicha("Monto final", cobro.getMontoFinal().toString()));
        campos.add(new CampoFicha("Número de operación", cobro.getNumeroOperacion()));
        campos.add(new CampoFicha("Fecha", cobro.getFecha().format(FORMATO_FECHA)));
        return campos;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReciboResponse> buscarRecibos(String busqueda, LocalDate desde, LocalDate hasta, int page, int size) {
        asegurarRangoDeFechas(desde, hasta);
        List<Specification<Recibo>> filtros = new ArrayList<>();
        if (hayTexto(busqueda)) {
            filtros.add(ReciboSpecs.delCobro(cobroIdDeNumeroDeRecibo(busqueda)));
        }
        if (desde != null) {
            filtros.add(ReciboSpecs.desde(desde.atStartOfDay()));
        }
        if (hasta != null) {
            filtros.add(ReciboSpecs.hasta(hasta.plusDays(1).atStartOfDay()));
        }
        Page<Recibo> recibos = reciboRepository.findAll(Specification.allOf(filtros),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
        return PageResponse.de(recibos, cobroMapper::aRespuesta);
    }

    /** El número de recibo (R-000123) es el id del cobro que lo originó; otro formato es un 400. */
    private Long cobroIdDeNumeroDeRecibo(String busqueda) {
        Matcher coincidencia = NUMERO_DE_RESERVA.matcher(Textos.recortar(busqueda));
        if (!coincidencia.matches()) {
            throw new SolicitudInvalidaException(CodigoError.CRITERIO_DE_BUSQUEDA_INVALIDO,
                    "Buscá por el número de recibo, con el formato R-000123");
        }
        return Long.parseLong(coincidencia.group(1));
    }

    /** Acepta el código del cobro (C-000123) o el número de la reserva (R-000123); otro formato es un 400. */
    private Specification<Cobro> filtroDeBusqueda(String busqueda) {
        String texto = Textos.recortar(busqueda);
        Matcher porCodigo = CODIGO_DE_COBRO.matcher(texto);
        if (porCodigo.matches()) {
            return CobroSpecs.conId(Long.parseLong(porCodigo.group(1)));
        }
        Matcher porReserva = NUMERO_DE_RESERVA.matcher(texto);
        if (porReserva.matches()) {
            return CobroSpecs.deReserva(Long.parseLong(porReserva.group(1)));
        }
        throw new SolicitudInvalidaException(CodigoError.CRITERIO_DE_BUSQUEDA_INVALIDO,
                "Buscá por el código del cobro (C-000123) o el número de reserva (R-000123)");
    }

    private void asegurarRangoDeFechas(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new SolicitudInvalidaException(CodigoError.RANGO_DE_CONSULTA_INVALIDO, "La fecha 'desde' no puede ser posterior a 'hasta'");
        }
    }

    private boolean hayTexto(String texto) {
        return texto != null && !texto.isBlank();
    }

    private void asegurarCobrable(Reserva reserva) {
        if (!reserva.getEstado().puedeCobrarseSenia() && !reserva.getEstado().puedeCobrarseSaldo()) {
            throw new ReglaDeNegocioException(CodigoError.RESERVA_NO_COBRABLE,
                    "La reserva está " + reserva.getEstado() + ": no admite cobros");
        }
    }

    /** La seña es el 50% del total, una sola vez; el saldo, lo que falta cubrir según los cobros ya pagados. */
    private BigDecimal montoBaseVigente(ConceptoCobro concepto, Reserva reserva) {
        if (concepto == ConceptoCobro.SEÑA) {
            if (cobroRepository.existsByReservaIdAndEstadoIn(reserva.getId(), EstadoCobro.queBloqueanNuevoCobro())) {
                throw new ConflictoException(CodigoError.COBRO_YA_REGISTRADO, "La reserva ya tiene un cobro registrado");
            }
            return calculadoraDeCobro.montoBase(reserva.getMontoTotal());
        }
        BigDecimal saldo = reserva.getMontoTotal().subtract(pagosDeReserva.resumen(reserva.getId()).baseCubierta());
        if (saldo.signum() <= 0) {
            throw new ConflictoException(CodigoError.COBRO_YA_REGISTRADO, "La reserva no tiene saldo pendiente");
        }
        return saldo;
    }

    /**
     * Débito y crédito se cargan con lo que muestra la terminal física del club, y el efectivo lo recibe la caja: solo el personal la opera, así
     * que solo el personal puede reportar {@code aprobado}. Sin este chequeo, un socio podría declararse su propio
     * pago aprobado sin que haya pasado ninguna tarjeta. El QR sí lo puede iniciar el socio: lo paga y lo confirma
     * Mercado Pago, no quien cobra.
     */
    private void asegurarMedioPermitido(MedioPago medioPago, Actor actor) {
        if (medioPago != MedioPago.QR && !AccesoAReservas.esPersonal(actor)) {
            throw new AccessDeniedException("Solo el personal del club cobra con débito, crédito o efectivo");
        }
    }

    private Cobro nuevo(Reserva reserva, MedioPago medioPago, ConceptoCobro concepto, Actor actor) {
        Cobro cobro = new Cobro();
        cobro.setReserva(reserva);
        cobro.setConcepto(concepto);
        cobro.setMedioPago(medioPago);
        cobro.setRegistradoPorId(AccesoAReservas.esPersonal(actor) ? actor.id() : null);
        // Placeholder para la restricción NOT NULL hasta calcular el monto real, una vez que el cobro ya tiene id.
        cobro.setMontoBase(BigDecimal.ZERO);
        cobro.setMontoFinal(BigDecimal.ZERO);
        return cobro;
    }

    private void completarMontos(Cobro cobro, BigDecimal montoBase, Long reservaId) {
        cobro.setCodigo(CobroMapper.codigoDe(cobro.getId()));
        Long socioId = accesoAReservas.titularId(reservaId);
        CalculadoraDeCobro.MontoDeCobro monto = calculadoraDeCobro.aplicarDescuentos(cobro.getId(), montoBase, socioId);
        cobro.setMontoBase(monto.montoBase());
        cobro.setMontoFinal(monto.montoFinal());
    }

    private ResultadoDeCobro cobrar(Cobro cobro, CobroRequest request) {
        DatosDeCobro datos = new DatosDeCobro(request.numeroOperacion(), request.cuotas(), request.aprobado(), cobro.getRegistradoPorId());
        ResultadoDeCobro resultado = formasDeCobro.de(request.medioPago()).cobrar(cobro, datos);
        cobro.setEstado(resultado.estado());
        if (resultado.numeroOperacion() != null) {
            cobro.setNumeroOperacion(resultado.numeroOperacion());
        }
        if (resultado.idPagoExterno() != null) {
            cobro.setIdPagoExterno(resultado.idPagoExterno());
            cobro.setFechaExpiracionQr(resultado.fechaExpiracionQr());
        }
        return resultado;
    }
}
