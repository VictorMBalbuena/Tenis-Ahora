package com.tenisahora.modules.descuento.service;

import com.tenisahora.modules.descuento.dto.DescuentoRequest;
import com.tenisahora.modules.descuento.dto.DescuentoResponse;
import com.tenisahora.modules.descuento.entity.TipoDescuento;
import com.tenisahora.shared.dto.PageResponse;

public interface DescuentoService {
    DescuentoResponse registrar(DescuentoRequest request);
    DescuentoResponse consultar(Long id);
    PageResponse<DescuentoResponse> buscar(TipoDescuento tipo, Boolean activo, String busqueda, int page, int size);
    DescuentoResponse modificar(Long id, DescuentoRequest request);
    void eliminar(Long id);
    byte[] imprimir(Long id);
}
