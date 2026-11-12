package com.tenisahora.modules.reporte.service;

import com.tenisahora.modules.cancha.entity.Cancha;
import com.tenisahora.modules.cancha.repository.CanchaRepository;
import com.tenisahora.modules.reporte.dto.ReporteReservasResponse;
import com.tenisahora.modules.reporte.dto.ReporteReservasResponse.Cancelaciones;
import com.tenisahora.modules.reporte.dto.ReporteReservasResponse.FranjaOcupacion;
import com.tenisahora.modules.reporte.dto.ReporteReservasResponse.PorCancha;
import com.tenisahora.modules.reserva.entity.EstadoReserva;
import com.tenisahora.modules.reserva.entity.Reserva;
import com.tenisahora.modules.reserva.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// La ocupación se mide contra una jornada fija de 08 a 22 por cancha, en franjas de dos horas.
@Service
@RequiredArgsConstructor
public class ReporteReservasService {

    private static final int HORA_APERTURA = 8;
    private static final int HORA_CIERRE = 22;
    private static final int ANCHO_FRANJA = 2;
    private static final Set<EstadoReserva> CONFIRMADAS = Set.of(EstadoReserva.CONFIRMADA, EstadoReserva.EN_CURSO, EstadoReserva.FINALIZADA);

    private final ReservaRepository reservaRepository;
    private final CanchaRepository canchaRepository;

    @Transactional(readOnly = true)
    public ReporteReservasResponse generar(LocalDate desde, LocalDate hasta, String cancha) {
        List<Cancha> canchas = canchaRepository.findAll().stream().filter(c -> coincide(c, cancha)).toList();
        Set<Long> idsCanchas = canchas.stream().map(Cancha::getId).collect(Collectors.toSet());
        List<Reserva> todas = reservaRepository.findAll().stream().filter(r -> idsCanchas.contains(r.getCancha().getId())).toList();

        long dias = ChronoUnit.DAYS.between(desde, hasta) + 1;
        List<Reserva> delPeriodo = enRango(todas, desde, hasta);
        List<Reserva> anterior = enRango(todas, desde.minusDays(dias), desde.minusDays(1));
        return armar(delPeriodo, anterior, canchas.size(), dias);
    }

    private ReporteReservasResponse armar(List<Reserva> delPeriodo, List<Reserva> anterior, int cantidadCanchas, long dias) {
        List<Reserva> canceladas = delPeriodo.stream().filter(r -> r.getEstado() == EstadoReserva.CANCELADA).toList();
        long confirmadas = delPeriodo.stream().filter(r -> CONFIRMADAS.contains(r.getEstado())).count();
        long fueraPlazo = canceladas.stream().filter(r -> positivo(r.getMontoPenalidad())).count();
        BigDecimal retenido = canceladas.stream().map(Reserva::getMontoPenalidad).filter(this::positivo).reduce(BigDecimal.ZERO, BigDecimal::add);

        double capacidad = (double) cantidadCanchas * dias * (HORA_CIERRE - HORA_APERTURA);
        double ocupacion = capacidad > 0 ? horasOcupadas(delPeriodo) * 100 / capacidad : 0;
        double ocupacionAnterior = capacidad > 0 ? horasOcupadas(anterior) * 100 / capacidad : 0;
        List<FranjaOcupacion> franjas = franjas(delPeriodo);

        return new ReporteReservasResponse(
                delPeriodo.size(),
                Porcentajes.variacion(delPeriodo.size(), anterior.size()),
                confirmadas,
                Porcentajes.de(confirmadas, delPeriodo.size()) + "% del total",
                canceladas.size(),
                fueraPlazo + " con penalidad",
                String.format("%.1f%%", ocupacion),
                Porcentajes.variacion(ocupacion, ocupacionAnterior),
                porCancha(delPeriodo),
                franjas,
                textoPico(franjas),
                new Cancelaciones(canceladas.size() - fueraPlazo, fueraPlazo, retenido));
    }

    private boolean coincide(Cancha cancha, String filtro) {
        if (filtro == null || filtro.isBlank() || filtro.equalsIgnoreCase("Todas")) {
            return true;
        }
        return filtro.split(" - ")[0].trim().equalsIgnoreCase(cancha.getIdentificador());
    }

    private List<Reserva> enRango(List<Reserva> reservas, LocalDate desde, LocalDate hasta) {
        return reservas.stream().filter(r -> !r.getFecha().isBefore(desde) && !r.getFecha().isAfter(hasta)).toList();
    }

    private double horasOcupadas(List<Reserva> reservas) {
        return reservas.stream()
                .filter(r -> r.getEstado() != EstadoReserva.CANCELADA)
                .mapToDouble(r -> Duration.between(r.getHoraInicio(), r.getHoraFin()).toMinutes() / 60.0)
                .sum();
    }

    private List<PorCancha> porCancha(List<Reserva> reservas) {
        Map<String, Long> cantidades = reservas.stream()
                .collect(Collectors.groupingBy(r -> r.getCancha().getIdentificador(), Collectors.counting()));
        long maximo = cantidades.values().stream().max(Long::compare).orElse(0L);
        return cantidades.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> new PorCancha(e.getKey(), e.getValue(), Porcentajes.de(e.getValue(), maximo)))
                .toList();
    }

    private List<FranjaOcupacion> franjas(List<Reserva> reservas) {
        List<double[]> horasPorFranja = new ArrayList<>();
        for (int hora = HORA_APERTURA; hora < HORA_CIERRE; hora += ANCHO_FRANJA) {
            int inicio = hora;
            double horas = reservas.stream()
                    .filter(r -> r.getEstado() != EstadoReserva.CANCELADA)
                    .mapToDouble(r -> solapamientoEnHoras(r, inicio, inicio + ANCHO_FRANJA))
                    .sum();
            horasPorFranja.add(new double[]{inicio, horas});
        }
        double total = horasPorFranja.stream().mapToDouble(f -> f[1]).sum();
        double pico = horasPorFranja.stream().mapToDouble(f -> f[1]).max().orElse(0);
        return horasPorFranja.stream()
                .map(f -> new FranjaOcupacion(String.format("%02d", (int) f[0]), Porcentajes.de(f[1], total), pico > 0 && f[1] == pico))
                .toList();
    }

    private double solapamientoEnHoras(Reserva reserva, int desde, int hasta) {
        double inicio = Math.max(reserva.getHoraInicio().toSecondOfDay() / 3600.0, desde);
        double fin = Math.min(reserva.getHoraFin().toSecondOfDay() / 3600.0, hasta);
        return Math.max(0, fin - inicio);
    }

    private String textoPico(List<FranjaOcupacion> franjas) {
        return franjas.stream()
                .filter(FranjaOcupacion::pico)
                .max(Comparator.comparingInt(FranjaOcupacion::porcentaje))
                .map(f -> "Franja pico: " + f.hora() + ":00 a " + String.format("%02d", Integer.parseInt(f.hora()) + ANCHO_FRANJA) + ":00")
                .orElse("Sin reservas en el período");
    }

    private boolean positivo(BigDecimal monto) {
        return monto != null && monto.signum() > 0;
    }
}
