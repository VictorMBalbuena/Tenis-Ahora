package com.tenisahora.modules.insumo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record InsumoResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "Raquetas") String nombre,
        CategoriaInsumoResponse categoria,
        @Schema(description = "Unidades libres en el depósito", example = "24") Integer cantidadDisponible,
        @Schema(example = "6") Integer cantidadMinima,
        @Schema(description = "Unidades asignadas a reservas en curso que todavía no se repusieron", example = "8") int enUso,
        @Schema(description = "Stock físico total (disponible + en uso)", example = "32") Integer stockTotal,
        EstadoStock estado
) {

    public InsumoResponse(Long id, String nombre, CategoriaInsumoResponse categoria, Integer cantidadDisponible, Integer cantidadMinima, int enUso, EstadoStock estado) {
        this(id, nombre, categoria, cantidadDisponible, cantidadMinima, enUso,
                (cantidadDisponible != null ? cantidadDisponible : 0) + enUso, estado);
    }
}
