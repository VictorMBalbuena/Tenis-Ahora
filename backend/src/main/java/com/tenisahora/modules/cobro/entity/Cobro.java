package com.tenisahora.modules.cobro.entity;

import com.tenisahora.modules.reserva.entity.Reserva;
import com.tenisahora.modules.actividad.entity.InscripcionClase;
import com.tenisahora.modules.torneo.entity.Inscripcion;
import com.tenisahora.modules.torneo.entity.Torneo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cobro",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_cobro_codigo", columnNames = "codigo"),
                @UniqueConstraint(name = "uk_cobro_id_pago_externo", columnNames = "id_pago_externo")
        },
        indexes = {
                @Index(name = "ix_cobro_reserva", columnList = "reserva_id"),
                @Index(name = "ix_cobro_estado_expiracion", columnList = "estado, fecha_expiracion_qr")
        })
@Getter
@Setter
@NoArgsConstructor
public class Cobro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Número con el que se identifica el cobro ante la persona: C-000123. Lo asigna el servicio al registrarlo. */
    @Column(length = 20)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserva_id")
    private Reserva reserva;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "torneo_id")
    private Torneo torneo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inscripcion_clase_id")
    private InscripcionClase inscripcionActividad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inscripcion_torneo_id")
    private Inscripcion inscripcionTorneo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConceptoCobro concepto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCobro estado = EstadoCobro.PENDIENTE;

    @Enumerated(EnumType.STRING)
    @Column(name = "medio_pago", nullable = false, length = 20)
    private MedioPago medioPago;

    /** Cantidad de cuotas, solo para crédito (CU87). */
    private Integer cuotas;

    /** Monto antes de los descuentos (RD21): queda registrado para poder explicar la diferencia con el final. */
    @Column(name = "monto_base", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoBase;

    @Column(name = "monto_final", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoFinal;

    /** Hasta cuándo sirve el QR pedido a Mercado Pago (10 minutos). Nulo si el cobro no es con QR. */
    @Column(name = "fecha_expiracion_qr")
    private LocalDateTime fechaExpiracionQr;

    /** Número que devuelve la terminal del club con débito o crédito (CU86, CU87). No se guarda ningún dato de la tarjeta. */
    @Column(name = "numero_operacion", length = 60)
    private String numeroOperacion;

    /**
     * Identificador del pago en Mercado Pago. Es único: si el mismo aviso llega repetido, la base rechaza el
     * segundo registro y el pago no se cuenta dos veces (CU78 A2).
     */
    @Column(name = "id_pago_externo", length = 60)
    private String idPagoExterno;

    /** Quién lo cobró. Es solo el número: este módulo no conoce a las personas. Nulo si lo pagó el socio online (CU77). */
    @Column(name = "registrado_por_id")
    private Long registradoPorId;

    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    @Column(name = "motivo_anulacion")
    private String motivoAnulacion;

    @Column(name = "fecha_anulacion")
    private LocalDateTime fechaAnulacion;

    @Column(name = "anulado_por_id")
    private Long anuladoPorId;
}
