package com.tenisahora.modules.torneo.service;

import com.tenisahora.modules.socio.entity.Socio;
import com.tenisahora.modules.socio.repository.SocioRepository;
import com.tenisahora.modules.torneo.dto.InscribirTorneoRequest;
import com.tenisahora.modules.torneo.dto.InscripcionResponse;
import com.tenisahora.modules.torneo.entity.EstadoTorneo;
import com.tenisahora.modules.torneo.entity.Inscripcion;
import com.tenisahora.modules.torneo.entity.Modalidad;
import com.tenisahora.modules.torneo.entity.Torneo;
import com.tenisahora.modules.torneo.repository.InscripcionRepository;
import com.tenisahora.modules.torneo.repository.TorneoRepository;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.ConflictoException;
import com.tenisahora.shared.exception.SolicitudInvalidaException;
import com.tenisahora.shared.model.Rol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InscripcionServiceTest {

    @Mock
    private TorneoRepository torneoRepository;
    @Mock
    private InscripcionRepository inscripcionRepository;
    @Mock
    private SocioRepository socioRepository;

    @InjectMocks
    private InscripcionService inscripcionService;

    private Torneo torneoSingle;
    private Torneo torneoPareja;
    private Socio socio1;
    private Socio socio2;

    @BeforeEach
    void setUp() {
        torneoSingle = new Torneo();
        torneoSingle.setId(1L);
        torneoSingle.setModalidad(Modalidad.SINGLE);
        torneoSingle.setEstado(EstadoTorneo.PLANIFICADO);

        torneoPareja = new Torneo();
        torneoPareja.setId(2L);
        torneoPareja.setModalidad(Modalidad.PAREJA);
        torneoPareja.setEstado(EstadoTorneo.PLANIFICADO);

        socio1 = new Socio();

        socio1.setSexo(com.tenisahora.shared.model.Sexo.MASCULINO);
        socio1.setId(10L);
        socio1.setNombre("Carlos");
        socio1.setApellido("Alcaraz");

        socio2 = new Socio();

        socio2.setSexo(com.tenisahora.shared.model.Sexo.MASCULINO);
        socio2.setId(20L);
        socio2.setNombre("Rafael");
        socio2.setApellido("Nadal");
    }

    @Test
    @DisplayName("Inscripción exitosa en torneo single")
    void inscripcionSingleExitosa() {
        InscribirTorneoRequest req = new InscribirTorneoRequest(10L, null, null);

        when(torneoRepository.findById(1L)).thenReturn(Optional.of(torneoSingle));
        when(socioRepository.findById(10L)).thenReturn(Optional.of(socio1));
        when(inscripcionRepository.existsSocioEnTorneo(1L, 10L)).thenReturn(false);
        when(inscripcionRepository.findByTorneoId(1L)).thenReturn(Collections.emptyList());
        when(inscripcionRepository.save(any(Inscripcion.class))).thenAnswer(inv -> {
            Inscripcion i = inv.getArgument(0);
            i.setId(100L);
            return i;
        });

        InscripcionResponse resp = inscripcionService.inscribir(1L, req);

        assertThat(resp).isNotNull();
        assertThat(resp.id()).isEqualTo(100L);
        assertThat(resp.equipo()).isEqualTo(1);
        assertThat(resp.jugador1Id()).isEqualTo(10L);
        assertThat(resp.parejaId()).isNull();
        verify(inscripcionRepository).save(any(Inscripcion.class));
    }

    @Test
    @DisplayName("Rechaza inscripción en torneo SINGLE si se envía parejaId")
    void rechazaParejaEnSingle() {
        InscribirTorneoRequest req = new InscribirTorneoRequest(10L, 20L, null);
        when(torneoRepository.findById(1L)).thenReturn(Optional.of(torneoSingle));

        assertThatThrownBy(() -> inscripcionService.inscribir(1L, req))
                .isInstanceOf(SolicitudInvalidaException.class)
                .satisfies(ex -> assertThat(((SolicitudInvalidaException) ex).getCodigoError())
                        .isEqualTo(CodigoError.MODALIDAD_INVALIDA));
    }

    @Test
    @DisplayName("Rechaza inscripción en torneo PAREJA si falta parejaId o si son la misma persona")
    void rechazaInvalidoEnPareja() {
        InscribirTorneoRequest sinPareja = new InscribirTorneoRequest(10L, null, null);
        InscribirTorneoRequest mismaPareja = new InscribirTorneoRequest(10L, 10L, null);
        when(torneoRepository.findById(2L)).thenReturn(Optional.of(torneoPareja));

        assertThatThrownBy(() -> inscripcionService.inscribir(2L, sinPareja))
                .isInstanceOf(SolicitudInvalidaException.class)
                .satisfies(ex -> assertThat(((SolicitudInvalidaException) ex).getCodigoError())
                        .isEqualTo(CodigoError.MODALIDAD_INVALIDA));

        assertThatThrownBy(() -> inscripcionService.inscribir(2L, mismaPareja))
                .isInstanceOf(SolicitudInvalidaException.class)
                .satisfies(ex -> assertThat(((SolicitudInvalidaException) ex).getCodigoError())
                        .isEqualTo(CodigoError.MODALIDAD_INVALIDA));
    }

    @Test
    @DisplayName("Rechaza inscripción duplicada si el socio ya participa")
    void rechazaInscripcionDuplicada() {
        InscribirTorneoRequest req = new InscribirTorneoRequest(10L, null, null);
        when(torneoRepository.findById(1L)).thenReturn(Optional.of(torneoSingle));
        when(inscripcionRepository.existsSocioEnTorneo(1L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> inscripcionService.inscribir(1L, req))
                .isInstanceOf(ConflictoException.class)
                .satisfies(ex -> assertThat(((ConflictoException) ex).getCodigoError())
                        .isEqualTo(CodigoError.INSCRIPCION_DUPLICADA));
    }

    @Test
    @DisplayName("Rechaza inscripción si el torneo no está en estado PLANIFICADO")
    void rechazaTorneoNoPlanificado() {
        torneoSingle.setEstado(EstadoTorneo.EN_CURSO);
        InscribirTorneoRequest req = new InscribirTorneoRequest(10L, null, null);
        when(torneoRepository.findById(1L)).thenReturn(Optional.of(torneoSingle));

        assertThatThrownBy(() -> inscripcionService.inscribir(1L, req))
                .isInstanceOf(SolicitudInvalidaException.class)
                .satisfies(ex -> assertThat(((SolicitudInvalidaException) ex).getCodigoError())
                        .isEqualTo(CodigoError.TORNEO_ESTADO_INVALIDO));
    }

    @Test
    @DisplayName("Socio intentando inscribir a otro socio ajeno recibe AccessDeniedException")
    void socioInscribiendoAOtroLanzaAccesoDenegado() {
        Actor actorSocio99 = new Actor(99L, Rol.SOCIO);
        InscribirTorneoRequest req = new InscribirTorneoRequest(10L, null, null);

        assertThatThrownBy(() -> inscripcionService.inscribir(1L, req, actorSocio99))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Un socio solo puede inscribirse a sí mismo");
    }
}
