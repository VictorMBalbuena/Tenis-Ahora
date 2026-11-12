package com.tenisahora.modules.descuento.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "descuento")
@Getter
@Setter
@NoArgsConstructor
public class Descuento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Lo que el socio escribe al reservar para que se le aplique el descuento, por ejemplo "CYBERWEEK".
     *
     * Se guarda siempre en mayúsculas y sin espacios alrededor, así "cyberweek" y "CyberWeek" son el mismo
     * código. Es único: es la forma de identificar un descuento desde afuera.
     */
    @Column(nullable = false, unique = true, length = 30)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoDescuento tipo;

    @Column(precision = 5, scale = 2)
    private BigDecimal porcentaje;

    @Column(name = "monto_fijo", precision = 10, scale = 2)
    private BigDecimal montoFijo;

    private String condiciones;

    @Column(nullable = false)
    private Boolean activo = true;
}
