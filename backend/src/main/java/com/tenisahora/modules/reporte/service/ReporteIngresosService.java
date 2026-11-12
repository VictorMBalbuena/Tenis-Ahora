package com.tenisahora.modules.reporte.service;

import com.tenisahora.modules.cobro.entity.Cobro;
import com.tenisahora.modules.cobro.entity.ConceptoCobro;
import com.tenisahora.modules.cobro.entity.EstadoCobro;
import com.tenisahora.modules.cobro.entity.MedioPago;
import com.tenisahora.modules.cobro.repository.CobroRepository;
import com.tenisahora.modules.descuento.entity.CobroDescuento;
import com.tenisahora.modules.descuento.repository.CobroDescuentoRepository;
import com.tenisahora.modules.reporte.dto.ReporteIngresosResponse;
import com.tenisahora.modules.reporte.dto.ReporteIngresosResponse.DescuentoAplicado;
import com.tenisahora.modules.reporte.dto.ReporteIngresosResponse.MontoDelDia;
import com.tenisahora.modules.reporte.dto.ReporteIngresosResponse.PorConcepto;
import com.tenisahora.modules.reporte.dto.ReporteIngresosResponse.PorMedio;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// Los conceptos de la pantalla agrupan los del cobro: la reserva es alquiler, el torneo inscripción y el resto clases y entrenamientos.
@Service
@RequiredArgsConstructor
public class ReporteIngresosService {

    private static final String ALQUILER = "Alquiler de canchas";
    private static final String TORNEOS = "Inscripciones a torneos";
    private static final String ACTIVIDADES = "Clases y entrenamientos";
    private static final String PENALIDADES = "Penalidades por cancelacion";
    private static final String REEMBOLSOS = "Reembolsos";
    private static final Map<MedioPago, String> ETIQUETA_MEDIO = Map.of(
            MedioPago.DEBITO, "Débito", MedioPago.CREDITO, "Crédito", MedioPago.QR, "QR Mercado Pago",
            MedioPago.EFECTIVO, "Efectivo");

    private final CobroRepository cobroRepository;
    private final CobroDescuentoRepository cobroDescuentoRepository;

    @Transactional(readOnly = true)
    public ReporteIngresosResponse generar(LocalDate desde, LocalDate hasta, String concepto) {
        List<Movimiento> filtrados = movimientos().stream().filter(m -> coincide(m, concepto)).toList();
        long dias = ChronoUnit.DAYS.between(desde, hasta) + 1;
        List<Movimiento> delPeriodo = enRango(filtrados, desde, hasta);
        List<Movimiento> anterior = enRango(filtrados, desde.minusDays(dias), desde.minusDays(1));
        return armar(delPeriodo, anterior, desde, hasta);
    }

    private List<Movimiento> movimientos() {
        List<Movimiento> todos = new ArrayList<>();
        cobroRepository.findAll().stream()
                .filter(c -> c.getEstado() == EstadoCobro.PAGADO)
                .forEach(c -> todos.add(movimientoDe(c)));
        return todos;
    }

    /** Un reembolso resta. Lo retenido por cancelar fuera de plazo ya está en su cobro: se muestra como penalidad, sin sumarlo dos veces. */
    private Movimiento movimientoDe(Cobro c) {
        if (c.getConcepto() == ConceptoCobro.REEMBOLSO) {
            return new Movimiento(c.getFecha().toLocalDate(), REEMBOLSOS, c.getMontoFinal().negate(), null);
        }
        return new Movimiento(c.getFecha().toLocalDate(), conceptoDe(c), c.getMontoFinal(), c);
    }

    private ReporteIngresosResponse armar(List<Movimiento> delPeriodo, List<Movimiento> anterior, LocalDate desde, LocalDate hasta) {
        BigDecimal total = suma(delPeriodo);
        BigDecimal senas = sumaDeConcepto(delPeriodo, ConceptoCobro.SEÑA);
        BigDecimal saldos = sumaDeConcepto(delPeriodo, ConceptoCobro.SALDO);
        List<Cobro> cobrosDelPeriodo = delPeriodo.stream().map(Movimiento::cobro).filter(c -> c != null).toList();
        BigDecimal facturado = cobrosDelPeriodo.stream().map(Cobro::getMontoBase).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal bonificado = cobrosDelPeriodo.stream().map(this::bonificadoDe).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<MontoDelDia> evolucion = evolucion(delPeriodo, desde, hasta);

        return new ReporteIngresosResponse(
                total,
                Porcentajes.variacion(total.doubleValue(), suma(anterior).doubleValue()),
                senas,
                Porcentajes.de(senas, total) + "% del total",
                saldos,
                Porcentajes.de(saldos, total) + "% del total",
                bonificado,
                "-" + Porcentajes.de(bonificado, facturado) + "% sobre lo facturado",
                evolucion,
                textoPico(delPeriodo),
                porMedio(cobrosDelPeriodo),
                porConcepto(delPeriodo, total),
                descuentos(cobrosDelPeriodo));
    }

    private record Movimiento(LocalDate fecha, String concepto, BigDecimal monto, Cobro cobro) {
    }

