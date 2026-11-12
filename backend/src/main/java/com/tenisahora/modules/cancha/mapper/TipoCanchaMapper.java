package com.tenisahora.modules.cancha.mapper;

import com.tenisahora.modules.cancha.dto.TipoCanchaRequest;
import com.tenisahora.modules.cancha.dto.TipoCanchaResponse;
import com.tenisahora.modules.cancha.entity.TipoCancha;
import org.springframework.stereotype.Component;

@Component
public class TipoCanchaMapper {

    public TipoCancha aEntidad(TipoCanchaRequest request) {
        TipoCancha tipo = new TipoCancha();
        tipo.setNombre(request.nombre());
        tipo.setSuperficie(request.superficie());
        tipo.setCapacidad(request.capacidad());
        tipo.setDescripcion(request.descripcion());
        tipo.setActivo(true);
        return tipo;
    }

    public TipoCanchaResponse aRespuesta(TipoCancha tipo, int cantidadCanchas) {
        return new TipoCanchaResponse(
                tipo.getId(),
                tipo.getNombre(),
                tipo.getSuperficie(),
                tipo.getCapacidad(),
                tipo.getDescripcion(),
                tipo.getActivo(),
                cantidadCanchas
        );
    }

    public void aplicar(TipoCanchaRequest request, TipoCancha tipo) {
        tipo.setNombre(request.nombre());
        tipo.setSuperficie(request.superficie());
        tipo.setCapacidad(request.capacidad());
        tipo.setDescripcion(request.descripcion());
    }
}

