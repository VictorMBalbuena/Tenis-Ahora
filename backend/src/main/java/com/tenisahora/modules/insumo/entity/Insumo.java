package com.tenisahora.modules.insumo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "insumo", uniqueConstraints = @UniqueConstraint(name = "uk_insumo_nombre", columnNames = "nombre"))
@Getter
@Setter
@NoArgsConstructor
public class Insumo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private CategoriaInsumo categoria;

    @Column(name = "cantidad_disponible", nullable = false)
    private Integer cantidadDisponible = 0;

    @Column(name = "cantidad_minima", nullable = false)
    private Integer cantidadMinima = 0;

    /** false cuando se dio de baja (CU101): no aparece en listados ni en reservas nuevas, pero conserva su historial. */
    @Column(nullable = false, columnDefinition = "boolean default true")
    private Boolean activo = true;
}