    private String conceptoDe(Cobro cobro) {
        if (cobro.getReserva() != null) {
            return positivo(cobro.getReserva().getMontoPenalidad()) ? PENALIDADES : ALQUILER;
        }
        if (cobro.getTorneo() != null || cobro.getConcepto() == ConceptoCobro.INSCRIPCION) {
            return TORNEOS;
        }
        return ACTIVIDADES;
    }

    private boolean coincide(Movimiento movimiento, String filtro) {
        return filtro == null || filtro.isBlank() || filtro.equalsIgnoreCase("Todos") || filtro.equalsIgnoreCase(movimiento.concepto());
    }

    private List<Movimiento> enRango(List<Movimiento> movimientos, LocalDate desde, LocalDate hasta) {
        return movimientos.stream().filter(m -> !m.fecha().isBefore(desde) && !m.fecha().isAfter(hasta)).toList();
    }

    private BigDecimal suma(List<Movimiento> movimientos) {
        return movimientos.stream().map(Movimiento::monto).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumaDeConcepto(List<Movimiento> movimientos, ConceptoCobro concepto) {
        return movimientos.stream()
                .filter(m -> m.cobro() != null && m.cobro().getConcepto() == concepto)
                .map(Movimiento::monto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal bonificadoDe(Cobro cobro) {
        BigDecimal diferencia = cobro.getMontoBase().subtract(cobro.getMontoFinal());
        return diferencia.signum() > 0 ? diferencia : BigDecimal.ZERO;
    }

    private List<MontoDelDia> evolucion(List<Movimiento> movimientos, LocalDate desde, LocalDate hasta) {
        Map<LocalDate, BigDecimal> porFecha = movimientos.stream()
                .collect(Collectors.groupingBy(Movimiento::fecha, Collectors.reducing(BigDecimal.ZERO, Movimiento::monto, BigDecimal::add)));
        List<MontoDelDia> dias = new ArrayList<>();
        for (LocalDate fecha = desde; !fecha.isAfter(hasta); fecha = fecha.plusDays(1)) {
            dias.add(new MontoDelDia(fecha.getDayOfMonth(), porFecha.getOrDefault(fecha, BigDecimal.ZERO)));
        }
        return dias;
    }

    private String textoPico(List<Movimiento> movimientos) {
        return movimientos.stream()
                .collect(Collectors.groupingBy(Movimiento::fecha, Collectors.reducing(BigDecimal.ZERO, Movimiento::monto, BigDecimal::add)))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> "Pico el " + e.getKey().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " con $ " + e.getValue().toPlainString())
                .orElse("Sin cobros en el período");
    }

    private List<PorMedio> porMedio(List<Cobro> cobros) {
        BigDecimal total = cobros.stream().map(Cobro::getMontoFinal).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<MedioPago, BigDecimal> montos = cobros.stream()
                .collect(Collectors.groupingBy(Cobro::getMedioPago, Collectors.reducing(BigDecimal.ZERO, Cobro::getMontoFinal, BigDecimal::add)));
        return montos.entrySet().stream()
                .sorted(Map.Entry.<MedioPago, BigDecimal>comparingByValue().reversed())
                .map(e -> new PorMedio(ETIQUETA_MEDIO.get(e.getKey()), e.getValue(), Porcentajes.de(e.getValue(), total)))
                .toList();
    }

    private List<PorConcepto> porConcepto(List<Movimiento> movimientos, BigDecimal total) {
        Map<String, BigDecimal> montos = new LinkedHashMap<>();
        movimientos.forEach(m -> montos.merge(m.concepto(), m.monto(), BigDecimal::add));
        return montos.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .map(e -> new PorConcepto(e.getKey(), e.getValue(), Porcentajes.de(e.getValue(), total)))
                .toList();
    }

    private List<DescuentoAplicado> descuentos(List<Cobro> cobros) {
        Set<Long> ids = cobros.stream().map(Cobro::getId).collect(Collectors.toSet());
        Map<String, List<CobroDescuento>> porDescuento = cobroDescuentoRepository.findAll().stream()
                .filter(cd -> ids.contains(cd.getCobro().getId()))
                .collect(Collectors.groupingBy(cd -> nombreDescuento(cd)));
        return porDescuento.entrySet().stream()
                .map(e -> new DescuentoAplicado(e.getKey(), e.getValue().size(),
                        e.getValue().stream().map(cd -> bonificadoDe(cd.getCobro())).reduce(BigDecimal.ZERO, BigDecimal::add)))
                .sorted(Comparator.comparing(DescuentoAplicado::bonificado).reversed())
                .toList();
    }

    private String nombreDescuento(CobroDescuento aplicado) {
        var descuento = aplicado.getDescuento();
        String valor = descuento.getPorcentaje() != null ? descuento.getPorcentaje().stripTrailingZeros().toPlainString() + "%" : "$" + descuento.getMontoFijo();
        return descuento.getTipo().name().replace('_', ' ').toLowerCase() + " (" + valor + ")";
    }

    private boolean positivo(BigDecimal monto) {
        return monto != null && monto.signum() > 0;
    }
}
