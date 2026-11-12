package com.tenisahora.modules.permiso.dto;

import com.tenisahora.modules.permiso.model.AccionSobreModulo;
import com.tenisahora.modules.permiso.model.ModuloDelSistema;
import com.tenisahora.modules.permiso.model.PermisoEspecial;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Arrays;
import java.util.List;

/**
 * Qué se puede marcar en la matriz. La pantalla arma las columnas y las filas con esto, así no hay una lista de
 * módulos escrita a mano en el frontend que se desincronice de la del backend.
 */
@Schema(description = "Módulos, acciones y permisos especiales disponibles (CU75)")
public record CatalogoDePermisosResponse(List<Item> modulos, List<Item> acciones, List<Item> especiales) {

    @Schema(description = "Un valor con su etiqueta para mostrar")
    public record Item(String codigo, String etiqueta) {
    }

    public static CatalogoDePermisosResponse armar() {
        return new CatalogoDePermisosResponse(
                Arrays.stream(ModuloDelSistema.values())
                        .map(m -> new Item(m.name(), m.etiqueta())).toList(),
                Arrays.stream(AccionSobreModulo.values())
                        .map(a -> new Item(a.name(), a.etiqueta())).toList(),
                Arrays.stream(PermisoEspecial.values())
                        .map(e -> new Item(e.name(), e.etiqueta())).toList());
    }
}
