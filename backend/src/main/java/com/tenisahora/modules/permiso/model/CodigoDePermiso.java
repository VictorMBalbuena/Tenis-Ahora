package com.tenisahora.modules.permiso.model;

import java.util.Optional;

/**
 * Cómo se guarda y cómo se nombra un permiso suelto. La matriz se guarda como un conjunto de códigos de texto
 * ("COBROS:VER", "ESPECIAL:ANULAR_COBROS") en vez de una fila por casilla: la pantalla siempre manda la matriz
 * entera, así que no hace falta calcular diferencias, y leer la tabla a mano se entiende sin un join.
 */
public final class CodigoDePermiso {

    private static final String PREFIJO_ESPECIAL = "ESPECIAL:";
    private static final String SEPARADOR = ":";

    private CodigoDePermiso() {
    }

    public static String de(ModuloDelSistema modulo, AccionSobreModulo accion) {
        return modulo.name() + SEPARADOR + accion.name();
    }

    public static String de(PermisoEspecial especial) {
        return PREFIJO_ESPECIAL + especial.name();
    }

    /** La autoridad de Spring Security que lleva la sesión: "PERM_COBROS:VER". */
    public static String comoAutoridad(String codigo) {
        return "PERM_" + codigo;
    }

    public static Optional<ModuloDelSistema> moduloDe(String codigo) {
        if (codigo == null || codigo.startsWith(PREFIJO_ESPECIAL)) {
            return Optional.empty();
        }
        return valorDe(ModuloDelSistema.class, codigo.substring(0, Math.max(codigo.indexOf(SEPARADOR), 0)));
    }

    public static Optional<AccionSobreModulo> accionDe(String codigo) {
        if (codigo == null || codigo.startsWith(PREFIJO_ESPECIAL) || !codigo.contains(SEPARADOR)) {
            return Optional.empty();
        }
        return valorDe(AccionSobreModulo.class, codigo.substring(codigo.indexOf(SEPARADOR) + 1));
    }

    public static Optional<PermisoEspecial> especialDe(String codigo) {
        if (codigo == null || !codigo.startsWith(PREFIJO_ESPECIAL)) {
            return Optional.empty();
        }
        return valorDe(PermisoEspecial.class, codigo.substring(PREFIJO_ESPECIAL.length()));
    }

    /** Un código que quedó guardado de una versión anterior y ya no existe se ignora, no rompe la pantalla. */
    private static <E extends Enum<E>> Optional<E> valorDe(Class<E> tipo, String nombre) {
        try {
            return Optional.of(Enum.valueOf(tipo, nombre));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
