package com.tenisahora.modules.reporte.service;

import com.tenisahora.modules.actividad.entity.Asistencia;
import com.tenisahora.modules.actividad.repository.AsistenciaRepository;
import com.tenisahora.modules.reporte.dto.ReporteAsistenciasResponse;
import com.tenisahora.modules.reporte.dto.ReporteAsistenciasResponse.DetalleAlumno;
import com.tenisahora.modules.reporte.dto.ReporteAsistenciasResponse.Porcentaje;
import com.tenisahora.shared.entity.Persona;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReporteAsistenciasService {

    private static final int TOPE_DETALLE = 6;

    private final AsistenciaRepository asistenciaRepository;

    @Transactional(readOnly = true)
    public ReporteAsistenciasResponse generar(LocalDate desde, LocalDate hasta, String actividad) {
        List<Asistencia> todas = asistenciaRepository.findAll().stream().filter(a -> coincide(a, actividad)).toList();
        long dias = ChronoUnit.DAYS.between(desde, hasta) + 1;
        List<Asistencia> delPeriodo = enRango(todas, desde, hasta);
        List<Asistencia> anterior = enRango(todas, desde.minusDays(dias), desde.minusDays(1));

        long presentes = presentes(delPeriodo);
        long presentesAntes = presentes(anterior);
        int tasa = Porcentajes.de(presentes, delPeriodo.size());
        int tasaAntes = Porcentajes.de(presentesAntes, anterior.size());

        return new ReporteAsistenciasResponse(
                presentes,
                Porcentajes.variacion(presentes, presentesAntes),
                tasa + "%",
                Porcentajes.variacion(tasa, tasaAntes),
                delPeriodo.size() - presentes,
                delPeriodo.stream().map(a -> a.getSocio().getId()).distinct().count(),
                tasaPor(delPeriodo, a -> a.getActividad().getNombre()),
                tasaPor(delPeriodo, a -> nombre(a.getActividad().getInstructor())),
                detalle(delPeriodo));
    }

    private boolean coincide(Asistencia asistencia, String filtro) {
        if (filtro == null || filtro.isBlank() || filtro.equalsIgnoreCase("Todas")) {
            return true;
        }
        return filtro.equalsIgnoreCase(asistencia.getActividad().getNombre());
    }

    private List<Asistencia> enRango(List<Asistencia> asistencias, LocalDate desde, LocalDate hasta) {
        return asistencias.stream().filter(a -> !a.getFecha().isBefore(desde) && !a.getFecha().isAfter(hasta)).toList();
    }

    private long presentes(List<Asistencia> asistencias) {
        return asistencias.stream().filter(a -> Boolean.TRUE.equals(a.getPresente())).count();
    }

    private List<Porcentaje> tasaPor(List<Asistencia> asistencias, Function<Asistencia, String> clave) {
        return asistencias.stream()
                .collect(Collectors.groupingBy(clave))
                .entrySet().stream()
                .map(e -> new Porcentaje(e.getKey(), Porcentajes.de(presentes(e.getValue()), e.getValue().size())))
                .sorted(Comparator.comparingInt(Porcentaje::porcentaje).reversed())
                .toList();
    }

    private List<DetalleAlumno> detalle(List<Asistencia> asistencias) {
        Map<String, List<Asistencia>> porAlumnoYActividad = asistencias.stream()
                .collect(Collectors.groupingBy(a -> a.getSocio().getId() + "-" + a.getActividad().getId()));
        return porAlumnoYActividad.values().stream()
                .map(this::filaDe)
                .sorted(Comparator.comparingLong(DetalleAlumno::presentes).reversed())
                .limit(TOPE_DETALLE)
                .toList();
    }

    private DetalleAlumno filaDe(List<Asistencia> registros) {
        Asistencia primera = registros.get(0);
        Persona socio = primera.getSocio();
        long presentes = presentes(registros);
        return new DetalleAlumno(
                iniciales(socio),
                socio.getNombre() + " " + socio.getApellido(),
                primera.getActividad().getNombre(),
                registros.size(),
                presentes,
                registros.size() - presentes,
                Porcentajes.de(presentes, registros.size()) + "%");
    }

    private String nombre(Persona persona) {
        return persona == null ? "Sin asignar" : persona.getApellido() + ", " + persona.getNombre();
    }

    private String iniciales(Persona persona) {
        return (persona.getNombre().substring(0, 1) + persona.getApellido().substring(0, 1)).toUpperCase();
    }
}
