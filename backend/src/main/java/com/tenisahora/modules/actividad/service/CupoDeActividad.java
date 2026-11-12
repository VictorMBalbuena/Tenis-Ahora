package com.tenisahora.modules.actividad.service;

import com.tenisahora.modules.actividad.repository.InscripcionClaseRepository;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** CU70: el cupo se fija al programar la clase o el entrenamiento, con el tope general de configuración (RD01, RD04). */
@Component
@RequiredArgsConstructor
public class CupoDeActividad {

    private final InscripcionClaseRepository inscripcionRepository;
    private final ActividadesProperties propiedades;

    /** Aplica el default si no viene cupo, y rechaza el que supera el tope general (A1 de CU70). El &lt; 1 lo corta @Min(1) en el DTO. */
    public int asignar(Integer pedido) {
        int cupo = pedido != null ? pedido : propiedades.cupoPorDefecto();
        if (cupo > propiedades.cupoMaximoGeneral()) {
            throw new ReglaDeNegocioException(CodigoError.CUPO_SUPERA_EL_LIMITE,
                    "El cupo no puede superar el límite del club (" + propiedades.cupoMaximoGeneral() + ")");
        }
        return cupo;
    }

    /** CU80/CU82 A1: no deja inscribir si ya no hay lugar. */
    public void asegurarQueHayLugar(Long actividadId, Integer cupoMaximo) {
        if (ocupado(actividadId) >= cupoMaximo) {
            throw new ReglaDeNegocioException(CodigoError.CUPO_COMPLETO, "La actividad ya no tiene cupo disponible");
        }
    }

    /** Al modificar: no se puede bajar el cupo por debajo de los ya inscriptos. */
    public void asegurarCupoSuficienteParaModificar(Long actividadId, int nuevoCupo) {
        if (nuevoCupo < ocupado(actividadId)) {
            throw new ReglaDeNegocioException(CodigoError.CUPO_MENOR_A_INSCRIPTOS,
                    "El cupo no puede ser menor a la cantidad de inscriptos activos");
        }
    }

    public int ocupado(Long actividadId) {
        return (int) inscripcionRepository.countByActividadIdAndActivaTrue(actividadId);
    }
}
