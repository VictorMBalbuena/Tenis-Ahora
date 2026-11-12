package com.tenisahora.shared.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/** Listado paginado, igual en todo el sistema. */
@Schema(description = "Página de resultados")
public record PageResponse<T>(
        List<T> contenido,
        @Schema(example = "0") int pagina,
        @Schema(example = "20") int tamanio,
        @Schema(example = "134") long totalElementos,
        @Schema(example = "7") int totalPaginas
) {

    public static <E, T> PageResponse<T> de(Page<E> pagina, Function<E, T> conversor) {
        return new PageResponse<>(
                pagina.getContent().stream().map(conversor).toList(),
                pagina.getNumber(),
                pagina.getSize(),
                pagina.getTotalElements(),
                pagina.getTotalPages());
    }
}
