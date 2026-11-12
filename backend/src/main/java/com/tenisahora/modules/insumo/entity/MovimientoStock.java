package com.tenisahora.modules.insumo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Rastro de cada cambio de stock: qué pasó, cuánto, cómo quedó, por qué y quién lo hizo. */
@Entity
@Table(name = "movimiento_stock", indexes = @Index(name = "ix_movimiento_stock_insumo", columnList = "insumo_id"))
@Getter
@Setter
@NoArgsConstructor
public class MovimientoStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "insumo_id", nullable = false)
    private Insumo insumo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMovimiento tipo;

    /** Con signo: negativa si el stock bajó, positiva si subió. */
    @Column(nullable = false)
    private Integer variacion;

    @Column(name = "stock_resultante", nullable = false)
    private Integer stockResultante;

    private String motivo;

    @Column(nullable = false)
    private LocalDateTime fecha;

    /** Id de quien lo hizo; vacío cuando lo hace el sistema. Es solo el número: este módulo no conoce a las personas. */
    @Column(name = "persona_id")
    private Long personaId;
}
