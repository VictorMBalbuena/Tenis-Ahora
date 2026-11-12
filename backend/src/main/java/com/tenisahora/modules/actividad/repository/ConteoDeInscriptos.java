package com.tenisahora.modules.actividad.repository;

/** Proyección de una consulta agrupada: cuántos inscriptos activos tiene cada actividad. */
public interface ConteoDeInscriptos {

    Long getActividadId();

    Long getCantidad();
}
