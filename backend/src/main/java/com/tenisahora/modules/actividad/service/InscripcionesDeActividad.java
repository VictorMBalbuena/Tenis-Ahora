package com.tenisahora.modules.actividad.service;

import com.tenisahora.modules.actividad.dto.InscripcionRequest;
import com.tenisahora.modules.actividad.dto.InscripcionResponse;
import com.tenisahora.modules.actividad.entity.Actividad;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.shared.exception.CodigoError;

/** CU80 y CU82: inscribir y cancelar la inscripción de un socio a una clase o a un entrenamiento. */
public interface InscripcionesDeActividad {

    /**
     * El socio se inscribe a sí mismo; el personal inscribe a cualquiera. Reinscribe si estaba cancelada.
     * @param tipo clase esperada (Clase o Entrenamiento): una actividad del otro tipo responde 404 igual que si no existiera.
     * @param codigoNoEncontrada el código de error a usar en ese 404 (distinto para clase y para entrenamiento).
     */
    InscripcionResponse inscribir(Long actividadId, Class<? extends Actividad> tipo, CodigoError codigoNoEncontrada,
                                   InscripcionRequest request, Actor actor);

    /** Cancela la inscripción (no la borra): activa=false, conserva el historial. */
    void cancelar(Long actividadId, CodigoError codigoNoEncontrada, Long socioId, Actor actor);
}
