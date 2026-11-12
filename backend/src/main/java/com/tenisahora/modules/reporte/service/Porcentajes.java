package com.tenisahora.modules.reporte.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class Porcentajes {

    private Porcentajes() {
    }

    static int de(double parte, double total) {
        return total <= 0 ? 0 : (int) Math.round(parte * 100 / total);
    }

    static int de(BigDecimal parte, BigDecimal total) {
        if (total.signum() <= 0) {
            return 0;
        }
        return parte.multiply(BigDecimal.valueOf(100)).divide(total, 0, RoundingMode.HALF_UP).intValue();
    }

    static String variacion(double actual, double anterior) {
        if (anterior <= 0) {
            return actual > 0 ? "Sin datos del período anterior" : "";
        }
        int cambio = (int) Math.round((actual - anterior) * 100 / anterior);
        return (cambio >= 0 ? "+" : "") + cambio + "% vs período anterior";
    }
}
