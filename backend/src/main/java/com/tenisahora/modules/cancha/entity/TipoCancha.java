package com.tenisahora.modules.cancha.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tipo o categoría de cancha de tenis (ej. Polvo de ladrillo, Har-Tru, Resina acrílica).
 * Define la superficie y la capacidad máxima de jugadores según el enunciado.
 */
@Entity
@Table(name = "tipo_cancha", uniqueConstraints = @UniqueConstraint(name = "uk_tipo_cancha_nombre", columnNames = "nombre"))
@Getter
@Setter
@NoArgsConstructor
public class TipoCancha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String nombre;

    @Column(name = "superficie", nullable = false, length = 100)
    private String superficie;

    @Column(nullable = false)
    private Integer capacidad;

    @Column(length = 200)
    private String descripcion;

    @Column(nullable = false)
    private Boolean activo = true;

    public TipoCancha(String nombre, String superficie, Integer capacidad, String descripcion) {
        this.nombre = nombre;
        this.superficie = superficie;
        this.capacidad = capacidad;
        this.descripcion = descripcion;
        this.activo = true;
    }

    public String getTipoSuperficie() {
        return superficie;
    }

    public void setTipoSuperficie(String tipoSuperficie) {
        this.superficie = tipoSuperficie;
    }
}

