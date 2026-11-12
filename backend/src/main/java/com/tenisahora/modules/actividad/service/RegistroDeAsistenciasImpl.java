package com.tenisahora.modules.actividad.service;

import com.tenisahora.modules.actividad.dto.AsistenciaResponse;
import com.tenisahora.modules.actividad.dto.MarcaDeAsistenciaRequest;
import com.tenisahora.modules.actividad.entity.Actividad;
import com.tenisahora.modules.actividad.entity.ActividadDia;
import com.tenisahora.modules.actividad.entity.Asistencia;
import com.tenisahora.modules.actividad.entity.InscripcionClase;
import com.tenisahora.modules.actividad.repository.ActividadDiaRepository;
import com.tenisahora.modules.actividad.repository.AsistenciaRepository;
import com.tenisahora.modules.actividad.repository.InscripcionClaseRepository;
import com.tenisahora.modules.socio.entity.Socio;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** CU42 y CU43: un solo flujo para clases y entrenamientos, con distinto actor (profesor o entrenador). */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegistroDeAsistenciasImpl implements RegistroDeAsistencias {

    private final ActividadDiaRepository diasRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final InscripcionClaseRepository inscripcionRepository;
    private final AccesoAActividades acceso;
    private final Clock clock;

    @Override
    @Transactional
    public List<AsistenciaResponse> registrar(Long actividadId, LocalDate fecha, List<MarcaDeAsistenciaRequest> marcas, Actor actor) {
        Actividad actividad = acceso.paraOperar(actividadId, Actividad.class, CodigoError.ACTIVIDAD_NO_ENCONTRADA, actor);
        LocalDate fechaEfectiva = fecha != null ? fecha : LocalDate.now(clock);
        asegurarFechaDeDictado(actividad, fechaEfectiva);
        Map<Long, Socio> inscriptos = inscripcionRepository.deLaActividad(actividadId).stream()
                .collect(Collectors.toMap(i -> i.getSocio().getId(), InscripcionClase::getSocio));

        List<AsistenciaResponse> resultado = marcas.stream().map(marca -> marcar(actividad, inscriptos, marca, fechaEfectiva)).toList();
        log.info("Actividad {}: {} marcas de asistencia registradas por la persona {}", actividadId, marcas.size(), actor.id());
        return resultado;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AsistenciaResponse> delDia(Long actividadId, LocalDate fecha, Actor actor) {
        acceso.paraOperar(actividadId, Actividad.class, CodigoError.ACTIVIDAD_NO_ENCONTRADA, actor);
        LocalDate fechaEfectiva = fecha != null ? fecha : LocalDate.now(clock);
        Map<Long, Boolean> presentes = asistenciaRepository.deLaActividadEnLaFecha(actividadId, fechaEfectiva).stream()
                .collect(Collectors.toMap(a -> a.getSocio().getId(), Asistencia::getPresente));
        return inscripcionRepository.deLaActividad(actividadId).stream()
                .map(inscripcion -> aRespuesta(actividadId, inscripcion.getSocio(), fechaEfectiva,
                        Boolean.TRUE.equals(presentes.get(inscripcion.getSocio().getId()))))
                .toList();
    }

    private AsistenciaResponse marcar(Actividad actividad, Map<Long, Socio> inscriptos, MarcaDeAsistenciaRequest marca, LocalDate fecha) {
        Socio socio = inscriptos.get(marca.socioId());
        if (socio == null) {
            throw new ReglaDeNegocioException(CodigoError.SOCIO_NO_INSCRIPTO,
                    "El socio " + marca.socioId() + " no está inscripto en esta actividad");
        }
        Asistencia asistencia = asistenciaRepository.findByActividadIdAndSocioIdAndFecha(actividad.getId(), marca.socioId(), fecha)
                .orElseGet(() -> nueva(actividad, socio, fecha));
        asistencia.setPresente(marca.presente());
        asistenciaRepository.save(asistencia);
        return aRespuesta(actividad.getId(), socio, fecha, asistencia.getPresente());
    }

    /** "La actividad es del día": no futura, dentro de su vigencia y en uno de sus días de dictado. */
    private void asegurarFechaDeDictado(Actividad actividad, LocalDate fecha) {
        if (fecha.isAfter(LocalDate.now(clock))) {
            throw new ReglaDeNegocioException(CodigoError.FECHA_SIN_DICTADO, "No se puede registrar asistencia de una fecha futura");
        }
        if (fecha.isBefore(actividad.getFechaInicio()) || fecha.isAfter(actividad.getFechaFin())) {
            throw new ReglaDeNegocioException(CodigoError.FECHA_SIN_DICTADO, "Esa fecha está fuera de la vigencia de la actividad");
        }
        Set<DayOfWeek> dias = diasRepository.findByActividadId(actividad.getId()).stream()
                .map(ActividadDia::getDiaSemana).collect(Collectors.toSet());
        if (!dias.contains(fecha.getDayOfWeek())) {
            throw new ReglaDeNegocioException(CodigoError.FECHA_SIN_DICTADO, "La actividad no se dicta ese día de la semana");
        }
    }

    private Asistencia nueva(Actividad actividad, Socio socio, LocalDate fecha) {
        Asistencia asistencia = new Asistencia();
        asistencia.setActividad(actividad);
        asistencia.setSocio(socio);
        asistencia.setFecha(fecha);
        return asistencia;
    }

    private AsistenciaResponse aRespuesta(Long actividadId, Socio socio, LocalDate fecha, boolean presente) {
        return new AsistenciaResponse(actividadId, socio.getId(), socio.getApellido() + ", " + socio.getNombre(), fecha, presente);
    }
}
