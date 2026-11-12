package com.tenisahora.modules.actividad.service;

import com.tenisahora.modules.actividad.entity.InscripcionClase;

import java.math.BigDecimal;

/** Lo que este módulo necesita saber de los cobros, sin depender de ellos: lo implementa el módulo de cobros. */
public interface PagosDeInscripcion {

    record Resumen(BigDecimal montoSenia, BigDecimal baseCubierta, BigDecimal cobrado) {
    }

    Resumen resumen(InscripcionClase inscripcion);
}
