package com.tenisahora.modules.torneo.service;

import com.tenisahora.modules.cancha.entity.Cancha;
import com.tenisahora.modules.cancha.repository.CanchaRepository;
import com.tenisahora.modules.torneo.dto.CrearTorneoRequest;
import com.tenisahora.modules.torneo.dto.ModificarTorneoRequest;
import com.tenisahora.modules.torneo.dto.TorneoResponse;
import com.tenisahora.modules.torneo.entity.CategoriaTorneo;
import com.tenisahora.modules.torneo.entity.EstadoTorneo;
import com.tenisahora.modules.torneo.entity.Inscripcion;
import com.tenisahora.modules.torneo.entity.Modalidad;
import com.tenisahora.modules.torneo.entity.Torneo;
import com.tenisahora.modules.torneo.repository.InscripcionRepository;
import com.tenisahora.modules.torneo.repository.TorneoRepository;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.ConflictoException;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import com.tenisahora.shared.exception.SolicitudInvalidaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TorneoServiceTest {

    @Mock
    private TorneoRepository torneoRepository;
    @Mock
    private CanchaRepository canchaRepository;
    @Mock
    private InscripcionRepository inscripcionRepository;

    @InjectMocks
    private TorneoService torneoService;

    private Cancha cancha;

    @BeforeEach
    void setUp() {
        cancha = new Cancha();
        cancha.setId(1L);
        cancha.setIdentificador("Cancha 1");
    }

    @Test
    @DisplayName("Crear torneo exitoso inicializa estado en PLANIFICADO")
    void crearTorneoExitoso() {
        CrearTorneoRequest request = new CrearTorneoRequest(
                "Torneo Primavera",
                1L,
                CategoriaTorneo.MASCULINO,
                Modalidad.SINGLE,
                "LADRILLO",
                LocalDate.of(2026, 11, 10),
                LocalDate.of(2026, 11, 20),
                BigDecimal.valueOf(5000), null);

        when(canchaRepository.findById(1L)).thenReturn(Optional.of(cancha));
        when(torneoRepository.save(any(Torneo.class))).thenAnswer(inv -> {
            Torneo t = inv.getArgument(0);
            t.setId(10L);
            return t;
        });

        TorneoResponse response = torneoService.crearTorneo(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.nombre()).isEqualTo("Torneo Primavera");
        assertThat(response.estado()).isEqualTo(EstadoTorneo.PLANIFICADO);
        verify(torneoRepository).save(any(Torneo.class));
    }

    @Test
    @DisplayName("Rechaza creación de torneo si fechaFin es anterior a fechaInicio")
    void crearTorneoFechasInvertidas() {
        CrearTorneoRequest request = new CrearTorneoRequest(
                "Torneo Fallido",
                1L,
                CategoriaTorneo.MASCULINO,
                Modalidad.SINGLE,
                "LADRILLO",
                LocalDate.of(2026, 11, 20),
                LocalDate.of(2026, 11, 10),
                BigDecimal.valueOf(5000), null);

        assertThatThrownBy(() -> torneoService.crearTorneo(request))
                .isInstanceOf(SolicitudInvalidaException.class)
                .satisfies(ex -> assertThat(((SolicitudInvalidaException) ex).getCodigoError())
                        .isEqualTo(CodigoError.TORNEO_FECHAS_INVALIDAS));
    }

    @Test
    @DisplayName("Obtener torneo por ID existente retorna DTO y si no existe lanza RecursoNoEncontradoException")
    void obtenerTorneoPorId() {
        Torneo torneo = new Torneo();
        torneo.setId(5L);
        torneo.setNombre("Torneo Master");
        torneo.setEstado(EstadoTorneo.PLANIFICADO);
        torneo.setFechaInicio(LocalDate.of(2026, 12, 1));
        torneo.setFechaFin(LocalDate.of(2026, 12, 15));
        torneo.setCategoria(CategoriaTorneo.FEMENINO);
        torneo.setModalidad(Modalidad.SINGLE);
        torneo.setTipoSuperficie("CEMENTO");

        when(torneoRepository.findById(5L)).thenReturn(Optional.of(torneo));
        when(torneoRepository.findById(99L)).thenReturn(Optional.empty());

        TorneoResponse resp = torneoService.obtenerTorneo(5L);
        assertThat(resp.id()).isEqualTo(5L);
        assertThat(resp.nombre()).isEqualTo("Torneo Master");

        assertThatThrownBy(() -> torneoService.obtenerTorneo(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .satisfies(ex -> assertThat(((RecursoNoEncontradoException) ex).getCodigoError())
                        .isEqualTo(CodigoError.TORNEO_NO_ENCONTRADO));
    }

    @Test
    @DisplayName("Eliminar torneo con inscriptos lanza ConflictoException")
    void eliminarTorneoConInscriptos() {
        Torneo torneo = new Torneo();
        torneo.setId(5L);
        torneo.setEstado(EstadoTorneo.PLANIFICADO);

        when(torneoRepository.findById(5L)).thenReturn(Optional.of(torneo));
        when(inscripcionRepository.findByTorneoId(5L)).thenReturn(List.of(new Inscripcion()));

        assertThatThrownBy(() -> torneoService.eliminarTorneo(5L))
                .isInstanceOf(ConflictoException.class)
                .satisfies(ex -> assertThat(((ConflictoException) ex).getCodigoError())
                        .isEqualTo(CodigoError.TORNEO_ESTADO_INVALIDO));
    }

    @Test
    @DisplayName("Modificar modalidad con inscriptos existentes lanza ConflictoException")
    void modificarModalidadConInscriptosLanzaConflicto() {
        Torneo torneo = new Torneo();
        torneo.setId(5L);
        torneo.setEstado(EstadoTorneo.PLANIFICADO);
        torneo.setModalidad(Modalidad.SINGLE);
        torneo.setFechaInicio(LocalDate.now());
        torneo.setFechaFin(LocalDate.now().plusDays(5));

        when(torneoRepository.findById(5L)).thenReturn(Optional.of(torneo));
        when(inscripcionRepository.findByTorneoId(5L)).thenReturn(List.of(new Inscripcion()));

        ModificarTorneoRequest req = new ModificarTorneoRequest(
                null, null, null, Modalidad.PAREJA, null, null, null, null, null
        );

        assertThatThrownBy(() -> torneoService.modificarTorneo(5L, req))
                .isInstanceOf(ConflictoException.class)
                .satisfies(ex -> assertThat(((ConflictoException) ex).getCodigoError())
                        .isEqualTo(CodigoError.TORNEO_ESTADO_INVALIDO));
    }

    @Test
    @DisplayName("Eliminar torneo sin inscriptos y en estado PLANIFICADO lo elimina")
    void eliminarTorneoLibre() {
        Torneo torneo = new Torneo();
        torneo.setId(5L);
        torneo.setEstado(EstadoTorneo.PLANIFICADO);

        when(torneoRepository.findById(5L)).thenReturn(Optional.of(torneo));
        when(inscripcionRepository.findByTorneoId(5L)).thenReturn(List.of());

        torneoService.eliminarTorneo(5L);
        verify(torneoRepository).delete(torneo);
    }
}
