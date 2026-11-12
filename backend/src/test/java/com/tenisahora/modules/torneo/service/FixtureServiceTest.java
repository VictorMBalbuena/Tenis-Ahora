package com.tenisahora.modules.torneo.service;

import com.tenisahora.infrastructure.pdf.PdfService;
import com.tenisahora.modules.socio.entity.Socio;
import com.tenisahora.modules.torneo.dto.CargarResultadoRequest;
import com.tenisahora.modules.torneo.dto.PartidoResponse;
import com.tenisahora.modules.torneo.entity.EstadoInscripcionTorneo;
import com.tenisahora.modules.torneo.entity.EstadoTorneo;
import com.tenisahora.modules.torneo.entity.Inscripcion;
import com.tenisahora.modules.torneo.entity.Partido;
import com.tenisahora.modules.torneo.entity.TipoCompetencia;
import com.tenisahora.modules.torneo.entity.Torneo;
import com.tenisahora.modules.torneo.repository.InscripcionRepository;
import com.tenisahora.modules.torneo.repository.PartidoRepository;
import com.tenisahora.modules.torneo.repository.TorneoRepository;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.ConflictoException;
import com.tenisahora.shared.exception.SolicitudInvalidaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FixtureServiceTest {

    @Mock
    private TorneoRepository torneoRepository;
    @Mock
    private PartidoRepository partidoRepository;
    @Mock
    private InscripcionRepository inscripcionRepository;
    @Mock
    private PdfService pdfService;

    @InjectMocks
    private FixtureService fixtureService;

    private Torneo torneo;
    private List<Inscripcion> inscriptos4;

    @BeforeEach
    void setUp() {
        torneo = new Torneo();
        torneo.setId(1L);
        torneo.setEstado(EstadoTorneo.PLANIFICADO);
        torneo.setFechaInicio(LocalDate.of(2026, 12, 1));

        inscriptos4 = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            Inscripcion ins = new Inscripcion();
            ins.setId((long) i);
            Socio s = new Socio();
            s.setId((long) (100 + i));
            s.setNombre("Jugador" + i);
            s.setApellido("Test");
            ins.setJugador1(s);
            ins.setTorneo(torneo);
            ins.setEquipo(i);
            inscriptos4.add(ins);
        }
    }

    @Test
    @DisplayName("Genera fixture para 4 jugadores: 2 partidos de Ronda 1 y 1 partido de Ronda 2")
    void generarFixture4Jugadores() {
        when(torneoRepository.findById(1L)).thenReturn(Optional.of(torneo));
        when(partidoRepository.findByTorneoIdOrderByRondaAscOrdenAsc(1L)).thenReturn(List.of());
        when(inscripcionRepository.findByTorneoId(1L)).thenReturn(inscriptos4);

        List<PartidoResponse> partidos = fixtureService.generarFixture(1L);

        assertThat(partidos).hasSize(3);
        assertThat(torneo.getEstado()).isEqualTo(EstadoTorneo.EN_CURSO);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Partido>> captor = ArgumentCaptor.forClass(List.class);
        verify(partidoRepository).saveAll(captor.capture());
        List<Partido> guardados = captor.getValue();
        assertThat(guardados).hasSize(3);

        Partido sem1 = guardados.get(0);
        assertThat(sem1.getRonda()).isEqualTo(1);
        assertThat(sem1.getOrden()).isEqualTo(1);
        assertThat(sem1.getParticipanteA().getId()).isEqualTo(1L);
        assertThat(sem1.getParticipanteB().getId()).isEqualTo(2L);

        Partido sem2 = guardados.get(1);
        assertThat(sem2.getRonda()).isEqualTo(1);
        assertThat(sem2.getOrden()).isEqualTo(2);
        assertThat(sem2.getParticipanteA().getId()).isEqualTo(3L);
        assertThat(sem2.getParticipanteB().getId()).isEqualTo(4L);

        Partido fin = guardados.get(2);
        assertThat(fin.getRonda()).isEqualTo(2);
        assertThat(fin.getOrden()).isEqualTo(1);
        assertThat(fin.getParticipanteA()).isNull();
        assertThat(fin.getParticipanteB()).isNull();
    }

    @Test
    @DisplayName("Rechaza generación de fixture si cantidad de inscriptos no es potencia de 2")
    void rechazaNoPotenciaDeDos() {
        when(torneoRepository.findById(1L)).thenReturn(Optional.of(torneo));
        when(partidoRepository.findByTorneoIdOrderByRondaAscOrdenAsc(1L)).thenReturn(List.of());
        when(inscripcionRepository.findByTorneoId(1L)).thenReturn(inscriptos4.subList(0, 3));

        assertThatThrownBy(() -> fixtureService.generarFixture(1L))
                .isInstanceOf(SolicitudInvalidaException.class)
                .satisfies(ex -> assertThat(((SolicitudInvalidaException) ex).getCodigoError())
                        .isEqualTo(CodigoError.CUPOS_INSUFICIENTES));
    }

    @Test
    @DisplayName("Regenerar fixture cuando ya existen partidos lanza ConflictoException FIXTURE_YA_GENERADO")
    void regenerarFixtureLanzaFixtureYaGenerado() {
        torneo.setEstado(EstadoTorneo.EN_CURSO);
        when(torneoRepository.findById(1L)).thenReturn(Optional.of(torneo));
        when(partidoRepository.findByTorneoIdOrderByRondaAscOrdenAsc(1L)).thenReturn(List.of(new Partido()));

        assertThatThrownBy(() -> fixtureService.generarFixture(1L))
                .isInstanceOf(ConflictoException.class)
                .satisfies(ex -> assertThat(((ConflictoException) ex).getCodigoError())
                        .isEqualTo(CodigoError.FIXTURE_YA_GENERADO));
    }

    @Test
    @DisplayName("Cargar resultado en semifinal avanza ganador a la final")
    void cargarResultadoSemifinalAvanza() {
        torneo.setEstado(EstadoTorneo.EN_CURSO);

        Partido sem1 = new Partido();
        sem1.setId(10L);
        sem1.setTorneo(torneo);
        sem1.setRonda(1);
        sem1.setOrden(1);
        sem1.setParticipanteA(inscriptos4.get(0));
        sem1.setParticipanteB(inscriptos4.get(1));

        Partido fin = new Partido();
        fin.setId(20L);
        fin.setTorneo(torneo);
        fin.setRonda(2);
        fin.setOrden(1);

        when(torneoRepository.findById(1L)).thenReturn(Optional.of(torneo));
        when(partidoRepository.findById(10L)).thenReturn(Optional.of(sem1));
        when(partidoRepository.findByTorneoIdAndRondaAndOrden(1L, 2, 1)).thenReturn(Optional.of(fin));

        CargarResultadoRequest req = new CargarResultadoRequest("6-3 6-4");
        PartidoResponse resp = fixtureService.cargarResultado(1L, 10L, req);

        assertThat(resp.resultado()).contains("Gana Jugador1 Test");
        assertThat(sem1.getSets()).isEqualTo("6-3 6-4");
        assertThat(fin.getParticipanteA()).isEqualTo(inscriptos4.get(0));
        assertThat(fin.getParticipanteB()).isNull();
        verify(partidoRepository).save(fin);
        verify(partidoRepository).save(sem1);
    }

    @Test
    @DisplayName("Cargar resultado en final corona campeón y pasa torneo a FINALIZADO")
    void cargarResultadoFinalFinalizaTorneo() {
        torneo.setEstado(EstadoTorneo.EN_CURSO);

        Partido fin = new Partido();
        fin.setId(20L);
        fin.setTorneo(torneo);
        fin.setRonda(2);
        fin.setOrden(1);
        fin.setParticipanteA(inscriptos4.get(0));
        fin.setParticipanteB(inscriptos4.get(2));

        when(torneoRepository.findById(1L)).thenReturn(Optional.of(torneo));
        when(partidoRepository.findById(20L)).thenReturn(Optional.of(fin));
        when(partidoRepository.findByTorneoIdAndRondaAndOrden(1L, 3, 1)).thenReturn(Optional.empty());

        CargarResultadoRequest req = new CargarResultadoRequest("6-4 4-6 7-5");
        PartidoResponse resp = fixtureService.cargarResultado(1L, 20L, req);

        assertThat(resp.resultado()).contains("Gana Jugador1 Test");
        assertThat(torneo.getEstado()).isEqualTo(EstadoTorneo.FINALIZADO);
        verify(torneoRepository).save(torneo);
        verify(partidoRepository).save(fin);
    }

    // --- Ligas: todos contra todos, y solo con los que confirmaron ---

    @Test
    @DisplayName("En una liga de 4 juegan todos contra todos: 6 partidos en 3 fechas")
    void generarFixtureDeLiga() {
        torneo.setTipo(TipoCompetencia.LIGA);
        when(torneoRepository.findById(1L)).thenReturn(Optional.of(torneo));
        when(partidoRepository.findByTorneoIdOrderByRondaAscOrdenAsc(1L)).thenReturn(List.of());
        when(inscripcionRepository.findByTorneoId(1L)).thenReturn(inscriptos4);

        List<PartidoResponse> partidos = fixtureService.generarFixture(1L);

        // n*(n-1)/2 = 6, repartidos en n-1 = 3 fechas
        assertThat(partidos).hasSize(6);
        assertThat(partidos).extracting(PartidoResponse::ronda).containsOnly(1, 2, 3);
        // En una liga nadie espera rival: todos los partidos salen con los dos jugadores puestos
        assertThat(partidos).allSatisfy(p -> {
            assertThat(p.participanteAId()).isNotNull();
            assertThat(p.participanteBId()).isNotNull();
        });
    }

    @Test
    @DisplayName("Una liga admite cualquier cantidad desde 2, no solo potencias de 2")
    void ligaConTresInscriptos() {
        torneo.setTipo(TipoCompetencia.LIGA);
        when(torneoRepository.findById(1L)).thenReturn(Optional.of(torneo));
        when(partidoRepository.findByTorneoIdOrderByRondaAscOrdenAsc(1L)).thenReturn(List.of());
        when(inscripcionRepository.findByTorneoId(1L)).thenReturn(inscriptos4.subList(0, 3));

        // Lo mismo con 3 en un torneo falla: ver rechazaNoPotenciaDeDos
        assertThat(fixtureService.generarFixture(1L)).hasSize(3);
    }

    @Test
    @DisplayName("Al armar la liga solo entran los inscriptos confirmados")
    void ligaIgnoraInscripcionesSinConfirmar() {
        torneo.setTipo(TipoCompetencia.LIGA);
        inscriptos4.get(2).setEstado(EstadoInscripcionTorneo.PENDIENTE_PAGO);
        inscriptos4.get(3).setEstado(EstadoInscripcionTorneo.CANCELADA);
        when(torneoRepository.findById(1L)).thenReturn(Optional.of(torneo));
        when(partidoRepository.findByTorneoIdOrderByRondaAscOrdenAsc(1L)).thenReturn(List.of());
        when(inscripcionRepository.findByTorneoId(1L)).thenReturn(inscriptos4);

        List<PartidoResponse> partidos = fixtureService.generarFixture(1L);

        // Quedan 2 confirmados: un solo partido entre ellos
        assertThat(partidos).hasSize(1);
        assertThat(partidos.get(0).participanteAId()).isEqualTo(1L);
        assertThat(partidos.get(0).participanteBId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("Una liga con un solo confirmado no se puede armar")
    void ligaConUnSoloConfirmado() {
        torneo.setTipo(TipoCompetencia.LIGA);
        inscriptos4.subList(1, 4).forEach(i -> i.setEstado(EstadoInscripcionTorneo.CANCELADA));
        when(torneoRepository.findById(1L)).thenReturn(Optional.of(torneo));
        when(partidoRepository.findByTorneoIdOrderByRondaAscOrdenAsc(1L)).thenReturn(List.of());
        when(inscripcionRepository.findByTorneoId(1L)).thenReturn(inscriptos4);

        assertThatThrownBy(() -> fixtureService.generarFixture(1L))
                .isInstanceOf(SolicitudInvalidaException.class)
                .satisfies(ex -> assertThat(((SolicitudInvalidaException) ex).getCodigoError())
                        .isEqualTo(CodigoError.CUPOS_INSUFICIENTES));
    }
}
