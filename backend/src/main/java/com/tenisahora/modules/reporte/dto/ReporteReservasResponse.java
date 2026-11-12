package com.tenisahora.modules.reporte.dto;

import java.math.BigDecimal;
import java.util.List;

public record ReporteReservasResponse(
        long reservasRegistradas,
        String variacionReservas,
        long confirmadas,
        String porcentajeConfirmadas,
        long canceladas,
        String detalleCanceladas,
        String ocupacionMedia,
        String variacionOcupacion,
        List<PorCancha> porCancha,
        List<FranjaOcupacion> ocupacionHoraria,
        String textoFranjaPico,
        Cancelaciones cancelacionesDetalle) {

    public record PorCancha(String cancha, long cantidad, int porcentaje) {
    }

    public record FranjaOcupacion(String hora, int porcentaje, boolean pico) {
    }

    public record Cancelaciones(long dentroPlazo, long fueraPlazo, BigDecimal montoRetenido) {
    }
}
