package com.tenisahora.modules.auth.repository;

import com.tenisahora.modules.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /** Busca por el hash del token: el valor original nunca se guarda en la base. */
    Optional<RefreshToken> findByToken(String tokenHash);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revocado = true WHERE rt.token = :token AND rt.revocado = false")
    int revocarSiVigente(String token);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revocado = true WHERE rt.personaId = :personaId AND rt.revocado = false")
    int revocarTodosDeLaPersona(Long personaId);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.personaId = :personaId")
    int borrarTodosDeLaPersona(Long personaId);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.revocado = true OR rt.fechaExpiracion < :ahora")
    int purgarVencidosORevocados(LocalDateTime ahora);
}
