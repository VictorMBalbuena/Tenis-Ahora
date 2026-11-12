package com.tenisahora.modules.actividad.service;

import com.tenisahora.modules.actividad.repository.InscripcionClaseRepository;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CupoDeActividadTest {

    private static final long ACTIVIDAD = 1L;
    private static final ActividadesProperties PROPIEDADES = new ActividadesProperties(30, 30);

    @Mock
    private InscripcionClaseRepository inscripcionRepository;

    private CupoDeActividad cupoDeActividad;

    @BeforeEach
    void setUp() {
        cupoDeActividad = new CupoDeActividad(inscripcionRepository, PROPIEDADES);
    }

    @Test
    void asignar_sinPedido_usaElDefault() {
        assertThat(cupoDeActividad.asignar(null)).isEqualTo(30);
    }

    @Test
    void asignar_conPedidoDentroDelLimite_loDevuelveIgual() {
        assertThat(cupoDeActividad.asignar(15)).isEqualTo(15);
    }

    @Test
    void asignar_conPedidoQueSuperaElLimite_422_A1() {
        assertThatThrownBy(() -> cupoDeActividad.asignar(31))
                .isInstanceOf(ReglaDeNegocioException.class)
                .extracting("codigo").isEqualTo("CUPO_SUPERA_EL_LIMITE");
    }

    @Test
    void asegurarQueHayLugar_conCupoLibre_noLanzaNada() {
        when(inscripcionRepository.countByActividadIdAndActivaTrue(ACTIVIDAD)).thenReturn(5L);

        cupoDeActividad.asegurarQueHayLugar(ACTIVIDAD, 20);
    }

    @Test
    void asegurarQueHayLugar_cupoCompleto_422() {
        when(inscripcionRepository.countByActividadIdAndActivaTrue(ACTIVIDAD)).thenReturn(20L);

        assertThatThrownBy(() -> cupoDeActividad.asegurarQueHayLugar(ACTIVIDAD, 20))
                .isInstanceOf(ReglaDeNegocioException.class)
                .extracting("codigo").isEqualTo("CUPO_COMPLETO");
    }

    @Test
    void asegurarCupoSuficienteParaModificar_porDebajoDeLosInscriptos_422() {
        when(inscripcionRepository.countByActividadIdAndActivaTrue(ACTIVIDAD)).thenReturn(10L);

        assertThatThrownBy(() -> cupoDeActividad.asegurarCupoSuficienteParaModificar(ACTIVIDAD, 9))
                .isInstanceOf(ReglaDeNegocioException.class)
                .extracting("codigo").isEqualTo("CUPO_MENOR_A_INSCRIPTOS");
    }

    @Test
    void asegurarCupoSuficienteParaModificar_igualOPorEncima_noLanzaNada() {
        when(inscripcionRepository.countByActividadIdAndActivaTrue(ACTIVIDAD)).thenReturn(10L);

        cupoDeActividad.asegurarCupoSuficienteParaModificar(ACTIVIDAD, 10);
    }

    @Test
    void ocupado_cuentaSoloLasActivas() {
        when(inscripcionRepository.countByActividadIdAndActivaTrue(ACTIVIDAD)).thenReturn(7L);

        assertThat(cupoDeActividad.ocupado(ACTIVIDAD)).isEqualTo(7);
    }
}
