package com.tenisahora.modules.descuento.entity;

import com.tenisahora.modules.cobro.entity.Cobro;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cobro_descuento")
@Getter
@Setter
@NoArgsConstructor
public class CobroDescuento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cobro_id", nullable = false)
    private Cobro cobro;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "descuento_id", nullable = false)
    private Descuento descuento;
}
