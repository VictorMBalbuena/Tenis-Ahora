package com.tenisahora.modules.cancha.repository;

import com.tenisahora.modules.cancha.entity.Cancha;
import com.tenisahora.modules.cancha.entity.Disponibilidad;
import com.tenisahora.modules.cancha.entity.EstadoDisponibilidad;
import com.tenisahora.shared.util.Textos;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalTime;

/** Filtros combinables del listado de canchas (CU08), mismo criterio que InsumoSpecs. */
public final class CanchaSpecs {

    private CanchaSpecs() {
    }

    public static Specification<Cancha> deSuperficie(String superficie) {
        if (superficie == null || superficie.isBlank()) {
            return null;
        }
        String clean = "%" + superficie.trim().toLowerCase() + "%";
        return (cancha, consulta, cb) -> {
            var joinTipo = cancha.join("tipoCancha", jakarta.persistence.criteria.JoinType.LEFT);
            return cb.or(
                    cb.like(cb.lower(joinTipo.get("superficie")), clean),
                    cb.like(cb.lower(cancha.get("tipoSuperficie")), clean)
            );
        };
    }

    public static Specification<Cancha> deSuperficieTexto(String superficieTexto) {
        return deSuperficie(superficieTexto);
    }

    public static Specification<Cancha> activa(boolean activa) {
        return (cancha, consulta, cb) -> cb.equal(cancha.get("activa"), activa);
    }

    /** El patrón ya viene sin tildes, en minúsculas y con los comodines escapados con '!' (ver Textos.patronContiene). */
    public static Specification<Cancha> identificadorContiene(String patron) {
        return (cancha, consulta, cb) -> cb.like(
                cb.lower(cb.function("translate", String.class, cancha.get("identificador"),
                        cb.literal(Textos.LETRAS_CON_TILDE), cb.literal(Textos.LETRAS_SIN_TILDE))),
                patron, '!');
    }

    /** Tiene ahora mismo un bloqueo por mantenimiento que cubre esta fecha y hora. */
    public static Specification<Cancha> enMantenimientoAhora(LocalDate fecha, LocalTime hora) {
        return (cancha, consulta, cb) -> {
            Subquery<Long> bloqueoVigente = consulta.subquery(Long.class);
            var disponibilidad = bloqueoVigente.from(Disponibilidad.class);
            bloqueoVigente.select(disponibilidad.get("id"))
                    .where(
                            cb.equal(disponibilidad.get("cancha"), cancha),
                            cb.equal(disponibilidad.get("estado"), EstadoDisponibilidad.MANTENIMIENTO),
                            cb.equal(disponibilidad.get("fecha"), fecha),
                            cb.lessThanOrEqualTo(disponibilidad.get("horaInicio"), hora),
                            cb.greaterThan(disponibilidad.get("horaFin"), hora));
            return cb.exists(bloqueoVigente);
        };
    }
}
