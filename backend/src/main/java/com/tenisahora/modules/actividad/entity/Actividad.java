package com.tenisahora.modules.actividad.entity;

import com.tenisahora.modules.cancha.entity.Cancha;
import com.tenisahora.modules.instructor.entity.Entrenador;
import com.tenisahora.modules.instructor.entity.Instructor;
import com.tenisahora.modules.instructor.entity.Profesor;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Un curso recurrente (clase o entrenamiento) que se dicta en los días de la semana de {@link ActividadDia},
 * entre fechaInicio y fechaFin. Tabla única con discriminador: así lo pide el DER.
 */
@Entity
@Table(name = "actividad", uniqueConstraints = @UniqueConstraint(name = "uk_actividad_codigo", columnNames = "codigo"))
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo_actividad", length = 15)
@Getter
@Setter
@NoArgsConstructor
public abstract class Actividad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    /** Código público, asignado por el service después del primer save (A-%06d), igual que el número de socio. */
    @Column(length = 10)
    protected String codigo;

    @Column(length = 80, nullable = false)
    protected String nombre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cancha_id", nullable = false)
    protected Cancha cancha;

    @Column(name = "fecha_inicio", nullable = false)
    protected LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    protected LocalDate fechaFin;

    @Column(name = "hora_inicio", nullable = false)
    protected LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    protected LocalTime horaFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    protected EstadoActividad estado = EstadoActividad.PROGRAMADA;

    @Column(name = "cupo_maximo", nullable = false)
    protected Integer cupoMaximo;

    @Column(name = "cupo_minimo")
    protected Integer cupoMinimo;

    @Column(name = "valor_mensual", precision = 10, scale = 2)
    protected BigDecimal valorMensual;

    @Column(length = 30)
    protected String categoria;

    /** Grupal o particular. No confundir con el discriminador tipo_actividad (clase o entrenamiento). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    protected Modalidad modalidad;

    /**
     * Los dos están en el DER a nivel actividad, nullable: una clase grupal usa profesor, un entrenamiento grupal usa
     * entrenador, y una particular (CU44) acepta cualquiera de los dos. Quién exige cuál lo valida el service, no el
     * esquema (ver {@code ValidacionDeInstructorPorModalidad}).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profesor_id")
    protected Profesor profesor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entrenador_id")
    protected Entrenador entrenador;

    public Instructor getInstructor() {
        return profesor != null ? profesor : entrenador;
    }

    public boolean tieneInstructor() {
        return profesor != null || entrenador != null;
    }
}
