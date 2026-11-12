package com.tenisahora.modules.actividad.entity;

import com.tenisahora.modules.socio.entity.Socio;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * La inscripción de un socio a una clase. No se borra al cancelar: se pone activa=false, así se conserva el
 * historial y una reinscripción reactiva la fila en vez de insertar otra.
 */
@Entity
@Table(name = "inscripcion_clase", uniqueConstraints = @UniqueConstraint(name = "uk_inscripcion_clase", columnNames = {"actividad_id", "socio_id"}))
@Getter
@Setter
@NoArgsConstructor
public class InscripcionClase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actividad_id", nullable = false)
    private Actividad actividad;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "socio_id", nullable = false)
    private Socio socio;

    @Column(name = "fecha_inscripcion", nullable = false)
    private LocalDate fechaInscripcion = LocalDate.now();

    @Column(nullable = false)
    private Boolean activa = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "varchar(20) default 'CONFIRMADA'")
    private EstadoInscripcion estado = EstadoInscripcion.CONFIRMADA;

    /** Desde cuándo corre el plazo para pagar la seña; también separa los cobros de esta inscripción de los de una anterior. */
    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_cancelacion")
    private LocalDateTime fechaCancelacion;

    /** El valor de la actividad al inscribirse: si después cambia, esta inscripción conserva el suyo. */
    @Column(name = "monto_total", precision = 10, scale = 2)
    private BigDecimal montoTotal;

    @Column(name = "monto_penalidad", precision = 10, scale = 2)
    private BigDecimal montoPenalidad;

    @Column(name = "monto_reembolso", precision = 10, scale = 2)
    private BigDecimal montoReembolso;
}
