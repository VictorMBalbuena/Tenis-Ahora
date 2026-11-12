package com.tenisahora.modules.actividad.repository;

import com.tenisahora.modules.actividad.entity.ActividadDia;
import com.tenisahora.modules.actividad.entity.EstadoActividad;
import com.tenisahora.modules.actividad.entity.Entrenamiento;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.DayOfWeek;
import java.time.LocalDate;

/** Filtros combinables de la búsqueda de entrenamientos (CU39): cada uno se agrega solo si el pedido lo trae. */
public final class EntrenamientoSpecs {

    private EntrenamientoSpecs() {
    }

    public static Specification<Entrenamiento> deCancha(Long canchaId) {
        return (entrenamiento, consulta, cb) -> cb.equal(entrenamiento.get("cancha").get("id"), canchaId);
    }

    public static Specification<Entrenamiento> deEntrenador(Long entrenadorId) {
        return (entrenamiento, consulta, cb) -> cb.equal(entrenamiento.get("entrenador").get("id"), entrenadorId);
    }

    public static Specification<Entrenamiento> conEstado(EstadoActividad estado) {
        return (entrenamiento, consulta, cb) -> cb.equal(entrenamiento.get("estado"), estado);
    }

    /** Entrenamientos cuyo rango de fechas incluye esa fecha. */
    public static Specification<Entrenamiento> enFecha(LocalDate fecha) {
        return (entrenamiento, consulta, cb) -> cb.and(
                cb.lessThanOrEqualTo(entrenamiento.get("fechaInicio"), fecha),
                cb.greaterThanOrEqualTo(entrenamiento.get("fechaFin"), fecha));
    }

    /** Entrenamientos que se dictan ese día de la semana. */
    public static Specification<Entrenamiento> enDia(DayOfWeek dia) {
        return (entrenamiento, consulta, cb) -> {
            Subquery<Long> sub = consulta.subquery(Long.class);
            Root<ActividadDia> actividadDia = sub.from(ActividadDia.class);
            sub.select(actividadDia.get("id")).where(cb.equal(actividadDia.get("actividad"), entrenamiento), cb.equal(actividadDia.get("diaSemana"), dia));
            return cb.exists(sub);
        };
    }
}
