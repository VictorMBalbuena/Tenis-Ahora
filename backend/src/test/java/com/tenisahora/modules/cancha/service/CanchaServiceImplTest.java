package com.tenisahora.modules.cancha.service;

import com.tenisahora.modules.cancha.dto.CanchaRequest;
import com.tenisahora.modules.cancha.dto.CanchaResponse;
import com.tenisahora.modules.cancha.dto.CanchaUpdateRequest;
import com.tenisahora.modules.cancha.dto.EstadoCancha;
import com.tenisahora.modules.cancha.entity.Cancha;
import com.tenisahora.modules.cancha.entity.EstadoDisponibilidad;
import com.tenisahora.modules.cancha.mapper.CanchaMapper;
import com.tenisahora.modules.cancha.repository.CanchaRepository;
import com.tenisahora.modules.cancha.repository.DisponibilidadRepository;
import com.tenisahora.shared.dto.PageResponse;
import com.tenisahora.shared.exception.ConflictoException;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CanchaServiceImplTest {

    private static final long ACTOR = 2L;
    // 2026-10-19 10:30 UTC
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-19T10:30:00Z"), ZoneOffset.UTC);

    @Mock
    private CanchaRepository canchaRepository;
    @Mock
    private DisponibilidadRepository disponibilidadRepository;
    @Spy
    private CanchaMapper canchaMapper = new CanchaMapper();

    private CanchaServiceImpl servicio;

    @BeforeEach
    void setUp() {
        servicio = new CanchaServiceImpl(canchaRepository, disponibilidadRepository, new CanchaPorId(canchaRepository), canchaMapper, RELOJ);
    }

    private static Cancha cancha(Long id, String identificador) {
        Cancha cancha = new Cancha();
        cancha.setId(id);
        cancha.setIdentificador(identificador);
        cancha.setTipoSuperficie("CEMENTO");
        cancha.setCapacidad(4);
        cancha.setPrecioPorHora(new BigDecimal("6000"));
        cancha.setActiva(true);
        return cancha;
    }

    private static CanchaRequest alta() {
        return new CanchaRequest("  Cancha 3 ", "PASTO", 4, new BigDecimal("7000"));
    }

    // ---------- CU06 ----------

    @Test
    void registrar_guardaLaCanchaActivaConElIdentificadorRecortado() {
        when(canchaRepository.existsByIdentificadorIgnoreCase("Cancha 3")).thenReturn(false);
        when(canchaRepository.saveAndFlush(any(Cancha.class))).thenAnswer(inv -> {
            Cancha guardada = inv.getArgument(0);
            guardada.setId(9L);
            return guardada;
        });

        CanchaResponse respuesta = servicio.registrar(alta(), ACTOR);

        assertThat(respuesta.id()).isEqualTo(9L);
        assertThat(respuesta.identificador()).isEqualTo("Cancha 3");
        assertThat(respuesta.tipoSuperficie()).isEqualTo("PASTO");
        assertThat(respuesta.activa()).isTrue();
        assertThat(respuesta.enMantenimiento()).isFalse();
    }

    @Test
    void registrar_conIdentificadorRepetido_409YNoGuarda() {
        when(canchaRepository.existsByIdentificadorIgnoreCase("Cancha 3")).thenReturn(true);

        assertThatThrownBy(() -> servicio.registrar(alta(), ACTOR))
                .isInstanceOf(ConflictoException.class)
                .extracting("codigo").isEqualTo("CANCHA_IDENTIFICADOR_DUPLICADO");
        verify(canchaRepository, never()).saveAndFlush(any());
    }

    // ---------- CU08 ----------

    @Test
    void consultar_devuelveLaCanchaYSiEstaEnMantenimiento() {
        when(canchaRepository.findById(1L)).thenReturn(Optional.of(cancha(1L, "Cancha 1")));
        when(disponibilidadRepository.canchasBloqueadasEn(Set.of(1L), EstadoDisponibilidad.MANTENIMIENTO,
                LocalDate.of(2026, 10, 19), LocalTime.of(10, 30))).thenReturn(Set.of(1L));

        CanchaResponse respuesta = servicio.consultar(1L);

        assertThat(respuesta.identificador()).isEqualTo("Cancha 1");
        assertThat(respuesta.enMantenimiento()).isTrue();
    }

    @Test
    void consultar_inexistente_404() {
        when(canchaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.consultar(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("Cancha no encontrada")
                .extracting("codigo").isEqualTo("CANCHA_NO_ENCONTRADA");
    }

    @Test
    void buscar_sinFiltros_usaElPatronQueTraeTodas() {
        Page<Cancha> pagina = new PageImpl<>(List.of(cancha(1L, "Cancha 1"), cancha(2L, "Cancha 2")));
        when(canchaRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pagina);
        when(disponibilidadRepository.canchasBloqueadasEn(anyCollection(), any(), any(), any())).thenReturn(Set.of(2L));

        PageResponse<CanchaResponse> respuesta = servicio.buscar(null, "  ", null, 0, 20);

        assertThat(respuesta.contenido()).extracting(CanchaResponse::enMantenimiento).containsExactly(false, true);
    }

    @Test
    void buscar_porSuperficie_usaLaConsultaConSuperficie() {
        when(canchaRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        PageResponse<CanchaResponse> respuesta = servicio.buscar("PASTO", null, null, 0, 20);

        assertThat(respuesta.contenido()).isEmpty();
        verify(disponibilidadRepository, never()).canchasBloqueadasEn(any(), any(), any(), any());
    }

    @Test
    void buscar_porIdentificador_normalizaYEscapaLosComodines() {
        when(canchaRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        servicio.buscar(null, " Cáncha_1% ", null, 0, 20);

        verify(canchaRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void buscar_porEstadoMantenimiento_filtraActivaYConBloqueoVigente() {
        when(canchaRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(cancha(1L, "Cancha 1"))));
        when(disponibilidadRepository.canchasBloqueadasEn(anyCollection(), any(), any(), any())).thenReturn(Set.of(1L));

        PageResponse<CanchaResponse> respuesta = servicio.buscar(null, null, EstadoCancha.MANTENIMIENTO, 0, 20);

        assertThat(respuesta.contenido()).extracting(CanchaResponse::enMantenimiento).containsExactly(true);
    }

    // ---------- CU07 ----------

    @Test
    void modificar_cambiaSoloLoQueVieneYGuarda() {
        Cancha existente = cancha(1L, "Cancha 1");
        when(canchaRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(canchaRepository.existsByIdentificadorIgnoreCaseAndIdNot("Central", 1L)).thenReturn(false);

        CanchaResponse respuesta = servicio.modificar(1L,
                new CanchaUpdateRequest("Central", null, 2, null, false), ACTOR);

        assertThat(respuesta.identificador()).isEqualTo("Central");
        assertThat(respuesta.capacidad()).isEqualTo(2);
        assertThat(respuesta.activa()).isFalse();
        assertThat(respuesta.tipoSuperficie()).isEqualTo("CEMENTO");
        assertThat(respuesta.precioPorHora()).isEqualByComparingTo("6000");
        verify(canchaRepository).saveAndFlush(existente);
    }

    @Test
    void modificar_sinIdentificador_noChequeaDuplicados() {
        when(canchaRepository.findById(1L)).thenReturn(Optional.of(cancha(1L, "Cancha 1")));

        servicio.modificar(1L, new CanchaUpdateRequest(null, "PASTO", null, null, null), ACTOR);

        verify(canchaRepository, never()).existsByIdentificadorIgnoreCaseAndIdNot(anyString(), any());
    }

    @Test
    void modificar_conIdentificadorDeOtraCancha_409() {
        when(canchaRepository.findById(1L)).thenReturn(Optional.of(cancha(1L, "Cancha 1")));
        when(canchaRepository.existsByIdentificadorIgnoreCaseAndIdNot("Cancha 2", 1L)).thenReturn(true);

        assertThatThrownBy(() -> servicio.modificar(1L, new CanchaUpdateRequest("Cancha 2", null, null, null, null), ACTOR))
                .isInstanceOf(ConflictoException.class)
                .extracting("codigo").isEqualTo("CANCHA_IDENTIFICADOR_DUPLICADO");
        verify(canchaRepository, never()).saveAndFlush(any());
    }

    @Test
    void modificar_inexistente_404() {
        when(canchaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.modificar(99L, new CanchaUpdateRequest(null, null, 2, null, null), ACTOR))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }
}
