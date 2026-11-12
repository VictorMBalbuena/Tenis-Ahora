package com.tenisahora.modules.actividad.service;

import com.tenisahora.infrastructure.pdf.PdfService;
import com.tenisahora.modules.actividad.dto.EntrenamientoRequest;
import com.tenisahora.modules.actividad.dto.EntrenamientoUpdateRequest;
import com.tenisahora.modules.actividad.entity.Actividad;
import com.tenisahora.modules.actividad.entity.Entrenamiento;
import com.tenisahora.modules.actividad.entity.Modalidad;
import com.tenisahora.modules.actividad.repository.ActividadDiaRepository;
import com.tenisahora.modules.actividad.repository.ActividadRepository;
import com.tenisahora.modules.actividad.repository.EntrenamientoRepository;
import com.tenisahora.modules.actividad.repository.InscripcionClaseRepository;
import com.tenisahora.modules.cancha.entity.Cancha;
import com.tenisahora.modules.cancha.repository.CanchaRepository;
import com.tenisahora.modules.instructor.entity.Entrenador;
import com.tenisahora.modules.instructor.repository.EntrenadorRepository;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.shared.exception.ConflictoException;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import com.tenisahora.shared.model.Rol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.tenisahora.modules.actividad.DatosDePrueba.cancha;
import static com.tenisahora.modules.actividad.DatosDePrueba.entrenador;
import static com.tenisahora.modules.actividad.DatosDePrueba.entrenamiento;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Espejo de ClaseServiceImplTest. */
@ExtendWith(MockitoExtension.class)
class EntrenamientoServiceImplTest {

    private static final Actor EMPLEADO = new Actor(2L, Rol.EMPLEADO);
    private static final EntrenamientoRequest PEDIDO = new EntrenamientoRequest("Entrenamiento funcional", Modalidad.GRUPAL,
            "Mejorar el saque", 1L, 5L, LocalDate.of(2026, 11, 2), LocalDate.of(2027, 3, 2), Set.of(DayOfWeek.MONDAY),
            LocalTime.of(18, 0), LocalTime.of(19, 0), 20, null, null, null);

    @Mock
    private EntrenamientoRepository entrenamientoRepository;
    @Mock
    private ActividadRepository actividadRepository;
    @Mock
    private ActividadDiaRepository diasRepository;
    @Mock
    private InscripcionClaseRepository inscripcionRepository;
    @Mock
    private CanchaRepository canchaRepository;
    @Mock
    private EntrenadorRepository entrenadorRepository;
    @Mock
    private ValidadorDeAgendaDeActividades validador;
    @Mock
    private CupoDeActividad cupoDeActividad;
    @Mock
    private ValidacionDeInstructorPorModalidad validacionDeInstructor;
    @Mock
    private ArmadoDeRespuestasDeEntrenamiento respuestas;
    @Mock
    private PdfService pdfService;

