package com.tenisahora.modules.instructor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** El título o certificación habilitante de un profesor o entrenador (RD02, RD03). */
@Entity
@Table(name = "certificacion", uniqueConstraints = @UniqueConstraint(name = "uk_certificacion_numero", columnNames = "numero_certificacion"))
@Getter
@Setter
@NoArgsConstructor
public class Certificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_certificacion", nullable = false, length = 50)
    private String numeroCertificacion;

    @Column(name = "entidad_emisora", nullable = false, length = 120)
    private String entidadEmisora;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDate fechaEmision;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Column(name = "archivo_titulo_url", length = 300)
    private String archivoTituloUrl;

    /** RD02 y RD03: el título o la certificación valen solo mientras no estén vencidos. */
    public boolean estaVigente(LocalDate hoy) {
        return fechaVencimiento == null || !fechaVencimiento.isBefore(hoy);
    }
}
