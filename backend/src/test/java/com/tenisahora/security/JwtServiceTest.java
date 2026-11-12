package com.tenisahora.security;

import com.tenisahora.shared.model.Rol;
import com.tenisahora.modules.socio.entity.Socio;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MissingClaimException;
import io.jsonwebtoken.IncorrectClaimException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRETO = "clave-de-test-con-largo-suficiente-para-hs384-0123456789";

    private JwtService jwtService;
    private Socio persona;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRETO, 30);
        persona = new Socio();
        persona.setId(7L);
        persona.setEmail("socio@tenisahora.com");
    }

    @Test
    void generarToken_incluyeIdYRol() {
        Claims claims = jwtService.parseClaims(jwtService.generarToken(persona.getId(), persona.getRol().name()));

        assertThat(jwtService.extraerId(claims)).isEqualTo(7L);
        assertThat(jwtService.extraerRol(claims)).isEqualTo("SOCIO");
    }

    @Test
    void generarToken_noIncluyeElEmail() {
        Claims claims = jwtService.parseClaims(jwtService.generarToken(persona.getId(), persona.getRol().name()));

        assertThat(claims.values()).doesNotContain("socio@tenisahora.com");
    }

    @Test
    void generarToken_venceALos30Minutos() {
        Claims claims = jwtService.parseClaims(jwtService.generarToken(persona.getId(), persona.getRol().name()));

        long duracionSegundos = (claims.getExpiration().getTime() - claims.getIssuedAt().getTime()) / 1000;
        assertThat(duracionSegundos).isEqualTo(30 * 60);
    }

    @Test
    void parseClaims_tokenFirmadoConOtraClave_lanzaExcepcion() {
        JwtService otroServicio = new JwtService("otra-clave-distinta-con-largo-suficiente-para-hs384-xx", 30);
        String tokenAjeno = otroServicio.generarToken(persona.getId(), persona.getRol().name());

        assertThatThrownBy(() -> jwtService.parseClaims(tokenAjeno)).isInstanceOf(SignatureException.class);
    }

    @Test
    void parseClaims_tokenVencido_lanzaExcepcion() {
        JwtService servicioVencido = new JwtService(SECRETO, -1);
        String tokenVencido = servicioVencido.generarToken(persona.getId(), persona.getRol().name());

        assertThatThrownBy(() -> jwtService.parseClaims(tokenVencido)).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void parseClaims_tokenMalFormado_lanzaExcepcion() {
        assertThatThrownBy(() -> jwtService.parseClaims("esto-no-es-un-jwt")).isInstanceOf(JwtException.class);
    }

    @Test
    void parseClaims_tokenAdulterado_lanzaExcepcion() {
        String token = jwtService.generarToken(persona.getId(), persona.getRol().name());
        String[] partes = token.split("\\.");
        // Se cambia el payload por uno con rol ADMINISTRADOR, manteniendo la firma original
        String payloadAdulterado = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"7\",\"rol\":\"ADMINISTRADOR\"}".getBytes());
        String tokenAdulterado = partes[0] + "." + payloadAdulterado + "." + partes[2];

        assertThatThrownBy(() -> jwtService.parseClaims(tokenAdulterado)).isInstanceOf(SignatureException.class);
    }

    private SecretKey claveDeTest() {
        return new SecretKeySpec(SECRETO.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    private String tokenConClaims(String emisor, String audiencia) {
        return Jwts.builder()
                .issuer(emisor)
                .audience().add(audiencia).and()
                .subject("7")
                .claim("rol", "SOCIO")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(claveDeTest(), Jwts.SIG.HS256)
                .compact();
    }

    @Test
    void generarToken_incluyeEmisorAudienciaYIdentificadorUnico() {
        Claims claims = jwtService.parseClaims(jwtService.generarToken(persona.getId(), persona.getRol().name()));

        assertThat(claims.getIssuer()).isEqualTo("tenis-ahora");
        assertThat(claims.getAudience()).containsExactly("tenis-ahora-api");
        assertThat(claims.getId()).isNotBlank();
    }

    @Test
    void generarToken_cadaTokenTieneUnIdentificadorDistinto() {
        String primero = jwtService.parseClaims(jwtService.generarToken(persona.getId(), persona.getRol().name())).getId();
        String segundo = jwtService.parseClaims(jwtService.generarToken(persona.getId(), persona.getRol().name())).getId();

        assertThat(primero).isNotEqualTo(segundo);
    }

    @Test
    void generarToken_sefirmaConHS256() {
        String token = jwtService.generarToken(persona.getId(), persona.getRol().name());

        String cabecera = new String(java.util.Base64.getUrlDecoder().decode(token.split("[.]")[0]));
        assertThat(cabecera).contains("\"alg\":\"HS256\"");
    }

    @Test
    void parseClaims_emisorIncorrecto_lanzaExcepcion() {
        String token = tokenConClaims("otro-sistema", "tenis-ahora-api");

        assertThatThrownBy(() -> jwtService.parseClaims(token)).isInstanceOf(IncorrectClaimException.class);
    }

    @Test
    void parseClaims_audienciaIncorrecta_lanzaExcepcion() {
        String token = tokenConClaims("tenis-ahora", "otra-api");

        assertThatThrownBy(() -> jwtService.parseClaims(token)).isInstanceOf(IncorrectClaimException.class);
    }

    @Test
    void parseClaims_sinEmisor_lanzaExcepcion() {
        String token = Jwts.builder()
                .audience().add("tenis-ahora-api").and()
                .subject("7")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(claveDeTest(), Jwts.SIG.HS256)
                .compact();

        assertThatThrownBy(() -> jwtService.parseClaims(token)).isInstanceOf(MissingClaimException.class);
    }

    @Test
    void parseClaims_tokenSinFirma_lanzaExcepcion() {
        String sinFirma = Jwts.builder()
                .issuer("tenis-ahora")
                .audience().add("tenis-ahora-api").and()
                .subject("7")
                .claim("rol", "ADMINISTRADOR")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .compact();

        assertThatThrownBy(() -> jwtService.parseClaims(sinFirma)).isInstanceOf(JwtException.class);
    }

    @Test
    void parseClaims_firmadoConOtroAlgoritmoHmac_lanzaExcepcion() {
        String tokenHs512 = Jwts.builder()
                .issuer("tenis-ahora")
                .audience().add("tenis-ahora-api").and()
                .subject("7")
                .claim("rol", "SOCIO")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(new SecretKeySpec(
                        "una-clave-distinta-de-al-menos-64-bytes-para-poder-usar-hs512-0123456789".getBytes(StandardCharsets.UTF_8),
                        "HmacSHA512"), Jwts.SIG.HS512)
                .compact();

        assertThatThrownBy(() -> jwtService.parseClaims(tokenHs512)).isInstanceOf(JwtException.class);
    }

    @Test
    void constructor_secretoCorto_lanzaExcepcionConMensajeClaro() {
        assertThatThrownBy(() -> new JwtService("corto", 30))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("al menos 32 bytes");
    }
}
