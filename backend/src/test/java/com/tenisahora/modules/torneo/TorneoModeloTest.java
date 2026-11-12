package com.tenisahora.modules.torneo;

import com.tenisahora.modules.torneo.entity.Partido;
import com.tenisahora.modules.torneo.entity.Torneo;
import com.tenisahora.modules.torneo.repository.InscripcionRepository;
import com.tenisahora.modules.torneo.repository.PartidoRepository;
import com.tenisahora.shared.exception.CodigoError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class TorneoModeloTest {

    @Test
    @DisplayName("Torneo tiene campo nombre opcional y accesores")
    void torneoTieneCampoNombre() {
        Torneo torneo = new Torneo();
        torneo.setNombre("Torneo Primavera");
        assertThat(torneo.getNombre()).isEqualTo("Torneo Primavera");
    }

    @Test
    @DisplayName("Partido soporta ronda, orden y participantes nulos")
    void partidoSoportaRondaOrdenYNulos() {
        Partido partido = new Partido();
        partido.setRonda(1);
        partido.setOrden(2);
        partido.setFecha(LocalDate.of(2026, 11, 10));
        partido.setParticipanteA(null);
        partido.setParticipanteB(null);

        assertThat(partido.getRonda()).isEqualTo(1);
        assertThat(partido.getOrden()).isEqualTo(2);
        assertThat(partido.getFecha()).isEqualTo(LocalDate.of(2026, 11, 10));
        assertThat(partido.getParticipanteA()).isNull();
        assertThat(partido.getParticipanteB()).isNull();
    }

    @Test
    @DisplayName("CodigoError contiene códigos requeridos para el módulo de torneos")
    void codigosErrorTorneoExisten() {
        assertThat(CodigoError.valueOf("TORNEO_NO_ENCONTRADO")).isNotNull();
        assertThat(CodigoError.valueOf("TORNEO_FECHAS_INVALIDAS")).isNotNull();
        assertThat(CodigoError.valueOf("TORNEO_ESTADO_INVALIDO")).isNotNull();
        assertThat(CodigoError.valueOf("INSCRIPCION_DUPLICADA")).isNotNull();
        assertThat(CodigoError.valueOf("MODALIDAD_INVALIDA")).isNotNull();
        assertThat(CodigoError.valueOf("CUPOS_INSUFICIENTES")).isNotNull();
        assertThat(CodigoError.valueOf("FIXTURE_YA_GENERADO")).isNotNull();
        assertThat(CodigoError.valueOf("FIXTURE_NO_DISPONIBLE")).isNotNull();
        assertThat(CodigoError.valueOf("PARTIDO_NO_ENCONTRADO")).isNotNull();
        assertThat(CodigoError.valueOf("RESULTADO_INVALIDO")).isNotNull();
        assertThat(CodigoError.valueOf("PARTIDO_NO_DISPUTABLE")).isNotNull();
    }

    @Test
    @DisplayName("Repositorios exponen métodos de consulta necesarios")
    void repositoriosExponenMetodosNecesarios() throws NoSuchMethodException {
        Method findByTorneoId = InscripcionRepository.class.getMethod("findByTorneoId", Long.class);
        assertThat(findByTorneoId).isNotNull();

        Method existsSocio = InscripcionRepository.class.getMethod("existsSocioEnTorneo", Long.class, Long.class);
        assertThat(existsSocio).isNotNull();

        Method findPartidos = PartidoRepository.class.getMethod("findByTorneoIdOrderByRondaAscOrdenAsc", Long.class);
        assertThat(findPartidos).isNotNull();

        Method findPartido = PartidoRepository.class.getMethod("findByTorneoIdAndRondaAndOrden", Long.class, Integer.class, Integer.class);
        assertThat(findPartido).isNotNull();
    }
}
