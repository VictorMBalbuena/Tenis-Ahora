package com.tenisahora.modules.insumo.mapper;

import com.tenisahora.modules.insumo.dto.CategoriaInsumoResponse;
import com.tenisahora.modules.insumo.dto.EstadoStock;
import com.tenisahora.modules.insumo.dto.InsumoRequest;
import com.tenisahora.modules.insumo.dto.InsumoResponse;
import com.tenisahora.modules.insumo.dto.InsumoUpdateRequest;
import com.tenisahora.modules.insumo.dto.MovimientoResponse;
import com.tenisahora.modules.insumo.entity.CategoriaInsumo;
import com.tenisahora.modules.insumo.entity.Insumo;
import com.tenisahora.modules.insumo.entity.MovimientoStock;
import org.springframework.stereotype.Component;

@Component
public class InsumoMapper {

    /** La categoría ya viene resuelta: quien llama es responsable de buscarla por su id. */
    public Insumo aEntidad(InsumoRequest request, CategoriaInsumo categoria) {
        Insumo insumo = new Insumo();
        insumo.setNombre(request.nombre());
        insumo.setCategoria(categoria);
        insumo.setCantidadDisponible(request.cantidadDisponible());
        insumo.setCantidadMinima(request.cantidadMinima());
        insumo.setActivo(true);
        return insumo;
    }

    public InsumoResponse aRespuesta(Insumo insumo, int enUso) {
        int disponible = insumo.getCantidadDisponible() != null ? insumo.getCantidadDisponible() : 0;
        int total = disponible + enUso;
        return new InsumoResponse(insumo.getId(), insumo.getNombre(), aRespuestaCategoria(insumo.getCategoria()),
                disponible, insumo.getCantidadMinima(), enUso, total, estadoDe(insumo));
    }

    public CategoriaInsumoResponse aRespuestaCategoria(CategoriaInsumo categoria) {
        return new CategoriaInsumoResponse(categoria.getId(), categoria.getNombre());
    }

    /** Sin stock si no queda nada, stock bajo si queda hasta el mínimo, disponible si queda más. Es la misma regla que usa el filtro del listado. */
    public EstadoStock estadoDe(Insumo insumo) {
        if (insumo.getCantidadDisponible() == 0) {
            return EstadoStock.SIN_STOCK;
        }
        return insumo.getCantidadDisponible() <= insumo.getCantidadMinima() ? EstadoStock.STOCK_BAJO : EstadoStock.DISPONIBLE;
    }

    /**
     * Modificación parcial: lo que viene nulo no se toca. La cantidad no se aplica acá: es un ajuste y deja rastro.
     * La categoría ya viene resuelta (no nula) solo cuando el pedido trajo un categoriaId.
     */
    public void aplicar(InsumoUpdateRequest request, Insumo insumo, CategoriaInsumo categoria) {
        if (request.nombre() != null) {
            insumo.setNombre(request.nombre());
        }
        if (categoria != null) {
            insumo.setCategoria(categoria);
        }
        if (request.cantidadMinima() != null) {
            insumo.setCantidadMinima(request.cantidadMinima());
        }
    }

    public MovimientoResponse aMovimiento(MovimientoStock movimiento) {
        return new MovimientoResponse(movimiento.getId(), movimiento.getTipo(), movimiento.getVariacion(),
                movimiento.getStockResultante(), movimiento.getMotivo(), movimiento.getFecha(), movimiento.getPersonaId());
    }
}
