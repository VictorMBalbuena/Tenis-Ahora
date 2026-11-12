package com.tenisahora.modules.torneo.entity;

import com.tenisahora.modules.cancha.entity.Cancha;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "torneo")
@Getter
@Setter
@NoArgsConstructor
public class Torneo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancha_id")
    private Cancha cancha;

    /**
     * Torneo (eliminación directa) o liga (todos contra todos). Arranca en TORNEO para que las competencias que
     * ya estaban cargadas sigan comportándose igual que antes de que existiera este campo.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoCompetencia tipo = TipoCompetencia.TORNEO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategoriaTorneo categoria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Modalidad modalidad;

    @Column(name = "tipo_superficie", nullable = false, length = 100)
    private String tipoSuperficie;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoTorneo estado = EstadoTorneo.PLANIFICADO;

    @Column(name = "costo_inscripcion", precision = 10, scale = 2)
    private BigDecimal costoInscripcion;

    /** RF de organización de torneos: los reglamentos internos que aplican, además de las reglas de la Asociación de Tenis (RD14). */
    @Column(length = 2000)
    private String reglamento;
}
