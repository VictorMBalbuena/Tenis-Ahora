package com.tenisahora.modules.actividad.service;

import com.tenisahora.modules.actividad.entity.Actividad;
import com.tenisahora.modules.actividad.entity.ActividadDia;
import com.tenisahora.modules.actividad.entity.EstadoActividad;
import com.tenisahora.modules.actividad.repository.ActividadDiaRepository;
import com.tenisahora.modules.actividad.repository.ActividadRepository;
import com.tenisahora.modules.cancha.service.ActividadesDelRango;
import com.tenisahora.modules.cancha.service.FranjaDelDia;
import com.tenisahora.modules.cancha.service.FranjaHoraria;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

/** Lo que el módulo de canchas pregunta de las clases y entrenamientos: espejo de ReservasDelRangoImpl. */
@Service
@RequiredArgsConstructor
public class ActividadesDelRangoImpl implements ActividadesDelRango {

    private static final List<EstadoActividad> ESTADOS_QUE_OCUPAN = EstadoActividad.queOcupanCancha();

    private final ActividadRepository actividadRepository;
    private final ActividadDiaRepository diasRepository;

    @Override
    @Transactional(readOnly = true)
    public List<FranjaDelDia> ocupadasEn(Long canchaId, LocalDate desde, LocalDate hasta) {
        return actividadRepository.vigentesEnRango(canchaId, desde, hasta, ESTADOS_QUE_OCUPAN).stream()
                .flatMap(actividad -> franjasDe(actividad, desde, hasta).stream())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> idsVigentesEnElDia(Long canchaId, LocalDate fecha, DayOfWeek dia, FranjaHoraria franja) {
        return actividadRepository.idsVigentesEnElDia(canchaId, fecha, dia, franja.desde(), franja.hasta(), ESTADOS_QUE_OCUPAN);
    }

    /** Las fechas del solapamiento entre el rango consultado y el de la actividad que caen en uno de sus días de dictado. */
    private List<FranjaDelDia> franjasDe(Actividad actividad, LocalDate desde, LocalDate hasta) {
        LocalDate inicio = actividad.getFechaInicio().isAfter(desde) ? actividad.getFechaInicio() : desde;
        LocalDate fin = actividad.getFechaFin().isBefore(hasta) ? actividad.getFechaFin() : hasta;
        if (inicio.isAfter(fin)) {
            return List.of();
        }
        List<DayOfWeek> dias = diasRepository.findByActividadId(actividad.getId()).stream().map(ActividadDia::getDiaSemana).toList();
        return inicio.datesUntil(fin.plusDays(1))
                .filter(fecha -> dias.contains(fecha.getDayOfWeek()))
                .map(fecha -> new FranjaDelDia(fecha, actividad.getHoraInicio(), actividad.getHoraFin()))
                .toList();
    }
}
