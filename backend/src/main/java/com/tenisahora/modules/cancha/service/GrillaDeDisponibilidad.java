package com.tenisahora.modules.cancha.service;

import com.tenisahora.modules.cancha.dto.EstadoFranja;
import com.tenisahora.modules.cancha.dto.FranjaResponse;
import com.tenisahora.modules.cancha.entity.HorarioSemanal;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Cruza los horarios semanales con los bloqueos y las reservas y arma la grilla de una hora por franja (CU092, RD05). */
public final class GrillaDeDisponibilidad {

    private GrillaDeDisponibilidad() {
    }

    public static List<FranjaResponse> calcular(LocalDate desde, LocalDate hasta, List<HorarioSemanal> horarios,
                                                List<FranjaDelDia> bloqueos, List<FranjaDelDia> ocupadas) {
        return calcular(desde, hasta, horarios, bloqueos, ocupadas, List.of());
    }

    public static List<FranjaResponse> calcular(LocalDate desde, LocalDate hasta, List<HorarioSemanal> horarios,
                                                List<FranjaDelDia> bloqueos, List<FranjaDelDia> reservas,
                                                List<FranjaDelDia> clases) {
        List<FranjaResponse> grilla = new ArrayList<>();
        desde.datesUntil(hasta.plusDays(1)).forEach(fecha ->
                grilla.addAll(franjasDelDia(fecha, horarios, bloqueos, reservas, clases)));
        return grilla;
    }

    private static List<FranjaResponse> franjasDelDia(LocalDate fecha, List<HorarioSemanal> horarios,
                                                      List<FranjaDelDia> bloqueos, List<FranjaDelDia> reservas,
                                                      List<FranjaDelDia> clases) {
        List<FranjaResponse> franjas = new ArrayList<>();
        horarios.stream()
                .filter(horario -> horario.getDiaSemana() == fecha.getDayOfWeek())
                .sorted(Comparator.comparing(HorarioSemanal::getHoraInicio))
                .forEach(horario -> {
                    for (LocalTime hora = horario.getHoraInicio(); hora.isBefore(horario.getHoraFin()); hora = hora.plusHours(1)) {
                        FranjaHoraria franja = new FranjaHoraria(hora, hora.plusHours(1));
                        franjas.add(crearFranja(fecha, franja, bloqueos, reservas, clases));
                    }
                });
        return franjas;
    }

    /** Mantenimiento manda sobre todo; luego clases y reservas (RD05). */
    private static FranjaResponse crearFranja(LocalDate fecha, FranjaHoraria franja,
                                              List<FranjaDelDia> bloqueos, List<FranjaDelDia> reservas,
                                              List<FranjaDelDia> clases) {
        if (bloqueos.stream().anyMatch(bloqueo -> bloqueo.seSuperponeCon(fecha, franja))) {
            return new FranjaResponse(fecha, franja.desde(), franja.hasta(), EstadoFranja.BLOQUEADA, "MANTENIMIENTO");
        }
        if (clases.stream().anyMatch(clase -> clase.seSuperponeCon(fecha, franja))) {
            return new FranjaResponse(fecha, franja.desde(), franja.hasta(), EstadoFranja.OCUPADA, "CLASE");
        }
        if (reservas.stream().anyMatch(reserva -> reserva.seSuperponeCon(fecha, franja))) {
            return new FranjaResponse(fecha, franja.desde(), franja.hasta(), EstadoFranja.OCUPADA, "RESERVA");
        }
        return new FranjaResponse(fecha, franja.desde(), franja.hasta(), EstadoFranja.LIBRE, null);
    }
}
