package com.tenisahora.modules.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_token")
@Getter
@Setter
@NoArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Solo el número: auth no conoce ninguna entidad de rol. El id es único entre todas las tablas de rol. */
    @Column(name = "persona_id", nullable = false)
    private Long personaId;

    @Column(nullable = false, length = 20)
    private String rol;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    @Column(nullable = false)
    private Boolean revocado = false;
}
