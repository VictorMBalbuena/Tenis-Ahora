package com.tenisahora.modules.cobro.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Aviso de Mercado Pago con firma inválida o ausente: no se procesa, pero queda para revisarlo (CU78 A1). */
@Entity
@Table(name = "aviso_pago_descartado")
@Getter
@Setter
@NoArgsConstructor
public class AvisoDePagoDescartado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_pago", length = 60)
    private String idPago;

    @Column(name = "id_pedido", length = 100)
    private String idPedido;

    @Column(name = "fecha_recepcion", nullable = false)
    private LocalDateTime fechaRecepcion;
}
