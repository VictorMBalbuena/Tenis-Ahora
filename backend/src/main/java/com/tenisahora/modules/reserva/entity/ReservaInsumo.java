package com.tenisahora.modules.reserva.entity;

import com.tenisahora.modules.insumo.entity.Insumo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reserva_insumo")
@Getter
@Setter
@NoArgsConstructor
public class ReservaInsumo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reserva_id", nullable = false)
    private Reserva reserva;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "insumo_id", nullable = false)
    private Insumo insumo;

    @Column(name = "cantidad_asignada", nullable = false)
    private Integer cantidadAsignada;

    @Column(nullable = false)
    private Boolean repuesto = false;

    /** true desde que se descontó del stock, al iniciar el alquiler (CU14). Asignado sin descontar: false. */
    @Column(nullable = false, columnDefinition = "boolean default false")
    private Boolean descontado = false;
}
