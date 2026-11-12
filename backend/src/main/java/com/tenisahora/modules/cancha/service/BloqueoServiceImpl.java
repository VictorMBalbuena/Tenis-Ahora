package com.tenisahora.modules.cancha.service;

import com.tenisahora.modules.cancha.dto.BloqueoDelDiaResponse;
import com.tenisahora.modules.cancha.dto.BloqueoRequest;
import com.tenisahora.modules.cancha.dto.BloqueoResponse;
import com.tenisahora.modules.cancha.entity.Cancha;
import com.tenisahora.modules.cancha.entity.Disponibilidad;
import com.tenisahora.modules.cancha.entity.EstadoDisponibilidad;
import com.tenisahora.modules.cancha.repository.DisponibilidadRepository;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.ConflictoException;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import com.tenisahora.shared.exception.SolicitudInvalidaException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BloqueoServiceImpl implements BloqueoService {

    private static final int MAX_DIAS_DE_BLOQUEO = 90;

    private final DisponibilidadRepository disponibilidadRepository;
    private final ReservasDelRango reservasDelRango;
    private final CanchaPorId canchaPorId;
    private final Clock clock;

    @Override
    @Transactional
    public BloqueoResponse bloquear(Long canchaId, BloqueoRequest request, Long actorId) {
        Cancha cancha = canchaPorId.activa(canchaId);
        FranjaHoraria franja = FranjaHoraria.validada(request.horaDesde(), request.horaHasta(), CodigoError.BLOQUEO_INVALIDO);
        validarFechas(request.fechaDesde(), request.fechaHasta());
        asegurarQueNoSePisaConOtroBloqueo(canchaId, request, franja);
        asegurarQueNoHayReservasConfirmadas(canchaId, request, franja);

        List<Disponibilidad> bloqueos = request.fechaDesde().datesUntil(request.fechaHasta().plusDays(1))
                .map(fecha -> bloqueoDelDia(cancha, fecha, franja, request.motivo())).toList();
        disponibilidadRepository.saveAll(bloqueos);
        log.info("Cancha {} bloqueada {} día(s) por la persona {}", canchaId, bloqueos.size(), actorId);
        return new BloqueoResponse(canchaId, request.fechaDesde(), request.fechaHasta(), franja.desde(), franja.hasta(),
                request.motivo(), bloqueos.size());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BloqueoDelDiaResponse> listar(Long canchaId, LocalDate desde, LocalDate hasta) {
        canchaPorId.existente(canchaId);
        if (hasta.isBefore(desde) || ChronoUnit.DAYS.between(desde, hasta) + 1 > MAX_DIAS_DE_BLOQUEO) {
            throw new SolicitudInvalidaException(CodigoError.RANGO_DE_CONSULTA_INVALIDO,
                    "El rango debe ser de hasta " + MAX_DIAS_DE_BLOQUEO + " días y con el inicio antes del fin");
        }
        return disponibilidadRepository.findByCanchaIdAndEstadoAndFechaBetween(canchaId, EstadoDisponibilidad.MANTENIMIENTO, desde, hasta)
                .stream()
                .sorted(Comparator.comparing(Disponibilidad::getFecha).thenComparing(Disponibilidad::getHoraInicio))
                .map(bloqueo -> new BloqueoDelDiaResponse(bloqueo.getId(), bloqueo.getFecha(), bloqueo.getHoraInicio(),
                        bloqueo.getHoraFin(), bloqueo.getMotivoBloqueo()))
                .toList();
    }

    @Override
    @Transactional
    public void desbloquear(Long canchaId, Long bloqueoId, Long actorId) {
        canchaPorId.existente(canchaId);
        Disponibilidad bloqueo = disponibilidadRepository.findByIdAndCanchaIdAndEstado(bloqueoId, canchaId, EstadoDisponibilidad.MANTENIMIENTO)
                .orElseThrow(() -> new RecursoNoEncontradoException(CodigoError.BLOQUEO_NO_ENCONTRADO, "Bloqueo no encontrado"));
        disponibilidadRepository.delete(bloqueo);
        log.info("Bloqueo {} de la cancha {} levantado por la persona {}", bloqueoId, canchaId, actorId);
    }

    private void validarFechas(LocalDate desde, LocalDate hasta) {
        if (hasta.isBefore(desde)) {
            throw new SolicitudInvalidaException(CodigoError.BLOQUEO_INVALIDO, "La fecha de inicio debe ser anterior o igual a la de fin");
        }
        if (desde.isBefore(LocalDate.now(clock))) {
            throw new SolicitudInvalidaException(CodigoError.BLOQUEO_INVALIDO, "No se puede bloquear una fecha pasada");
        }
        if (ChronoUnit.DAYS.between(desde, hasta) + 1 > MAX_DIAS_DE_BLOQUEO) {
            throw new SolicitudInvalidaException(CodigoError.BLOQUEO_INVALIDO,
                    "El bloqueo no puede durar más de " + MAX_DIAS_DE_BLOQUEO + " días");
        }
    }

    private void asegurarQueNoSePisaConOtroBloqueo(Long canchaId, BloqueoRequest request, FranjaHoraria franja) {
        boolean sePisa = disponibilidadRepository
                .findByCanchaIdAndEstadoAndFechaBetween(canchaId, EstadoDisponibilidad.MANTENIMIENTO, request.fechaDesde(), request.fechaHasta())
                .stream()
                .anyMatch(existente -> new FranjaHoraria(existente.getHoraInicio(), existente.getHoraFin()).seSuperponeCon(franja));
        if (sePisa) {
            throw new ConflictoException(CodigoError.BLOQUEO_SUPERPUESTO, "Ya hay un bloqueo por mantenimiento en ese rango");
        }
    }

    /** Si hay reservas vigentes (confirmadas o pendientes de pago) no se bloquea: hay que cancelarlas o reprogramarlas antes (CU091 A1). */
    private void asegurarQueNoHayReservasConfirmadas(Long canchaId, BloqueoRequest request, FranjaHoraria franja) {
        List<Long> reservas = reservasDelRango.idsVigentesEn(canchaId, request.fechaDesde(), request.fechaHasta(), franja);
        if (!reservas.isEmpty()) {
            String ids = reservas.stream().map(String::valueOf).collect(Collectors.joining(", "));
            throw new ConflictoException(CodigoError.BLOQUEO_CON_RESERVAS, "Hay " + reservas.size()
                    + " reserva(s) vigente(s) en ese rango (ids: " + ids + "). Cancelalas o reprogramalas antes de bloquear la cancha");
        }
    }

    private Disponibilidad bloqueoDelDia(Cancha cancha, LocalDate fecha, FranjaHoraria franja, String motivo) {
        Disponibilidad bloqueo = new Disponibilidad();
        bloqueo.setCancha(cancha);
        bloqueo.setFecha(fecha);
        bloqueo.setHoraInicio(franja.desde());
        bloqueo.setHoraFin(franja.hasta());
        bloqueo.setEstado(EstadoDisponibilidad.MANTENIMIENTO);
        bloqueo.setMotivoBloqueo(motivo);
        return bloqueo;
    }
}
