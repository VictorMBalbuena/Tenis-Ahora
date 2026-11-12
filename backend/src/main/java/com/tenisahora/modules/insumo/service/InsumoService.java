package com.tenisahora.modules.insumo.service;

import com.tenisahora.modules.insumo.dto.EstadoStock;
import com.tenisahora.modules.insumo.dto.InsumoRequest;
import com.tenisahora.modules.insumo.dto.InsumoResponse;
import com.tenisahora.modules.insumo.dto.InsumoUpdateRequest;
import com.tenisahora.modules.insumo.dto.ResumenInsumosResponse;
import com.tenisahora.shared.dto.PageResponse;

public interface InsumoService {

    /** CU10 */
    InsumoResponse registrar(InsumoRequest request, Long actorId);

    /** CU11 */
    InsumoResponse consultar(Long id);

    /** CU102 y CU11: lista los insumos activos, con filtros opcionales por texto en el nombre, categoría y estado de stock. */
    PageResponse<InsumoResponse> buscar(String busqueda, Long categoriaId, EstadoStock estado, int page, int size);

    /** CU12 */
    InsumoResponse modificar(Long id, InsumoUpdateRequest request, Long actorId);

    /** CU101: baja lógica; no se hace si hay reservas vigentes con el insumo asignado. */
    void eliminar(Long id, Long actorId);

    /** Totales de la tira de KPIs del listado, sobre todos los insumos activos. */
    ResumenInsumosResponse resumen();
}
