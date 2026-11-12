package com.tenisahora.security;

import java.util.Set;

/**
 * Puerto que el filtro de seguridad usa para saber qué permisos tiene una sesión (CU75). Lo implementa el módulo
 * de permisos, así security no depende de ningún módulo.
 */
public interface ConsultaDePermisos {

    /**
     * Los códigos de permiso de esa persona: los de su rol personalizado si tiene uno, y si no los de su rol
     * base. Devuelve un conjunto vacío si no hay nada configurado.
     *
     * @param personaId quién hace el pedido
     * @param rol su rol base, que el filtro ya validó contra la base
     */
    Set<String> permisosDe(Long personaId, String rol);
}
