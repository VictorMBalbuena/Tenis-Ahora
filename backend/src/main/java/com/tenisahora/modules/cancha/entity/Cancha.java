package com.tenisahora.modules.cancha.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "cancha", uniqueConstraints = @UniqueConstraint(name = "uk_cancha_identificador", columnNames = "identificador"))
@Getter
@Setter
@NoArgsConstructor
public class Cancha {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String identificador;

    @Column(name = "tipo_superficie", length = 100)
    private String tipoSuperficie;

    private Integer capacidad;

    public Integer getCapacidad() {
        if (tipoCancha != null && tipoCancha.getCapacidad() != null) {
            return tipoCancha.getCapacidad();
        }
        return capacidad;
    }

    public String getSuperficie() {
        if (tipoCancha != null && tipoCancha.getSuperficie() != null) {
            return tipoCancha.getSuperficie();
        }
        return tipoSuperficie;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_cancha_id")
    private TipoCancha tipoCancha;

    @Column(name = "precio_por_hora", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioPorHora;

    @Column(nullable = false)
    private Boolean activa = true;

    @Column(name = "techada", nullable = false)
    private Boolean techada = false;

    @Column(name = "iluminacion", nullable = false)
    private Boolean iluminacion = true;

    @Column(name = "tribuna", nullable = false)
    private Boolean tribuna = false;

    @Column(name = "apta_torneos", nullable = false)
    private Boolean aptaTorneos = false;

    @Column(name = "observaciones", length = 500)
    private String observaciones;
}
