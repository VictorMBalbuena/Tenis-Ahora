package com.tenisahora.modules.descuento.model;

import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.SolicitudInvalidaException;

import java.util.Locale;

/**
 * El código con el que se pide un descuento al reservar, por ejemplo "CYBERWEEK".
 *
 * Existe para que la regla de qué es un código válido esté en un solo lugar: la usa el alta (al guardarlo), la
 * búsqueda (al validarlo) y la reserva (al aplicarlo). Si cada una normalizara por su cuenta, "cyberweek" podría
 * guardarse de una forma y buscarse de otra, y el socio vería que su código no existe.
 */
public final class CodigoDeDescuento {

    /** Letras y números nada más: un código se dicta por teléfono y se escribe a mano. */
    private static final String FORMATO = "[A-Z0-9]+";

    public static final int LARGO_MAXIMO = 30;

    private CodigoDeDescuento() {
    }

    /** Lo normaliza sin validarlo: sirve para buscar, donde un código inexistente no es un error. */
    public static String normalizar(String codigo) {
        return codigo == null ? "" : codigo.strip().toUpperCase(Locale.ROOT);
    }

    /**
     * Lo normaliza y exige que sirva como código.
     *
     * @throws SolicitudInvalidaException si está vacío, es muy largo o tiene algo que no sea letra o número
     */
    public static String exigir(String codigo) {
        String limpio = normalizar(codigo);
        if (limpio.isEmpty()) {
            throw invalido("El código del descuento es obligatorio");
        }
        if (limpio.length() > LARGO_MAXIMO) {
            throw invalido("El código no puede superar los " + LARGO_MAXIMO + " caracteres");
        }
        if (!limpio.matches(FORMATO)) {
            throw invalido("El código solo puede tener letras y números, sin espacios ni símbolos");
        }
        return limpio;
    }

    private static SolicitudInvalidaException invalido(String mensaje) {
        return new SolicitudInvalidaException(CodigoError.BENEFICIO_INVALIDO, mensaje);
    }
}
