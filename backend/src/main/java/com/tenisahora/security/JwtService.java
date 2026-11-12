package com.tenisahora.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.UnsupportedJwtException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    static final String EMISOR = "tenis-ahora";
    static final String AUDIENCIA = "tenis-ahora-api";

    private static final String CLAIM_ROL = "rol";
    private static final int LARGO_MINIMO_SECRETO_BYTES = 32;
    private static final long TOLERANCIA_RELOJ_SEGUNDOS = 30;

    private final SecretKey clave;
    private final long expiracionMinutos;

    public JwtService(@Value("${app.jwt.secret}") String secreto,
                       @Value("${app.jwt.expiration-minutes}") long expiracionMinutos) {
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < LARGO_MINIMO_SECRETO_BYTES) {
            throw new IllegalStateException(
                    "app.jwt.secret debe tener al menos " + LARGO_MINIMO_SECRETO_BYTES + " bytes (256 bits)");
        }
        this.clave = new SecretKeySpec(bytes, "HmacSHA256");
        this.expiracionMinutos = expiracionMinutos;
    }

    public String generarToken(Long personaId, String rol) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(EMISOR)
                .audience().add(AUDIENCIA).and()
                .subject(String.valueOf(personaId))
                .claim(CLAIM_ROL, rol)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plusSeconds(expiracionMinutos * 60)))
                .signWith(clave, Jwts.SIG.HS256)
                .compact();
    }

    public Claims parseClaims(String token) {
        Jws<Claims> jws = Jwts.parser()
                .verifyWith(clave)
                .requireIssuer(EMISOR)
                .requireAudience(AUDIENCIA)
                .clockSkewSeconds(TOLERANCIA_RELOJ_SEGUNDOS)
                .build()
                .parseSignedClaims(token);
        // Solo se acepta la firma con la que se emite: la clave sirve para otros HMAC, pero no se admiten
        if (!Jwts.SIG.HS256.getId().equals(jws.getHeader().getAlgorithm())) {
            throw new UnsupportedJwtException("Algoritmo de firma no permitido");
        }
        return jws.getPayload();
    }

    public Long extraerId(Claims claims) {
        return Long.valueOf(claims.getSubject());
    }

    public String extraerRol(Claims claims) {
        return claims.get(CLAIM_ROL, String.class);
    }
}
