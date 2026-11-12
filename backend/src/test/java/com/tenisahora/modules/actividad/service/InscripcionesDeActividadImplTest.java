package com.tenisahora.modules.actividad.service;

import com.tenisahora.modules.actividad.dto.InscripcionRequest;
import com.tenisahora.modules.actividad.entity.Clase;
import com.tenisahora.modules.actividad.entity.InscripcionClase;
import com.tenisahora.modules.actividad.mapper.ClaseMapper;
import com.tenisahora.modules.actividad.repository.ActividadRepository;
import com.tenisahora.modules.actividad.repository.InscripcionClaseRepository;
import com.tenisahora.modules.socio.entity.Socio;
import com.tenisahora.modules.socio.repository.SocioRepository;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.ConflictoException;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import com.tenisahora.shared.model.Rol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.tenisahora.modules.actividad.DatosDePrueba.RELOJ;
import static com.tenisahora.modules.actividad.DatosDePrueba.clase;
import static com.tenisahora.modules.actividad.DatosDePrueba.socio;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InscripcionesDeActividadImplTest {

    private static final long CLASE = 1L;
    private static final Actor SOCIO = new Actor(3L, Rol.SOCIO);
    private static final Actor EMPLEADO = new Actor(2L, Rol.EMPLEADO);

    @Mock
    private ActividadRepository actividadRepository;
    @Mock
    private InscripcionClaseRepository inscripcionRepository;
    @Mock
    private SocioRepository socioRepository;
    @Mock
    private CupoDeActividad cupoDeActividad;
    @Mock
    private ClaseMapper claseMapper;

    @Mock
    private PoliticaDeBajaDeInscripcion politica;
    @Mock
    private org.springframework.context.ApplicationEventPublisher eventos;

    private InscripcionesDeActividadImpl impl;
    private Clase clase;

    @BeforeEach
    void setUp() {
        impl = new InscripcionesDeActividadImpl(actividadRepository, inscripcionRepository, socioRepository, cupoDeActividad, claseMapper, RELOJ, politica, eventos);
        org.mockito.Mockito.lenient().when(politica.evaluar(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new PoliticaDeBajaDeInscripcion.ResultadoDeBaja(true, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO));
        clase = clase(CLASE);
    }

    private void claseBloqueada() {
        when(actividadRepository.buscarParaModificar(CLASE)).thenReturn(Optional.of(clase));
    }

    private com.tenisahora.modules.actividad.dto.InscripcionResponse inscribir(InscripcionRequest request, Actor actor) {
        return impl.inscribir(CLASE, Clase.class, CodigoError.CLASE_NO_ENCONTRADA, request, actor);
    }

    @Test
    void inscribir_comoSocio_dejaCupoOcupadoMasUno() {
        claseBloqueada();
        when(socioRepository.findById(3L)).thenReturn(Optional.of(socio(3L)));
        when(inscripcionRepository.findByActividadIdAndSocioId(CLASE, 3L)).thenReturn(Optional.empty());

        inscribir(new InscripcionRequest(null), SOCIO);

        verify(inscripcionRepository).save(any());
        verify(cupoDeActividad).asegurarQueHayLugar(CLASE, 20);
    }

    @Test
    void inscribir_cupoCompleto_422_A1() {
        claseBloqueada();
        when(socioRepository.findById(3L)).thenReturn(Optional.of(socio(3L)));
        doThrowCupoCompleto();

        assertThatThrownBy(() -> inscribir(new InscripcionRequest(null), SOCIO))
                .isInstanceOf(ReglaDeNegocioException.class)
                .extracting("codigo").isEqualTo("CUPO_COMPLETO");
        verify(inscripcionRepository, never()).save(any());
    }

    @Test
    void inscribir_yaInscripto_409_A2() {
        claseBloqueada();
        when(socioRepository.findById(3L)).thenReturn(Optional.of(socio(3L)));
        InscripcionClase activa = new InscripcionClase();
        activa.setActiva(true);
        when(inscripcionRepository.findByActividadIdAndSocioId(CLASE, 3L)).thenReturn(Optional.of(activa));

        assertThatThrownBy(() -> inscribir(new InscripcionRequest(null), SOCIO))
                .isInstanceOf(ConflictoException.class)
                .extracting("codigo").isEqualTo("YA_INSCRIPTO");
    }

    @Test
    void inscribir_reinscripcionDespuesDeCancelar_reactivaLaFilaYNoInsertaOtra() {
        claseBloqueada();
        when(socioRepository.findById(3L)).thenReturn(Optional.of(socio(3L)));
        InscripcionClase cancelada = new InscripcionClase();
        cancelada.setActiva(false);
        cancelada.setSocio(socio(3L));
        cancelada.setActividad(clase);
        when(inscripcionRepository.findByActividadIdAndSocioId(CLASE, 3L)).thenReturn(Optional.of(cancelada));

        inscribir(new InscripcionRequest(null), SOCIO);

        verify(inscripcionRepository).save(cancelada);
        assertThat(cancelada.getActiva()).isTrue();
    }

    @Test
    void inscribir_unSocioConOtroSocioId_403() {
        assertThatThrownBy(() -> inscribir(new InscripcionRequest(99L), SOCIO))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        verify(actividadRepository, never()).buscarParaModificar(any());
    }

    @Test
    void inscribir_claseNoProgramada_422() {
        clase.setEstado(com.tenisahora.modules.actividad.entity.EstadoActividad.FINALIZADA);
        claseBloqueada();

        assertThatThrownBy(() -> inscribir(new InscripcionRequest(null), SOCIO))
                .isInstanceOf(ReglaDeNegocioException.class)
                .extracting("codigo").isEqualTo("ACTIVIDAD_NO_INSCRIBIBLE");
    }

    @Test
    void inscribir_comoEmpleadoSinIndicarSocio_400() {
        claseBloqueada();

        assertThatThrownBy(() -> inscribir(new InscripcionRequest(null), EMPLEADO))
                .isInstanceOf(com.tenisahora.shared.exception.SolicitudInvalidaException.class);
    }

    // ---------- Lo mismo, pero sobre un entrenamiento: mismo código, sin duplicarlo (Fase 2 de BACK 12) ----------

    @Test
    void inscribir_sobreUnEntrenamiento_funcionaIgual() {
        com.tenisahora.modules.actividad.entity.Entrenamiento entrenamiento = com.tenisahora.modules.actividad.DatosDePrueba.entrenamiento(CLASE);
        when(actividadRepository.buscarParaModificar(CLASE)).thenReturn(Optional.of(entrenamiento));
        when(socioRepository.findById(3L)).thenReturn(Optional.of(socio(3L)));
        when(inscripcionRepository.findByActividadIdAndSocioId(CLASE, 3L)).thenReturn(Optional.empty());

        impl.inscribir(CLASE, com.tenisahora.modules.actividad.entity.Entrenamiento.class, CodigoError.ENTRENAMIENTO_NO_ENCONTRADO,
                new InscripcionRequest(null), SOCIO);

        verify(inscripcionRepository).save(any());
        verify(cupoDeActividad).asegurarQueHayLugar(CLASE, 20);
    }

    @Test
    void inscribir_unaClaseComoSiFueraUnEntrenamiento_404_noConfundeTipos() {
        claseBloqueada();

        assertThatThrownBy(() -> impl.inscribir(CLASE, com.tenisahora.modules.actividad.entity.Entrenamiento.class,
                CodigoError.ENTRENAMIENTO_NO_ENCONTRADO, new InscripcionRequest(null), SOCIO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .extracting("codigo").isEqualTo("ENTRENAMIENTO_NO_ENCONTRADO");
    }

    @Test
    void cancelar_unSocioConOtraInscripcion_403() {
        assertThatThrownBy(() -> impl.cancelar(CLASE, CodigoError.CLASE_NO_ENCONTRADA, 99L, SOCIO))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        verify(inscripcionRepository, never()).findByActividadIdAndSocioId(any(), any());
    }

    @Test
    void cancelar_laPropia_laDejaInactiva() {
        InscripcionClase activa = new InscripcionClase();
        activa.setActiva(true);
        when(inscripcionRepository.findByActividadIdAndSocioId(CLASE, 3L)).thenReturn(Optional.of(activa));

        impl.cancelar(CLASE, CodigoError.CLASE_NO_ENCONTRADA, 3L, SOCIO);

        assertThat(activa.getActiva()).isFalse();
        verify(inscripcionRepository).save(activa);
    }

    @Test
    void cancelar_inexistente_404() {
        when(inscripcionRepository.findByActividadIdAndSocioId(CLASE, 3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> impl.cancelar(CLASE, CodigoError.CLASE_NO_ENCONTRADA, 3L, SOCIO)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    private void doThrowCupoCompleto() {
        org.mockito.Mockito.doThrow(new ReglaDeNegocioException(CodigoError.CUPO_COMPLETO, "La clase ya no tiene cupo"))
                .when(cupoDeActividad).asegurarQueHayLugar(CLASE, 20);
    }
}
