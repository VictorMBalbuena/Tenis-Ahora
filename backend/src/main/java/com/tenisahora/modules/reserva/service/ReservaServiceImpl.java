package com.tenisahora.modules.reserva.service;

import com.tenisahora.modules.cancha.entity.Cancha;
import com.tenisahora.modules.cancha.repository.CanchaRepository;
import com.tenisahora.modules.cancha.service.FranjaHoraria;
import com.tenisahora.modules.socio.entity.Socio;
import com.tenisahora.shared.model.Rol;
import com.tenisahora.modules.socio.repository.SocioRepository;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.modules.reserva.dto.ReservaRequest;
import com.tenisahora.modules.reserva.dto.ReservaResponse;
import com.tenisahora.modules.reserva.dto.ReservaUpdateRequest;
import com.tenisahora.modules.reserva.entity.EstadoReserva;
import com.tenisahora.modules.reserva.entity.Reserva;
import com.tenisahora.modules.reserva.entity.ReservaInsumo;
import com.tenisahora.modules.reserva.entity.ReservaJugador;
import com.tenisahora.modules.reserva.mapper.ReservaMapper;
import com.tenisahora.modules.reserva.repository.ReservaJugadorRepository;
import com.tenisahora.modules.reserva.repository.ReservaRepository;
import com.tenisahora.modules.reserva.repository.ReservaSpecs;
import com.tenisahora.shared.dto.PageResponse;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import com.tenisahora.shared.exception.SolicitudInvalidaException;
import com.tenisahora.shared.util.Textos;
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
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservaServiceImpl implements ReservaService {

    /** Acepta "R-000123", "r-123" o "123". */
    private static final Pattern NUMERO_DE_RESERVA = Pattern.compile("(?i)^R?-?0*(\\d{1,18})$");
    private static final Sort MAS_RECIENTES_PRIMERO = Sort.by(Sort.Order.desc("fecha"), Sort.Order.desc("horaInicio"));
    private static final Sort MAS_PROXIMAS_PRIMERO = Sort.by(Sort.Order.asc("fecha"), Sort.Order.asc("horaInicio"));

    private final ReservaRepository reservaRepository;
    private final ReservaJugadorRepository jugadorRepository;
    private final CanchaRepository canchaRepository;
    private final SocioRepository socioRepository;
    private final ReglasDeReserva reglas;
    private final CalculadoraDeMontos calculadora;
    private final ValidadorDeAgenda validadorDeAgenda;
    private final ArmadoDeJugadores armadoDeJugadores;
    private final ArmadoDeRespuestas respuestas;
    private final ReservaMapper reservaMapper;
    private final AccesoAReservas acceso;
    private final AsignacionDeInsumos asignacionDeInsumos;
    private final DescuentoPorCodigo descuentoPorCodigo;
    private final Clock clock;

    // ---------- Alta (CU16) ----------

    @Override
    @Transactional
    public ReservaResponse registrar(ReservaRequest request, Actor actor) {
        FranjaHoraria franja = reglas.validarTurno(request.fecha(), request.horaInicio(), request.duracionHoras());
        reglas.validarAcompaniantes(request.acompaniantes().size());
        Socio titular = titularDe(request.titularId(), actor);
        Cancha cancha = canchaParaReservar(request.canchaId());
        validadorDeAgenda.asegurarTurnoLibre(cancha, request.fecha(), franja, null);

        Reserva reserva = nueva(cancha, request, franja);
        List<ReservaJugador> jugadores = armadoDeJugadores.armar(reserva, titular, request.acompaniantes());
        reservaRepository.saveAndFlush(reserva);
        jugadorRepository.saveAll(jugadores);
        List<ReservaInsumo> insumos = asignacionDeInsumos.asignar(reserva, request.insumos());
        log.info("Reserva {} registrada por la persona {} en la cancha {}", reserva.getId(), actor.id(), cancha.getId());
        return reservaMapper.aRespuesta(reserva, jugadores, insumos);
    }

    // ---------- Consultas ----------

    @Override
    @Transactional(readOnly = true)
    public ReservaResponse consultar(Long id, Actor actor) {
        return respuestas.deReserva(acceso.visible(id, actor));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReservaResponse> misReservas(Boolean proximas, EstadoReserva estado, int page, int size, Actor actor) {
        List<Specification<Reserva>> filtros = new ArrayList<>(List.of(ReservaSpecs.participa(actor.id())));
        if (proximas != null) {
            filtros.add(ReservaSpecs.proximas(proximas, LocalDate.now(clock)));
        }
        if (estado != null) {
            filtros.add(ReservaSpecs.deEstado(estado));
        }
        return pagina(filtros, Boolean.TRUE.equals(proximas) ? MAS_PROXIMAS_PRIMERO : MAS_RECIENTES_PRIMERO, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReservaResponse> buscar(String documento, String numero, EstadoReserva estado, LocalDate fecha,
                                                Long canchaId, int page, int size) {
        List<Specification<Reserva>> filtros = new ArrayList<>();
        if (hayTexto(numero)) {
            filtros.add(ReservaSpecs.conId(numeroComoId(numero)));
        }
        if (hayTexto(documento)) {
            filtros.add(ReservaSpecs.titularConDni(documento.strip()));
        }
        if (estado != null) {
            filtros.add(ReservaSpecs.deEstado(estado));
        }
        if (fecha != null) {
            filtros.add(ReservaSpecs.enFecha(fecha));
        }
        if (canchaId != null) {
            filtros.add(ReservaSpecs.deCancha(canchaId));
        }
        return pagina(filtros, MAS_RECIENTES_PRIMERO, page, size);
    }

    // ---------- Modificación (CU17) ----------

    @Override
    @Transactional
    public ReservaResponse modificar(Long id, ReservaUpdateRequest request, Actor actor) {
        Reserva reserva = reservaRepository.buscarParaModificar(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(CodigoError.RESERVA_NO_ENCONTRADA, "Reserva no encontrada"));
        if (!reserva.getEstado().puedeModificarse()) {
            throw new ReglaDeNegocioException(CodigoError.RESERVA_NO_MODIFICABLE,
                    "La reserva está " + reserva.getEstado().etiqueta() + ": ya no se puede modificar");
        }
        LocalDate fecha = valor(request.fecha(), reserva.getFecha());
        int duracion = valor(request.duracionHoras(), (int) Duration.between(reserva.getHoraInicio(), reserva.getHoraFin()).toHours());
        FranjaHoraria franja = reglas.validarTurno(fecha, valor(request.horaInicio(), reserva.getHoraInicio()), duracion);
        Cancha cancha = canchaParaReservar(valor(request.canchaId(), reserva.getCancha().getId()));
        validadorDeAgenda.asegurarTurnoLibre(cancha, fecha, franja, reserva.getId());

        mover(reserva, cancha, fecha, franja, duracion);
        log.info("Reserva {} modificada por la persona {}", id, actor.id());
        return respuestas.deReserva(reserva);
    }

    // ---------- Auxiliares ----------

    /** Cambia el turno, recalcula el monto y vuelve a comprobar que los insumos asignados alcancen en el nuevo horario. */
    private void mover(Reserva reserva, Cancha cancha, LocalDate fecha, FranjaHoraria franja, int duracion) {
        reserva.setCancha(cancha);
        reserva.setFecha(fecha);
        reserva.setHoraInicio(franja.desde());
        reserva.setHoraFin(franja.hasta());
        // Cambió el turno: el total es otro, así que el descuento se vuelve a calcular sobre el nuevo.
        aplicarTotal(reserva, calculadora.total(cancha.getPrecioPorHora(), duracion), reserva.getCodigoDescuento());
        reservaRepository.saveAndFlush(reserva);
        asignacionDeInsumos.revalidar(reserva);
    }

    /**
     * Fija el total de la reserva, con el descuento aplicado si vino un código.
     *
     * El descuento se resuelve siempre acá contra el precio real, nunca con lo que haya mandado el cliente
     * (RD09). Si el código no existe o está dado de baja, el alta falla: es preferible a cobrarle de más a
     * alguien que creía tener un descuento.
     */
    private void aplicarTotal(Reserva reserva, BigDecimal total, String codigo) {
        if (codigo == null || codigo.isBlank()) {
            reserva.setMontoTotal(total);
            reserva.setCodigoDescuento(null);
            reserva.setMontoDescuento(null);
            return;
        }
        DescuentoPorCodigo.DescuentoAplicado aplicado = descuentoPorCodigo.aplicar(codigo, total);
        reserva.setMontoTotal(aplicado.montoFinal());
        reserva.setCodigoDescuento(aplicado.codigo());
        reserva.setMontoDescuento(aplicado.montoDescontado());
    }

    /** Un socio reserva siempre para sí mismo; el personal indica el socio titular, que tiene que estar activo. */
    private Socio titularDe(Long titularId, Actor actor) {
        if (actor.rol() == Rol.SOCIO) {
            if (titularId != null && !titularId.equals(actor.id())) {
                throw new AccessDeniedException("Un socio solo reserva para sí mismo");
            }
            return socioRepository.findById(actor.id()).orElseThrow(() -> titularInvalido("No se encontró tu cuenta"));
        }
        if (titularId == null) {
            throw new SolicitudInvalidaException(CodigoError.RESERVA_TITULAR_INVALIDO, "Indicá el socio titular de la reserva");
        }
        Socio titular = socioRepository.findById(titularId).orElseThrow(() -> titularInvalido("El titular no existe"));
        if (!Boolean.TRUE.equals(titular.getActivo())) {
            throw titularInvalido("El titular tiene que ser un socio activo");
        }
        return titular;
    }

    private Cancha canchaParaReservar(Long canchaId) {
        return canchaRepository.buscarParaReservar(canchaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(CodigoError.CANCHA_NO_ENCONTRADA, "Cancha no encontrada"));
    }

    private Reserva nueva(Cancha cancha, ReservaRequest request, FranjaHoraria franja) {
        Reserva reserva = new Reserva();
        reserva.setCancha(cancha);
        reserva.setFecha(request.fecha());
        reserva.setHoraInicio(franja.desde());
        reserva.setHoraFin(franja.hasta());
        reserva.setEstado(EstadoReserva.PENDIENTE_PAGO);
        reserva.setCantidadJugadores(1 + request.acompaniantes().size());
        aplicarTotal(reserva, calculadora.total(cancha.getPrecioPorHora(), request.duracionHoras()),
                request.codigoDescuento());
        reserva.setFechaCreacion(LocalDateTime.now(clock));
        return reserva;
    }

    private PageResponse<ReservaResponse> pagina(List<Specification<Reserva>> filtros, Sort orden, int page, int size) {
        Page<Reserva> reservas = reservaRepository.findAll(Specification.allOf(filtros), PageRequest.of(page, size, orden));
        return new PageResponse<>(respuestas.deReservas(reservas.getContent()), reservas.getNumber(), reservas.getSize(),
                reservas.getTotalElements(), reservas.getTotalPages());
    }

    private Long numeroComoId(String numero) {
        Matcher coincidencia = NUMERO_DE_RESERVA.matcher(Textos.recortar(numero));
        if (!coincidencia.matches()) {
            throw new SolicitudInvalidaException(CodigoError.CRITERIO_DE_BUSQUEDA_INVALIDO, "El número de reserva tiene el formato R-000123");
        }
        return Long.parseLong(coincidencia.group(1));
    }

    private boolean hayTexto(String texto) {
        return texto != null && !texto.isBlank();
    }

    private static <T> T valor(T nuevo, T actual) {
        return nuevo != null ? nuevo : actual;
    }

    private ReglaDeNegocioException titularInvalido(String mensaje) {
        return new ReglaDeNegocioException(CodigoError.RESERVA_TITULAR_INVALIDO, mensaje);
    }
}
