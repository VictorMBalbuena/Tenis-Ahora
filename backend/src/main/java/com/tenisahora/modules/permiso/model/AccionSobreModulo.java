package com.tenisahora.modules.permiso.model;

import org.springframework.http.HttpMethod;

/** Las cuatro acciones que se marcan por módulo (CU75, RF75). */
public enum AccionSobreModulo {

    VER("Ver"),
    CREAR("Crear"),
    EDITAR("Editar"),
    ELIMINAR("Eliminar");

    private final String etiqueta;

    AccionSobreModulo(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String etiqueta() {
        return etiqueta;
    }

    /** Qué acción representa un pedido HTTP: es lo que conecta la matriz con lo que pasa de verdad. */
    public static AccionSobreModulo deMetodo(String metodo) {
        if (HttpMethod.GET.matches(metodo) || HttpMethod.HEAD.matches(metodo)) {
            return VER;
        }
        if (HttpMethod.POST.matches(metodo)) {
            return CREAR;
        }
        if (HttpMethod.DELETE.matches(metodo)) {
            return ELIMINAR;
        }
        return EDITAR; // PUT y PATCH
    }
}
