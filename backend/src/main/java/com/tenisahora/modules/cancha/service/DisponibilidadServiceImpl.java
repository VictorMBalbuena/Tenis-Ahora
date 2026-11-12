package com.tenisahora.modules.cancha.service;

import com.tenisahora.modules.cancha.dto.GrillaResponse;
import com.tenisahora.modules.cancha.entity.Cancha;
import com.tenisahora.modules.cancha.entity.EstadoDisponibilidad;
import com.tenisahora.modules.cancha.repository.DisponibilidadRepository;
import com.tenisahora.modules.cancha.repository.HorarioSemanalRepository;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.SolicitudInvalidaException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DisponibilidadServiceImpl implements DisponibilidadService {

    private static final int MAX_DIAS_DE_CONSULTA = 31;

    private final HorarioSemanalRepository horarioRepository;
    private final DisponibilidadRepository disponibilidadRepository;
    private final ReservasDelRango reservasDelRango;
    private final ActividadesDelRango actividadesDelRango;
    private final CanchaPorId canchaPorId;

    @Override
    @Transactional(readOnly = true)
    public GrillaResponse consultar(Long canchaId, LocalDate desde, LocalDate hasta) {
        Cancha cancha = canchaPorId.existente(canchaId);
        validarRango(desde, hasta);
        if (!Boolean.TRUE.equals(cancha.getActiva())) {
            return new GrillaResponse(canchaId, desde, hasta, List.of());
        }
        List<FranjaDelDia> bloqueos = disponibilidadRepository
                .findByCanchaIdAndEstadoAndFechaBetween(canchaId, EstadoDisponibilidad.MANTENIMIENTO, desde, hasta).stream()
                .map(bloqueo -> new FranjaDelDia(bloqueo.getFecha(), bloqueo.getHoraInicio(), bloqueo.getHoraFin())).toList();
        List<FranjaDelDia> reservas = reservasDelRango.ocupadasEn(canchaId, desde, hasta);
        List<FranjaDelDia> clases = actividadesDelRango.ocupadasEn(canchaId, desde, hasta);
        return new GrillaResponse(canchaId, desde, hasta, GrillaDeDisponibilidad.calcular(desde, hasta,
                horarioRepository.findByCanchaIdOrderByDiaSemanaAscHoraInicioAsc(canchaId), bloqueos, reservas, clases));
    }

    private void validarRango(LocalDate desde, LocalDate hasta) {
        if (hasta.isBefore(desde)) {
            throw new SolicitudInvalidaException(CodigoError.RANGO_DE_CONSULTA_INVALIDO, "La fecha de inicio debe ser anterior o igual a la de fin");
        }
        if (ChronoUnit.DAYS.between(desde, hasta) + 1 > MAX_DIAS_DE_CONSULTA) {
            throw new SolicitudInvalidaException(CodigoError.RANGO_DE_CONSULTA_INVALIDO,
                    "Se pueden consultar hasta " + MAX_DIAS_DE_CONSULTA + " días por vez");
        }
    }
}
