package com.tenisahora.modules.torneo.entity;

import com.tenisahora.modules.socio.entity.Socio;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "inscripcion")
@Getter
@Setter
@NoArgsConstructor
public class Inscripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "torneo_id", nullable = false)
    private Torneo torneo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "jugador1_id", nullable = false)
    private Socio jugador1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pareja_id")
    private Socio pareja;

    @Column(nullable = false)
    private Integer equipo;

    @Column(name = "nombre_equipo")
    private String nombreEquipo;

    @Column(name = "fecha_inscripcion", nullable = false)
    private LocalDate fechaInscripcion = LocalDate.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "varchar(20) default 'CONFIRMADA'")
    private EstadoInscripcionTorneo estado = EstadoInscripcionTorneo.CONFIRMADA;

    /** Desde cuándo corre el plazo para pagar el arancel. */
    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    /** El arancel al inscribirse: si el torneo lo cambia, esta inscripción conserva el suyo. */
    @Column(name = "monto_total", precision = 10, scale = 2)
    private BigDecimal montoTotal;

    public boolean participa(Long socioId) {
        return jugador1.getId().equals(socioId) || (pareja != null && pareja.getId().equals(socioId));
    }
}
