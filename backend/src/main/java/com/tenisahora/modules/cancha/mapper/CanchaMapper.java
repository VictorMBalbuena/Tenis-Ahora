package com.tenisahora.modules.cancha.mapper;

import com.tenisahora.modules.cancha.dto.CanchaRequest;
import com.tenisahora.modules.cancha.dto.CanchaResponse;
import com.tenisahora.modules.cancha.dto.CanchaUpdateRequest;
import com.tenisahora.modules.cancha.entity.Cancha;
import org.springframework.stereotype.Component;

@Component
public class CanchaMapper {

    public Cancha aEntidad(CanchaRequest request) {
        Cancha cancha = new Cancha();
        cancha.setIdentificador(request.identificador());
        cancha.setTipoSuperficie(request.tipoSuperficie());
        cancha.setCapacidad(request.capacidad());
        cancha.setPrecioPorHora(request.precioPorHora());
        cancha.setActiva(true);
        cancha.setTechada(request.techada() != null && request.techada());
        cancha.setIluminacion(request.iluminacion() == null || request.iluminacion());
        cancha.setTribuna(request.tribuna() != null && request.tribuna());
        cancha.setAptaTorneos(request.aptaTorneos() != null && request.aptaTorneos());
        cancha.setObservaciones(request.observaciones());
        return cancha;
    }

    public CanchaResponse aRespuesta(Cancha cancha, boolean enMantenimiento) {
        Long tipoCanchaId = cancha.getTipoCancha() != null ? cancha.getTipoCancha().getId() : null;
        String tipoCanchaNombre = cancha.getTipoCancha() != null ? cancha.getTipoCancha().getNombre() : null;
        String superficie = cancha.getSuperficie();

        return new CanchaResponse(cancha.getId(), cancha.getIdentificador(), superficie,
                cancha.getCapacidad(), cancha.getPrecioPorHora(), cancha.getActiva(), enMantenimiento,
                tipoCanchaId, tipoCanchaNombre,
                Boolean.TRUE.equals(cancha.getTechada()),
                !Boolean.FALSE.equals(cancha.getIluminacion()),
                Boolean.TRUE.equals(cancha.getTribuna()),
                Boolean.TRUE.equals(cancha.getAptaTorneos()),
                cancha.getObservaciones(),
                superficie);
    }

    /** Modificación parcial: lo que viene nulo no se toca. */
    public void aplicar(CanchaUpdateRequest request, Cancha cancha) {
        aplicarDatosPrincipales(request, cancha);
        aplicarCaracteristicas(request, cancha);
    }

    private void aplicarDatosPrincipales(CanchaUpdateRequest request, Cancha cancha) {
        if (request.identificador() != null) {
            cancha.setIdentificador(request.identificador());
        }
        if (request.tipoSuperficie() != null) {
            cancha.setTipoSuperficie(request.tipoSuperficie());
        }
        if (request.capacidad() != null) {
            cancha.setCapacidad(request.capacidad());
        }
        if (request.precioPorHora() != null) {
            cancha.setPrecioPorHora(request.precioPorHora());
        }
        if (request.activa() != null) {
            cancha.setActiva(request.activa());
        }
    }

    private void aplicarCaracteristicas(CanchaUpdateRequest request, Cancha cancha) {
        if (request.techada() != null) {
            cancha.setTechada(request.techada());
        }
        if (request.iluminacion() != null) {
            cancha.setIluminacion(request.iluminacion());
        }
        if (request.tribuna() != null) {
            cancha.setTribuna(request.tribuna());
        }
        if (request.aptaTorneos() != null) {
            cancha.setAptaTorneos(request.aptaTorneos());
        }
        if (request.observaciones() != null) {
            cancha.setObservaciones(request.observaciones());
        }
    }
}
