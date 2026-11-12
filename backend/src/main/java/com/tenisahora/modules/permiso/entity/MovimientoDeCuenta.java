package com.tenisahora.modules.permiso.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Qué se le hizo a una cuenta, quién lo hizo y cuándo. RF74 lo pide explícitamente para el cambio de rol, y RF76
 * exige un motivo al deshabilitar: las dos cosas se guardan acá.
 *
 * <p>Se guarda el id de la persona y no una relación: la persona puede estar en cualquiera de las cinco tablas de
 * rol, y además el id cambia cuando cambia de rol. Por eso el movimiento se queda con el nombre y el detalle
 * escritos en el momento, que es lo que hace falta para leer el historial después.
 */
@Entity
@Table(name = "movimiento_de_cuenta", indexes = @Index(name = "idx_movimiento_persona", columnList = "persona_id"))
@Getter
@Setter
@NoArgsConstructor
public class MovimientoDeCuenta {

    public enum Tipo {
        CAMBIO_DE_ROL,
        DESHABILITACION,
        HABILITACION
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "persona_id", nullable = false)
    private Long personaId;

    /** El id que tenía antes del cambio de rol, para poder seguir el rastro cuando el traspaso le cambia el id. */
    @Column(name = "persona_id_anterior")
    private Long personaIdAnterior;

    @Column(name = "persona_nombre", nullable = false, length = 200)
    private String personaNombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Tipo tipo;

    @Column(nullable = false, length = 300)
    private String detalle;

    @Column(length = 300)
    private String motivo;

    @Column(name = "hecho_por_id", nullable = false)
    private Long hechoPorId;

    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();
}
