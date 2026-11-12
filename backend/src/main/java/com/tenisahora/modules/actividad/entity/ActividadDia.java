package com.tenisahora.modules.actividad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;

/** Un día de la semana en que se dicta la actividad. Se navega desde acá con consultas, sin @OneToMany en Actividad. */
@Entity
@Table(name = "actividad_dia", uniqueConstraints = @UniqueConstraint(name = "uk_actividad_dia", columnNames = {"actividad_id", "dia_semana"}))
@Getter
@Setter
@NoArgsConstructor
public class ActividadDia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actividad_id", nullable = false)
    private Actividad actividad;

    @Enumerated(EnumType.STRING)
    @Column(name = "dia_semana", nullable = false, length = 10)
    private DayOfWeek diaSemana;

    public ActividadDia(Actividad actividad, DayOfWeek diaSemana) {
        this.actividad = actividad;
        this.diaSemana = diaSemana;
    }
}
