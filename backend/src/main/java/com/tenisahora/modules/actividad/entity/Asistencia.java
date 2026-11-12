package com.tenisahora.modules.actividad.entity;

import com.tenisahora.modules.socio.entity.Socio;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** Una por alumno, por actividad y por fecha: así volver a registrar corrige en vez de duplicar (CU42, CU43). */
@Entity
@Table(name = "asistencia",
        uniqueConstraints = @UniqueConstraint(name = "uk_asistencia_actividad_socio_fecha", columnNames = {"actividad_id", "socio_id", "fecha"}),
        indexes = @Index(name = "ix_asistencia_socio_fecha", columnList = "socio_id, fecha"))
@Getter
@Setter
@NoArgsConstructor
public class Asistencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actividad_id", nullable = false)
    private Actividad actividad;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "socio_id", nullable = false)
    private Socio socio;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private Boolean presente = false;
}
