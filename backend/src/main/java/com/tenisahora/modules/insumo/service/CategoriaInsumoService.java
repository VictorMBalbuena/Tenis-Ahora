package com.tenisahora.modules.insumo.service;

import com.tenisahora.modules.insumo.dto.CategoriaInsumoRequest;
import com.tenisahora.modules.insumo.dto.CategoriaInsumoResponse;

import java.util.List;

public interface CategoriaInsumoService {

    List<CategoriaInsumoResponse> listar();

    CategoriaInsumoResponse registrar(CategoriaInsumoRequest request);

    CategoriaInsumoResponse modificar(Long id, CategoriaInsumoRequest request);

    void eliminar(Long id);
}
