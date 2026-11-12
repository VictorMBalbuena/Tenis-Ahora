package com.tenisahora.modules.instructor.entity;

import com.tenisahora.shared.entity.EmpleadoBase;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Lo que tienen en común el profesor y el entrenador. Sin tabla propia: cada uno guarda estos datos en la suya. */
@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
public abstract class Instructor extends EmpleadoBase {

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_validacion", nullable = false, length = 20)
    private EstadoValidacion estadoValidacion = EstadoValidacion.PENDIENTE;

    @Column(name = "fecha_validacion")
    private LocalDate fechaValidacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "certificacion_id")
    private Certificacion certificacion;

    /** Id del empleado que validó el título. Long suelto: instructor no puede importar el módulo empleado. */
    @Column(name = "validado_por_id")
    private Long validadoPorId;

    @Column(name = "valor_por_hora", precision = 10, scale = 2)
    private BigDecimal valorPorHora;

    @Column(length = 30)
    private String especialidad;

    /** Lo que deja escrito el administrador al rechazar la certificación (A1 de CU31). */
    @Column(name = "motivo_rechazo", length = 200)
    private String motivoRechazo;

    public boolean estaValidado() {
        return estadoValidacion == EstadoValidacion.VALIDADO;
    }
}
