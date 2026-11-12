package com.tenisahora.shared.persona;

import com.tenisahora.shared.dto.PersonaResponse;
import com.tenisahora.shared.dto.PersonaResumenResponse;
import com.tenisahora.shared.model.Rol;
import com.tenisahora.modules.socio.entity.Socio;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.time.LocalDate;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class MapeoDePersonaTest {

    private final MapeoDePersona mapper = new MapeoDePersona();

    private Socio socio() {
        Socio socio = new Socio();
        socio.setId(12L);
        socio.setNombre("Ana");
        socio.setApellido("Gómez");
        socio.setDni("30123456");
        socio.setFechaNacimiento(LocalDate.now().minusYears(30));
        socio.setCalle("Av. Siempreviva");
        socio.setNumeroCalle("742");
        socio.setCodigoArea("11");
        socio.setNumeroTelefono("12345678");
        socio.setEmail("ana@correo.com");
        socio.setPasswordHash("$2a$10$hash-secreto");
        socio.setActivo(true);
        socio.setFechaAlta(LocalDate.of(2026, 10, 10));
        socio.setNumeroSocio("S-0012");
        return socio;
    }

    @Test
    void aRespuesta_copiaLosDatosYCalculaLaEdad() {
        PersonaResponse respuesta = mapper.aRespuesta(socio());

        assertThat(respuesta.id()).isEqualTo(12L);
        assertThat(respuesta.nombre()).isEqualTo("Ana");
        assertThat(respuesta.dni()).isEqualTo("30123456");
        assertThat(respuesta.calle()).isEqualTo("Av. Siempreviva");
        assertThat(respuesta.numeroCalle()).isEqualTo("742");
        assertThat(respuesta.codigoArea()).isEqualTo("11");
        assertThat(respuesta.numeroTelefono()).isEqualTo("12345678");
        assertThat(respuesta.edad()).isEqualTo(30);
        assertThat(respuesta.rol()).isEqualTo(Rol.SOCIO);
    }

    @Test
    void aResumen_soloTraeLosDatosDelListado() {
        PersonaResumenResponse resumen = mapper.aResumen(socio());

        assertThat(resumen.id()).isEqualTo(12L);
        assertThat(resumen.email()).isEqualTo("ana@correo.com");
        assertThat(resumen.activo()).isTrue();
    }

    @Test
    void lasRespuestas_noTienenCamposDeContrasenia() {
        for (Class<?> tipo : new Class<?>[]{PersonaResponse.class, PersonaResumenResponse.class}) {
            assertThat(Arrays.stream(tipo.getRecordComponents()).map(RecordComponent::getName))
                    .noneMatch(nombre -> nombre.toLowerCase().contains("password")
                            || nombre.toLowerCase().contains("hash")
                            || nombre.toLowerCase().contains("contrasenia"));
        }
    }
}
