package com.tenisahora.modules.reserva.entity;

import com.tenisahora.modules.socio.entity.Socio;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reserva_jugador")
@Getter
@Setter
@NoArgsConstructor
public class ReservaJugador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reserva_id", nullable = false)
    private Reserva reserva;

    /** Vacío cuando el jugador es un invitado sin cuenta: en ese caso van sus datos abajo. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "socio_id")
    private Socio socio;

    @Column(name = "nombre_invitado", length = 100)
    private String nombre;

    @Column(name = "apellido_invitado", length = 100)
    private String apellido;

    @Column(name = "documento_invitado", length = 30)
    private String documento;

    @Column(name = "es_titular", nullable = false)
    private Boolean esTitular = true;
}
