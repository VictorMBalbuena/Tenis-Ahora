package com.tenisahora.modules.permiso.dto;

import com.tenisahora.modules.permiso.model.AccionSobreModulo;
import com.tenisahora.modules.permiso.model.CodigoDePermiso;
import com.tenisahora.modules.permiso.model.ModuloDelSistema;
import com.tenisahora.modules.permiso.model.PermisoEspecial;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * La matriz de permisos tal como la ve y la manda la pantalla (CU75): por cada módulo, qué acciones están
 * marcadas, más la lista de permisos especiales.
 *
 * <p>Viaja entera en los dos sentidos. Guardar la matriz completa en vez de marcar casilla por casilla evita que
 * dos administradores editando a la vez terminen con una mezcla de las dos configuraciones.
 */
@Schema(description = "Permisos de un rol: acciones por módulo y permisos especiales (CU75 / RF75)")
public record MatrizDePermisos(
        @Schema(description = "Por cada módulo, las acciones permitidas",
                example = "{\"COBROS\": [\"VER\", \"CREAR\"], \"RESERVAS\": [\"VER\"]}")
        Map<ModuloDelSistema, Set<AccionSobreModulo>> modulos,

        @Schema(description = "Permisos especiales concedidos", example = "[\"ANULAR_COBROS\"]")
        Set<PermisoEspecial> especiales) {

    public MatrizDePermisos {
        modulos = modulos == null ? Map.of() : modulos;
        especiales = especiales == null ? Set.of() : especiales;
    }

    /** De la matriz a los códigos que se guardan. */
    public Set<String> aCodigos() {
        Set<String> codigos = new LinkedHashSet<>();
        modulos.forEach((modulo, acciones) -> {
            if (modulo != null && acciones != null) {
                acciones.stream().filter(java.util.Objects::nonNull)
                        .forEach(accion -> codigos.add(CodigoDePermiso.de(modulo, accion)));
            }
        });
        especiales.stream().filter(java.util.Objects::nonNull)
                .forEach(especial -> codigos.add(CodigoDePermiso.de(especial)));
        return codigos;
    }

    /** De los códigos guardados a la matriz que entiende la pantalla. */
    public static MatrizDePermisos deCodigos(Set<String> codigos) {
        Map<ModuloDelSistema, Set<AccionSobreModulo>> modulos = new EnumMap<>(ModuloDelSistema.class);
        Set<PermisoEspecial> especiales = new LinkedHashSet<>();

        for (String codigo : codigos) {
            CodigoDePermiso.especialDe(codigo).ifPresent(especiales::add);
            CodigoDePermiso.moduloDe(codigo).ifPresent(modulo ->
                    CodigoDePermiso.accionDe(codigo).ifPresent(accion ->
                            modulos.computeIfAbsent(modulo, m -> new LinkedHashSet<>()).add(accion)));
        }
        return new MatrizDePermisos(modulos, especiales);
    }
}