    private EntrenamientoServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EntrenamientoServiceImpl(entrenamientoRepository, actividadRepository, diasRepository, inscripcionRepository,
                canchaRepository, entrenadorRepository, validador, cupoDeActividad, validacionDeInstructor, respuestas, pdfService);
    }

    private void entrenadorYCanchaExisten(boolean validado) {
        when(entrenadorRepository.findById(5L)).thenReturn(Optional.of(entrenador(5L, validado)));
        when(canchaRepository.findById(1L)).thenReturn(Optional.of(cancha(1L)));
    }

    @Test
    void programar_conTodoBien_loGuardaYLeAsignaCodigo() {
        entrenadorYCanchaExisten(true);
        when(cupoDeActividad.asignar(20)).thenReturn(20);
        when(entrenamientoRepository.saveAndFlush(any())).thenAnswer(invocacion -> {
            Entrenamiento entrenamiento = invocacion.getArgument(0);
            entrenamiento.setId(12L);
            return entrenamiento;
        });

        service.programar(PEDIDO, EMPLEADO);

        verify(validador).asegurarAgendaLibre(any(Cancha.class), eq(PEDIDO.fechaInicio()), eq(PEDIDO.fechaFin()), eq(PEDIDO.dias()), any(), isNull());
        verify(diasRepository).saveAll(any());
        verify(respuestas).deEntrenamiento(any());
        verify(validacionDeInstructor).asegurarValida(any());
    }

    @Test
    void programar_entrenadorSinCertificacion_422_A1_noGuardaNada() {
        when(entrenadorRepository.findById(5L)).thenReturn(Optional.of(entrenador(5L, false)));

        assertThatThrownBy(() -> service.programar(PEDIDO, EMPLEADO))
                .isInstanceOf(ReglaDeNegocioException.class)
                .extracting("codigo").isEqualTo("INSTRUCTOR_NO_VALIDADO");
        verify(entrenamientoRepository, never()).save(any());
        verify(entrenamientoRepository, never()).saveAndFlush(any());
    }

    @Test
    void programar_canchaFueraDeHorario_422() {
        entrenadorYCanchaExisten(true);
        org.mockito.Mockito.doThrow(new ReglaDeNegocioException(com.tenisahora.shared.exception.CodigoError.RESERVA_FUERA_DE_HORARIO, "x"))
                .when(validador).asegurarAgendaLibre(any(), any(), any(), any(), any(), any());

        assertThatThrownBy(() -> service.programar(PEDIDO, EMPLEADO)).isInstanceOf(ReglaDeNegocioException.class);
        verify(entrenamientoRepository, never()).saveAndFlush(any());
    }

    @Test
    void programar_canchaBloqueada_409() {
        entrenadorYCanchaExisten(true);
        org.mockito.Mockito.doThrow(new ConflictoException(com.tenisahora.shared.exception.CodigoError.RESERVA_HORARIO_BLOQUEADO, "x"))
                .when(validador).asegurarAgendaLibre(any(), any(), any(), any(), any(), any());

        assertThatThrownBy(() -> service.programar(PEDIDO, EMPLEADO)).isInstanceOf(ConflictoException.class);
    }

    @Test
    void programar_solapadoConOtraActividad_409() {
        entrenadorYCanchaExisten(true);
        org.mockito.Mockito.doThrow(new ConflictoException(com.tenisahora.shared.exception.CodigoError.CANCHA_OCUPADA, "x"))
                .when(validador).asegurarAgendaLibre(any(), any(), any(), any(), any(), any());

        assertThatThrownBy(() -> service.programar(PEDIDO, EMPLEADO))
                .isInstanceOf(ConflictoException.class)
                .extracting("codigo").isEqualTo("CANCHA_OCUPADA");
    }

    @Test
    void programar_solapadoConUnaReservaExistente_409() {
        entrenadorYCanchaExisten(true);
        org.mockito.Mockito.doThrow(new ConflictoException(com.tenisahora.shared.exception.CodigoError.CANCHA_OCUPADA, "reserva"))
                .when(validador).asegurarAgendaLibre(any(), any(), any(), any(), any(), any());

        assertThatThrownBy(() -> service.programar(PEDIDO, EMPLEADO)).hasMessageContaining("reserva");
    }

    @Test
    void modificar_aUnHorarioOcupado_409_A1() {
        Entrenamiento existente = entrenamiento(10L);
        when(actividadRepository.buscarParaModificar(10L)).thenReturn(Optional.of((Actividad) existente));
        org.mockito.Mockito.doThrow(new ConflictoException(com.tenisahora.shared.exception.CodigoError.CANCHA_OCUPADA, "x"))
                .when(validador).asegurarAgendaLibre(any(), any(), any(), any(), any(), eq(10L));
        when(diasRepository.findByActividadId(10L)).thenReturn(List.of());

        EntrenamientoUpdateRequest cambios = new EntrenamientoUpdateRequest(null, null, null, null, null, null, null, null,
                LocalTime.of(20, 0), LocalTime.of(21, 0), null, null, null, null);

        assertThatThrownBy(() -> service.modificar(10L, cambios, EMPLEADO)).isInstanceOf(ConflictoException.class);
    }

    @Test
    void eliminar_conInscriptosActivos_409_A1() {
        when(entrenamientoRepository.findById(10L)).thenReturn(Optional.of(entrenamiento(10L)));
        when(inscripcionRepository.existsByActividadIdAndActivaTrue(10L)).thenReturn(true);

        assertThatThrownBy(() -> service.eliminar(10L))
                .isInstanceOf(ConflictoException.class)
                .extracting("codigo").isEqualTo("ACTIVIDAD_CON_INSCRIPTOS");
        verify(entrenamientoRepository, never()).delete(any(Entrenamiento.class));
    }

    @Test
    void eliminar_sinInscriptos_borraYLimpiaLosDias() {
        Entrenamiento entrenamiento = entrenamiento(10L);
        when(entrenamientoRepository.findById(10L)).thenReturn(Optional.of(entrenamiento));
        when(inscripcionRepository.existsByActividadIdAndActivaTrue(10L)).thenReturn(false);

        service.eliminar(10L);

        verify(diasRepository).deleteByActividadId(10L);
        verify(entrenamientoRepository).delete(entrenamiento);
    }

    @Test
    void imprimir_laFichaTraeObjetivoCanchaEntrenadorYCupo() {
        Entrenamiento entrenamiento = entrenamiento(10L);
        when(entrenamientoRepository.findById(10L)).thenReturn(Optional.of(entrenamiento));
        when(cupoDeActividad.ocupado(10L)).thenReturn(5);
        when(pdfService.generarFicha(any(), any())).thenReturn(new byte[]{1});

        byte[] pdf = service.imprimir(10L);

        assertThat(pdf).isNotEmpty();
        verify(pdfService).generarFicha(eq("Ficha de entrenamiento"), argThat((List<PdfService.CampoFicha> campos) -> campos.stream()
                .anyMatch(campo -> campo.etiqueta().equals("Cupo") && campo.valor().equals("5 / 20"))));
    }
}
