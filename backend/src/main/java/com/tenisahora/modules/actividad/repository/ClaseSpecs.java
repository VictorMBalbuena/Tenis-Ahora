package com.tenisahora.modules.actividad.repository;

import com.tenisahora.modules.actividad.entity.ActividadDia;
import com.tenisahora.modules.actividad.entity.Clase;
import com.tenisahora.modules.actividad.entity.EstadoActividad;
import com.tenisahora.modules.actividad.entity.NivelClase;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.DayOfWeek;
import java.time.LocalDate;

/** Filtros combinables de la búsqueda de clases (CU34): cada uno se agrega solo si el pedido lo trae. */
public final class ClaseSpecs {

    private ClaseSpecs() {
    }

    public static Specification<Clase> deCancha(Long canchaId) {
        return (clase, consulta, cb) -> cb.equal(clase.get("cancha").get("id"), canchaId);
    }

    public static Specification<Clase> deProfesor(Long profesorId) {
        return (clase, consulta, cb) -> cb.equal(clase.get("profesor").get("id"), profesorId);
    }

    public static Specification<Clase> conEstado(EstadoActividad estado) {
        return (clase, consulta, cb) -> cb.equal(clase.get("estado"), estado);
    }

    public static Specification<Clase> conNivel(NivelClase nivel) {
        return (clase, consulta, cb) -> cb.equal(clase.get("nivel"), nivel);
    }

    /** Clases cuyo rango de fechas incluye esa fecha. */
    public static Specification<Clase> enFecha(LocalDate fecha) {
        return (clase, consulta, cb) -> cb.and(
                cb.lessThanOrEqualTo(clase.get("fechaInicio"), fecha),
                cb.greaterThanOrEqualTo(clase.get("fechaFin"), fecha));
    }

    /** Clases que se dictan ese día de la semana. */
    public static Specification<Clase> enDia(DayOfWeek dia) {
        return (clase, consulta, cb) -> {
            Subquery<Long> sub = consulta.subquery(Long.class);
            Root<ActividadDia> actividadDia = sub.from(ActividadDia.class);
            sub.select(actividadDia.get("id")).where(cb.equal(actividadDia.get("actividad"), clase), cb.equal(actividadDia.get("diaSemana"), dia));
            return cb.exists(sub);
        };
    }
}
