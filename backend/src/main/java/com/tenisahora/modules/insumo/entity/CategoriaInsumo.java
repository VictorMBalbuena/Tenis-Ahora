package com.tenisahora.modules.insumo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/**
 * Categoría de un insumo (ej. Raqueta, Pelota, Red). A diferencia del antiguo
 * enum TipoInsumo, es una tabla editable: el club puede sumar categorías
 * nuevas sin tocar código.
 */
@Entity
@Table(name = "categoria_insumo", uniqueConstraints = @UniqueConstraint(name = "uk_categoria_insumo_nombre", columnNames = "nombre"))
@Getter
@Setter
@NoArgsConstructor
public class CategoriaInsumo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String nombre;

    public CategoriaInsumo(String nombre) {
        this.nombre = nombre;
    }
}
