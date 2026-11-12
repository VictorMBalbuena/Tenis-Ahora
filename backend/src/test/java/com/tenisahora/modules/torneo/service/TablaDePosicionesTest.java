package com.tenisahora.modules.torneo.service;

import com.tenisahora.modules.torneo.dto.PosicionResponse;
import com.tenisahora.modules.torneo.entity.Inscripcion;
import com.tenisahora.modules.torneo.entity.Partido;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** La tabla de posiciones de una liga, armada con los resultados ya cargados. */
class TablaDePosicionesTest {

    private Inscripcion equipo(long id, String nombre) {
        Inscripcion inscripcion = new Inscripcion();
        inscripcion.setId(id);
        inscripcion.setNombreEquipo(nombre);
        return inscripcion;
    }

    private Partido jugado(Inscripcion a, Inscripcion b, String sets) {
        Partido partido = new Partido();
        partido.setParticipanteA(a);
        partido.setParticipanteB(b);
        partido.setSets(sets);
        partido.setResultado("Gana alguien");
        return partido;
    }

    private Partido pendiente(Inscripcion a, Inscripcion b) {
        Partido partido = new Partido();
        partido.setParticipanteA(a);
        partido.setParticipanteB(b);
        return partido;
    }

    @Test
    void cuentaGanadosPerdidosSetsYGames() {
        Inscripcion ana = equipo(1L, "Ana");
        Inscripcion beto = equipo(2L, "Beto");

        List<PosicionResponse> tabla = TablaDePosiciones.calcular(
                List.of(ana, beto), List.of(jugado(ana, beto, "6-4 6-3")));

        PosicionResponse primera = tabla.get(0);
        assertThat(primera.participante()).isEqualTo("Ana");
        assertThat(primera.jugados()).isEqualTo(1);
        assertThat(primera.ganados()).isEqualTo(1);
        assertThat(primera.perdidos()).isZero();
        assertThat(primera.setsAFavor()).isEqualTo(2);
        assertThat(primera.setsEnContra()).isZero();
        assertThat(primera.gamesAFavor()).isEqualTo(12);
        assertThat(primera.gamesEnContra()).isEqualTo(7);

        PosicionResponse segunda = tabla.get(1);
        assertThat(segunda.participante()).isEqualTo("Beto");
        assertThat(segunda.ganados()).isZero();
        assertThat(segunda.perdidos()).isEqualTo(1);
        assertThat(segunda.gamesAFavor()).isEqualTo(7);
    }

    @Test
    void unPartidoDeTresSetsSumaElSetPerdidoAlGanador() {
        Inscripcion ana = equipo(1L, "Ana");
        Inscripcion beto = equipo(2L, "Beto");

        List<PosicionResponse> tabla = TablaDePosiciones.calcular(
                List.of(ana, beto), List.of(jugado(ana, beto, "6-4 3-6 7-5")));

        assertThat(tabla.get(0).participante()).isEqualTo("Ana");
        assertThat(tabla.get(0).setsAFavor()).isEqualTo(2);
        assertThat(tabla.get(0).setsEnContra()).isEqualTo(1);
        assertThat(tabla.get(0).diferenciaDeSets()).isEqualTo(1);
    }

    @Test
    void ordenaPorPartidosGanados() {
        Inscripcion ana = equipo(1L, "Ana");
        Inscripcion beto = equipo(2L, "Beto");
        Inscripcion cata = equipo(3L, "Cata");

        List<PosicionResponse> tabla = TablaDePosiciones.calcular(
                List.of(ana, beto, cata),
                List.of(jugado(ana, beto, "6-0 6-0"),
                        jugado(ana, cata, "6-0 6-0"),
                        jugado(beto, cata, "6-0 6-0")));

        assertThat(tabla).extracting(PosicionResponse::participante)
                .containsExactly("Ana", "Beto", "Cata");
        assertThat(tabla).extracting(PosicionResponse::puesto).containsExactly(1, 2, 3);
    }

    @Test
    void conLosMismosGanadosDesempataLaDiferenciaDeSets() {
        Inscripcion ana = equipo(1L, "Ana");
        Inscripcion beto = equipo(2L, "Beto");
        Inscripcion cata = equipo(3L, "Cata");

        // Ciclo: Ana le gana a Beto, Beto a Cata y Cata a Ana. Los tres terminan con un ganado y un perdido,
        // asi que el orden lo define unicamente la diferencia de sets.
        List<PosicionResponse> tabla = TablaDePosiciones.calcular(
                List.of(ana, beto, cata),
                List.of(jugado(ana, beto, "6-0 6-0"),
                        jugado(beto, cata, "6-4 3-6 7-5"),
                        jugado(cata, ana, "6-0 6-0")));

        assertThat(tabla).allSatisfy(p -> assertThat(p.ganados()).isEqualTo(1));
        assertThat(tabla).extracting(PosicionResponse::participante)
                .containsExactly("Cata", "Ana", "Beto");
        assertThat(tabla).extracting(PosicionResponse::diferenciaDeSets)
                .containsExactly(1, 0, -1);
    }

    @Test
    void losPartidosSinResultadoNoSuman() {
        Inscripcion ana = equipo(1L, "Ana");
        Inscripcion beto = equipo(2L, "Beto");

        List<PosicionResponse> tabla = TablaDePosiciones.calcular(
                List.of(ana, beto), List.of(pendiente(ana, beto)));

        assertThat(tabla).allSatisfy(p -> assertThat(p.jugados()).isZero());
    }

    @Test
    void losQueTodaviaNoJugaronAparecenEnCero() {
        Inscripcion ana = equipo(1L, "Ana");
        Inscripcion beto = equipo(2L, "Beto");
        Inscripcion cata = equipo(3L, "Cata");

        List<PosicionResponse> tabla = TablaDePosiciones.calcular(
                List.of(ana, beto, cata), List.of(jugado(ana, beto, "6-0 6-0")));

        assertThat(tabla).hasSize(3);
        // Cata todavia no jugo, asi que queda en cero; Beto perdio y arrastra -2 de sets. En una tabla, el que
        // todavia no debuto va por encima del que perdio.
        assertThat(tabla).extracting(PosicionResponse::participante)
                .containsExactly("Ana", "Cata", "Beto");
        assertThat(tabla.get(1).jugados()).isZero();
        assertThat(tabla.get(2).diferenciaDeSets()).isNegative();
    }

    @Test
    void sinPartidosJugadosLaTablaEstaCompletaYEnCero() {
        Inscripcion ana = equipo(1L, "Ana");
        Inscripcion beto = equipo(2L, "Beto");

        List<PosicionResponse> tabla = TablaDePosiciones.calcular(List.of(ana, beto), List.of());

        assertThat(tabla).hasSize(2);
        assertThat(tabla).allSatisfy(p -> assertThat(p.jugados()).isZero());
        // Con todo empatado en cero, el nombre deja el orden estable
        assertThat(tabla).extracting(PosicionResponse::participante).containsExactly("Ana", "Beto");
    }

    @Test
    void unTanteadorMalEscritoNoRompeLaTabla() {
        Inscripcion ana = equipo(1L, "Ana");
        Inscripcion beto = equipo(2L, "Beto");

        List<PosicionResponse> tabla = TablaDePosiciones.calcular(
                List.of(ana, beto), List.of(jugado(ana, beto, "ilegible")));

        assertThat(tabla).hasSize(2);
        assertThat(tabla.get(0).jugados()).isEqualTo(1);
    }
}
