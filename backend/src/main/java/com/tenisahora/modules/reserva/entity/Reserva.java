package com.tenisahora.modules.reserva.entity;

import com.tenisahora.modules.cancha.entity.Cancha;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "reserva")
@Getter
@Setter
@NoArgsConstructor
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cancha_id", nullable = false)
    private Cancha cancha;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoReserva estado = EstadoReserva.PENDIENTE_PAGO;

    @Column(name = "cantidad_jugadores", nullable = false)
    private Integer cantidadJugadores;

    @Column(name = "monto_total", precision = 10, scale = 2)
    private BigDecimal montoTotal;

    /**
     * El código de descuento que se usó, si hubo alguno. Se guarda el texto y no una referencia al descuento a
     * propósito: deja a reserva sin depender del módulo de descuentos, y el comprobante sigue diciendo qué
     * código se aplicó aunque después ese descuento se dé de baja.
     */
    @Column(name = "codigo_descuento", length = 30)
    private String codigoDescuento;

    /** Cuánto se descontó del total por ese código. El montoTotal ya viene con el descuento restado. */
    @Column(name = "monto_descuento", precision = 10, scale = 2)
    private BigDecimal montoDescuento;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    /** Desde cuándo corre otra vez el plazo para pagar la seña: se completa si se anula la seña de una reserva confirmada. */
    @Column(name = "plazo_pago_desde")
    private LocalDateTime plazoPagoDesde;

    /** Cuándo se canceló; vacío si no está cancelada. */
    @Column(name = "fecha_cancelacion")
    private LocalDateTime fechaCancelacion;

    /** Cargo por cancelar fuera de plazo (RD12). El cobro real lo hace BACK 08. */
    @Column(name = "monto_penalidad", precision = 10, scale = 2)
    private BigDecimal montoPenalidad;

    /** Lo que corresponde devolver al cancelar en plazo (RD13). El reembolso real lo hace BACK 08. */
    @Column(name = "monto_reembolso", precision = 10, scale = 2)
    private BigDecimal montoReembolso;

    /** Cuándo se registró el inicio del alquiler (CU14). */
    @Column(name = "inicio_real")
    private LocalDateTime inicioReal;

    /** Cuándo se registró la finalización del alquiler (CU15), a mano o automática. */
    @Column(name = "fin_real")
    private LocalDateTime finReal;

    public LocalDateTime inicioDelPlazoDePago() {
        return plazoPagoDesde != null ? plazoPagoDesde : fechaCreacion;
    }
}
