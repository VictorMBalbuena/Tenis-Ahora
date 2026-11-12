package com.tenisahora.modules.insumo.service;

import com.tenisahora.modules.insumo.dto.CategoriaInsumoRequest;
import com.tenisahora.modules.insumo.dto.CategoriaInsumoResponse;
import com.tenisahora.modules.insumo.entity.CategoriaInsumo;
import com.tenisahora.modules.insumo.mapper.InsumoMapper;
import com.tenisahora.modules.insumo.repository.CategoriaInsumoRepository;
import com.tenisahora.modules.insumo.repository.InsumoRepository;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.ConflictoException;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoriaInsumoServiceImpl implements CategoriaInsumoService {

    private final CategoriaInsumoRepository categoriaInsumoRepository;
    private final InsumoRepository insumoRepository;
    private final InsumoMapper insumoMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaInsumoResponse> listar() {
        return categoriaInsumoRepository.findAllByOrderByNombreAsc().stream()
                .map(insumoMapper::aRespuestaCategoria)
                .toList();
    }

    @Override
    @Transactional
    public CategoriaInsumoResponse registrar(CategoriaInsumoRequest request) {
        if (categoriaInsumoRepository.existsByNombreIgnoreCase(request.nombre())) {
            throw new ConflictoException(CodigoError.CATEGORIA_INSUMO_NOMBRE_DUPLICADO, "Ya existe una categoría con ese nombre");
        }
        CategoriaInsumo guardada = categoriaInsumoRepository.save(new CategoriaInsumo(request.nombre()));
        return insumoMapper.aRespuestaCategoria(guardada);
    }

    @Override
    @Transactional
    public CategoriaInsumoResponse modificar(Long id, CategoriaInsumoRequest request) {
        CategoriaInsumo categoria = categoriaInsumoRepository.findById(id).orElseThrow(
                () -> new RecursoNoEncontradoException(CodigoError.CATEGORIA_INSUMO_NO_ENCONTRADA, "Categoría de insumo no encontrada"));
        if (categoriaInsumoRepository.existsByNombreIgnoreCaseAndIdNot(request.nombre(), id)) {
            throw new ConflictoException(CodigoError.CATEGORIA_INSUMO_NOMBRE_DUPLICADO, "Ya existe una categoría con ese nombre");
        }
        categoria.setNombre(request.nombre());
        return insumoMapper.aRespuestaCategoria(categoriaInsumoRepository.save(categoria));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        CategoriaInsumo categoria = categoriaInsumoRepository.findById(id).orElseThrow(
                () -> new RecursoNoEncontradoException(CodigoError.CATEGORIA_INSUMO_NO_ENCONTRADA, "Categoría de insumo no encontrada"));
        long cantidad = insumoRepository.countByCategoriaId(id);
        if (cantidad > 0) {
            throw new ConflictoException(CodigoError.CATEGORIA_INSUMO_CON_INSUMOS,
                    "Hay " + cantidad + " insumo(s) con esta categoría. Reasignalos antes de eliminarla");
        }
        categoriaInsumoRepository.delete(categoria);
    }
}
