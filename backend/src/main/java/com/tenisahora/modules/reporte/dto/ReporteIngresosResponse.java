package com.tenisahora.modules.reporte.dto;

import java.math.BigDecimal;
import java.util.List;

public record ReporteIngresosResponse(
        BigDecimal ingresosTotales,
        String variacionMesPrevio,
        BigDecimal senas,
        String porcentajeSenas,
        BigDecimal saldosFinales,
        String porcentajeSaldos,
        BigDecimal bonificadoDescuentos,
        String porcentajeBonificado,
        List<MontoDelDia> evolucionDiaria,
        String picoTexto,
        List<PorMedio> porMedioPago,
        List<PorConcepto> porConcepto,
        List<DescuentoAplicado> descuentosAplicados) {

    public record MontoDelDia(int dia, BigDecimal monto) {
    }

    public record PorMedio(String medio, BigDecimal monto, int porcentaje) {
    }

    public record PorConcepto(String concepto, BigDecimal monto, int porcentaje) {
    }

    public record DescuentoAplicado(String descuento, long aplicaciones, BigDecimal bonificado) {
    }
}
