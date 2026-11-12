package com.tenisahora.modules.permiso.model;

import org.springframework.http.HttpMethod;

import java.util.function.BiPredicate;

/**
 * Los cinco permisos especiales que nombra RF75, uno por uno. No son "ver, crear, editar o eliminar" sobre un
 * módulo: son acciones puntuales que el club quiere poder dar o quitar por separado.
 *
 * <p>Cada uno sabe reconocer el pedido que lo ejerce, para que el control sea real y no una pantalla decorativa.
 */
public enum PermisoEspecial {

    VALIDAR_CERTIFICACIONES("Validar certificaciones y títulos", PermisoEspecial::esUnaValidacion),
    CONFIGURAR_DESCUENTOS("Configurar descuentos", PermisoEspecial::tocaDescuentos),
    ANULAR_COBROS("Anular cobros", PermisoEspecial::anulaUnCobro),
    BLOQUEAR_CANCHAS("Bloquear canchas por mantenimiento", PermisoEspecial::tocaBloqueos),
    GESTIONAR_USUARIOS("Gestionar usuarios, roles y permisos", PermisoEspecial::tocaRolesOCuentas);

    private final String etiqueta;

    /** Recibe el método HTTP y la ruta del pedido. */
    private final BiPredicate<String, String> reconoce;

    PermisoEspecial(String etiqueta, BiPredicate<String, String> reconoce) {
        this.etiqueta = etiqueta;
        this.reconoce = reconoce;
    }

    public String etiqueta() {
        return etiqueta;
    }

    /** Si este pedido es, justamente, el ejercicio de este permiso especial. */
    public boolean loEjerce(String metodo, String ruta) {
        return reconoce.test(metodo, ruta);
    }

    private static boolean esUnaValidacion(String metodo, String ruta) {
        return HttpMethod.POST.matches(metodo) && ruta.endsWith("/validacion");
    }

    private static boolean tocaDescuentos(String metodo, String ruta) {
        return escribe(metodo) && ruta.startsWith("/api/descuentos");
    }

    private static boolean anulaUnCobro(String metodo, String ruta) {
        return HttpMethod.DELETE.matches(metodo)
                && (ruta.startsWith("/api/cobros") || ruta.startsWith("/api/recibos"));
    }

    private static boolean tocaBloqueos(String metodo, String ruta) {
        return escribe(metodo) && ruta.contains("/bloqueos");
    }

    private static boolean tocaRolesOCuentas(String metodo, String ruta) {
        return escribe(metodo) && (ruta.startsWith("/api/roles") || ruta.startsWith("/api/cuentas"));
    }

    /** Cualquier método que no sea de lectura. */
    private static boolean escribe(String metodo) {
        return !HttpMethod.GET.matches(metodo);
    }
}
