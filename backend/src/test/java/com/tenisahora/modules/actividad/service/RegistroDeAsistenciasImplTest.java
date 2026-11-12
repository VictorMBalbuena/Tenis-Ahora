package com.tenisahora.modules.actividad.service;

import com.tenisahora.modules.actividad.dto.AsistenciaResponse;
import com.tenisahora.modules.actividad.dto.MarcaDeAsistenciaRequest;
import com.tenisahora.modules.actividad.entity.Actividad;
import com.tenisahora.modules.actividad.entity.ActividadDia;
import com.tenisahora.modules.actividad.entity.Asistencia;
import com.tenisahora.modules.actividad.entity.Clase;
import com.tenisahora.modules.actividad.entity.InscripcionClase;
import com.tenisahora.modules.actividad.repository.ActividadDiaRepository;
import com.tenisahora.modules.actividad.repository.AsistenciaRepository;
import com.tenisahora.modules.actividad.repository.InscripcionClaseRepository;
import com.tenisahora.modules.socio.entity.Socio;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import com.tenisahora.shared.model.Rol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.tenisahora.modules.actividad.DatosDePrueba.HOY;
import static com.tenisahora.modules.actividad.DatosDePrueba.RELOJ;
import static com.tenisahora.modules.actividad.DatosDePrueba.clase;
import static com.tenisahora.modules.actividad.DatosDePrueba.socio;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistroDeAsistenciasImplTest {

    private static final long ACTIVIDAD = 10L;
    private static final Actor EMPLEADO = new Actor(2L, Rol.EMPLEADO);
    private static final Actor PROFESOR_PROPIO = new Actor(4L, Rol.PROFESOR);
    private static final Actor PROFESOR_AJENO = new Actor(99L, Rol.PROFESOR);

    @Mock
    private ActividadDiaRepository diasRepository;
    @Mock
    private AsistenciaRepository asistenciaRepository;
    @Mock
    private InscripcionClaseRepository inscripcionRepository;
    @Mock
    private AccesoAActividades acceso;

    private RegistroDeAsistenciasImpl impl;
    private Clase clase;
    private InscripcionClase inscripcion;

    @BeforeEach
    void setUp() {
        impl = new RegistroDeAsistenciasImpl(diasRepository, asistenciaRepository, inscripcionRepository, acceso, RELOJ);
        clase = clase(ACTIVIDAD);
        Socio socio = socio(3L);
        inscripcion = new InscripcionClase();
        inscripcion.setActividad(clase);
        inscripcion.setSocio(socio);
        inscripcion.setActiva(true);
    }

    private void actividadAccesible(Actor actor) {
        when(acceso.paraOperar(ACTIVIDAD, Actividad.class, CodigoError.ACTIVIDAD_NO_ENCONTRADA, actor)).thenReturn(clase);
    }

    private void diasLunesYMiercoles() {
        when(diasRepository.findByActividadId(ACTIVIDAD)).thenReturn(List.of(
                new ActividadDia(clase, DayOfWeek.MONDAY), new ActividadDia(clase, DayOfWeek.WEDNESDAY)));
    }

    @Test
    void registrar_marcaEnLotePresentesYAusentes() {
        actividadAccesible(EMPLEADO);
        diasLunesYMiercoles();
        when(inscripcionRepository.deLaActividad(ACTIVIDAD)).thenReturn(List.of(inscripcion));
        when(asistenciaRepository.findByActividadIdAndSocioIdAndFecha(ACTIVIDAD, 3L, HOY)).thenReturn(Optional.empty());

        List<AsistenciaResponse> resultado = impl.registrar(ACTIVIDAD, HOY, List.of(new MarcaDeAsistenciaRequest(3L, true)), EMPLEADO);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).presente()).isTrue();
    }

    @Test
    void registrar_volverARegistrar_corrigeYNoDuplica() {
        actividadAccesible(EMPLEADO);
        diasLunesYMiercoles();
        when(inscripcionRepository.deLaActividad(ACTIVIDAD)).thenReturn(List.of(inscripcion));
        Asistencia existente = new Asistencia();
        existente.setActividad(clase);
        existente.setSocio(inscripcion.getSocio());
        existente.setFecha(HOY);
        existente.setPresente(true);
        when(asistenciaRepository.findByActividadIdAndSocioIdAndFecha(ACTIVIDAD, 3L, HOY)).thenReturn(Optional.of(existente));

        List<AsistenciaResponse> resultado = impl.registrar(ACTIVIDAD, HOY, List.of(new MarcaDeAsistenciaRequest(3L, false)), EMPLEADO);

        assertThat(resultado.get(0).presente()).isFalse();
        assertThat(existente.getPresente()).isFalse();
        org.mockito.Mockito.verify(asistenciaRepository).save(existente);
    }

    @Test
    void registrar_fechaDeOtroDiaDeLaSemana_422_FECHA_SIN_DICTADO() {
        clase.setFechaInicio(HOY.minusWeeks(2));
        actividadAccesible(EMPLEADO);
        diasLunesYMiercoles();

        LocalDate domingoPasado = HOY.minusDays(1);
        assertThatThrownBy(() -> impl.registrar(ACTIVIDAD, domingoPasado, List.of(new MarcaDeAsistenciaRequest(3L, true)), EMPLEADO))
                .isInstanceOf(ReglaDeNegocioException.class)
                .extracting("codigo").isEqualTo("FECHA_SIN_DICTADO");
    }

    @Test
    void registrar_fechaFueraDelRango_422() {
        actividadAccesible(EMPLEADO);

        assertThatThrownBy(() -> impl.registrar(ACTIVIDAD, clase.getFechaInicio().minusDays(1),
                List.of(new MarcaDeAsistenciaRequest(3L, true)), EMPLEADO))
                .isInstanceOf(ReglaDeNegocioException.class)
                .extracting("codigo").isEqualTo("FECHA_SIN_DICTADO");
    }

    @Test
    void registrar_fechaFutura_422() {
        actividadAccesible(EMPLEADO);

        assertThatThrownBy(() -> impl.registrar(ACTIVIDAD, HOY.plusDays(1), List.of(new MarcaDeAsistenciaRequest(3L, true)), EMPLEADO))
                .isInstanceOf(ReglaDeNegocioException.class)
                .extracting("codigo").isEqualTo("FECHA_SIN_DICTADO");
    }

    @Test
    void registrar_socioNoInscripto_422() {
        actividadAccesible(EMPLEADO);
        diasLunesYMiercoles();
        when(inscripcionRepository.deLaActividad(ACTIVIDAD)).thenReturn(List.of());

        assertThatThrownBy(() -> impl.registrar(ACTIVIDAD, HOY, List.of(new MarcaDeAsistenciaRequest(3L, true)), EMPLEADO))
                .isInstanceOf(ReglaDeNegocioException.class)
                .extracting("codigo").isEqualTo("SOCIO_NO_INSCRIPTO");
    }

    @Test
    void registrar_profesorSobreUnaClaseAjena_404() {
        when(acceso.paraOperar(ACTIVIDAD, Actividad.class, CodigoError.ACTIVIDAD_NO_ENCONTRADA, PROFESOR_AJENO))
                .thenThrow(new RecursoNoEncontradoException(CodigoError.ACTIVIDAD_NO_ENCONTRADA, "No encontrada"));

        assertThatThrownBy(() -> impl.registrar(ACTIVIDAD, HOY, List.of(new MarcaDeAsistenciaRequest(3L, true)), PROFESOR_AJENO))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void registrar_profesorSobreSuPropiaClase_funciona() {
        actividadAccesible(PROFESOR_PROPIO);
        diasLunesYMiercoles();
        when(inscripcionRepository.deLaActividad(ACTIVIDAD)).thenReturn(List.of(inscripcion));
        when(asistenciaRepository.findByActividadIdAndSocioIdAndFecha(ACTIVIDAD, 3L, HOY)).thenReturn(Optional.empty());

        List<AsistenciaResponse> resultado = impl.registrar(ACTIVIDAD, HOY, List.of(new MarcaDeAsistenciaRequest(3L, true)), PROFESOR_PROPIO);

        assertThat(resultado).hasSize(1);
    }

    @Test
    void delDia_devuelveALosInscriptosAunqueTodaviaNoEstenMarcados() {
        actividadAccesible(EMPLEADO);
        when(inscripcionRepository.deLaActividad(ACTIVIDAD)).thenReturn(List.of(inscripcion));
        when(asistenciaRepository.deLaActividadEnLaFecha(ACTIVIDAD, HOY)).thenReturn(List.of());

        List<AsistenciaResponse> resultado = impl.delDia(ACTIVIDAD, HOY, EMPLEADO);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).presente()).isFalse();
    }
}
