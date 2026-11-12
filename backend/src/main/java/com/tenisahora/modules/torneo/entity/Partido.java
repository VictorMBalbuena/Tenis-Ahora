package com.tenisahora.modules.torneo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "partido")
@Getter
@Setter
@NoArgsConstructor
public class Partido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "torneo_id", nullable = false)
    private Torneo torneo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participante_a_id")
    private Inscripcion participanteA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participante_b_id")
    private Inscripcion participanteB;

    @Column(nullable = false)
    private Integer ronda;

    @Column(nullable = false)
    private Integer orden;

    private String sets;

    private String resultado;

    @Column
    private LocalDate fecha;
}
