package com.tenisahora.modules.cobro.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "recibo")
@Getter
@Setter
@NoArgsConstructor
public class Recibo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Un cobro tiene un solo recibo: la columna es única, así que no se puede emitir dos veces (CU57). */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cobro_id", nullable = false, unique = true)
    private Cobro cobro;

    @Column(name = "numero_recibo", nullable = false, unique = true)
    private String numeroRecibo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoRecibo estado = EstadoRecibo.EMITIDO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    private String concepto;

    /** Copia del medio y del número de operación del cobro: el recibo es el comprobante y no cambia si el cobro se corrige. */
    @Enumerated(EnumType.STRING)
    @Column(name = "medio_pago", nullable = false, length = 20)
    private MedioPago medioPago;

    @Column(name = "numero_operacion", length = 60)
    private String numeroOperacion;

    @Column(columnDefinition = "text")
    private String observaciones;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDateTime fechaEmision = LocalDateTime.now();
}
