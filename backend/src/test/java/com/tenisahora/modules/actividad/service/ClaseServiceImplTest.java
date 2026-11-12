package com.tenisahora.modules.actividad.service;

import com.tenisahora.infrastructure.pdf.PdfService;
import com.tenisahora.modules.actividad.dto.ClaseRequest;
import com.tenisahora.modules.actividad.dto.ClaseUpdateRequest;
import com.tenisahora.modules.actividad.entity.Actividad;
import com.tenisahora.modules.actividad.entity.Clase;
import com.tenisahora.modules.actividad.entity.EstadoActividad;
import com.tenisahora.modules.actividad.entity.Modalidad;
import com.tenisahora.modules.actividad.entity.NivelClase;
import com.tenisahora.modules.actividad.mapper.ClaseMapper;
import com.tenisahora.modules.actividad.repository.ActividadDiaRepository;
import com.tenisahora.modules.actividad.repository.ActividadRepository;
import com.tenisahora.modules.actividad.repository.ClaseRepository;
import com.tenisahora.modules.actividad.repository.InscripcionClaseRepository;
import com.tenisahora.modules.cancha.entity.Cancha;
import com.tenisahora.modules.cancha.repository.CanchaRepository;
import com.tenisahora.modules.instructor.entity.Profesor;
import com.tenisahora.modules.instructor.repository.ProfesorRepository;
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
import static com.tenisahora.modules.actividad.DatosDePrueba.clase;
import static com.tenisahora.modules.actividad.DatosDePrueba.profesor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaseServiceImplTest {

    private static final Actor EMPLEADO = new Actor(2L, Rol.EMPLEADO);
    private static final ClaseRequest PEDIDO = new ClaseRequest("Tenis para principiantes", Modalidad.GRUPAL, NivelClase.INICIAL,
            1L, 4L, LocalDate.of(2026, 11, 2), LocalDate.of(2027, 3, 2), Set.of(DayOfWeek.MONDAY), LocalTime.of(18, 0),
            LocalTime.of(19, 0), 20, null, null, null);

    @Mock
    private ClaseRepository claseRepository;
    @Mock
    private ActividadRepository actividadRepository;
    @Mock
    private ActividadDiaRepository diasRepository;
    @Mock
    private InscripcionClaseRepository inscripcionRepository;
    @Mock
    private CanchaRepository canchaRepository;
    @Mock
    private ProfesorRepository profesorRepository;
    @Mock
    private ValidadorDeAgendaDeActividades validador;
    @Mock
    private CupoDeActividad cupoDeClase;
    @Mock
    private ArmadoDeRespuestasDeClase respuestas;
    @Mock
    private PdfService pdfService;

    private ClaseServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ClaseServiceImpl(claseRepository, actividadRepository, diasRepository, inscripcionRepository, canchaRepository,
                profesorRepository, validador, cupoDeClase, respuestas, pdfService);
    }

    private void profesorYCanchaExisten(boolean validado) {
        when(profesorRepository.findById(4L)).thenReturn(Optional.of(profesor(4L, validado)));
        when(canchaRepository.findById(1L)).thenReturn(Optional.of(cancha(1L)));
    }

    @Test
    void programar_conTodoBien_laGuardaYLeAsignaCodigo() {
        profesorYCanchaExisten(true);
        when(cupoDeClase.asignar(20)).thenReturn(20);
        when(claseRepository.saveAndFlush(any())).thenAnswer(invocacion -> {
            Clase clase = invocacion.getArgument(0);
            clase.setId(12L);
            return clase;
        });

        service.programar(PEDIDO, EMPLEADO);

        verify(validador).asegurarAgendaLibre(any(Cancha.class), eq(PEDIDO.fechaInicio()), eq(PEDIDO.fechaFin()), eq(PEDIDO.dias()), any(), isNull());
        verify(diasRepository).saveAll(any());
        verify(respuestas).deClase(any());
    }

    @Test
    void programar_profesorSinCertificacion_422_A1_noGuardaNada() {
        when(profesorRepository.findById(4L)).thenReturn(Optional.of(profesor(4L, false)));

        assertThatThrownBy(() -> service.programar(PEDIDO, EMPLEADO))
                .isInstanceOf(ReglaDeNegocioException.class)
                .extracting("codigo").isEqualTo("PROFESOR_NO_VALIDADO");
        verify(claseRepository, never()).save(any());
        verify(claseRepository, never()).saveAndFlush(any());
    }

    @Test
    void programar_canchaFueraDeHorario_422() {
        profesorYCanchaExisten(true);
        org.mockito.Mockito.doThrow(new ReglaDeNegocioException(com.tenisahora.shared.exception.CodigoError.RESERVA_FUERA_DE_HORARIO, "x"))
                .when(validador).asegurarAgendaLibre(any(), any(), any(), any(), any(), any());

        assertThatThrownBy(() -> service.programar(PEDIDO, EMPLEADO)).isInstanceOf(ReglaDeNegocioException.class);
        verify(claseRepository, never()).saveAndFlush(any());
    }

    @Test
    void programar_canchaBloqueada_409() {
        profesorYCanchaExisten(true);
        org.mockito.Mockito.doThrow(new ConflictoException(com.tenisahora.shared.exception.CodigoError.RESERVA_HORARIO_BLOQUEADO, "x"))
                .when(validador).asegurarAgendaLibre(any(), any(), any(), any(), any(), any());

        assertThatThrownBy(() -> service.programar(PEDIDO, EMPLEADO)).isInstanceOf(ConflictoException.class);
    }

    @Test
    void programar_solapadaConOtraClase_409() {
        profesorYCanchaExisten(true);
        org.mockito.Mockito.doThrow(new ConflictoException(com.tenisahora.shared.exception.CodigoError.CANCHA_OCUPADA, "x"))
                .when(validador).asegurarAgendaLibre(any(), any(), any(), any(), any(), any());

        assertThatThrownBy(() -> service.programar(PEDIDO, EMPLEADO))
                .isInstanceOf(ConflictoException.class)
                .extracting("codigo").isEqualTo("CANCHA_OCUPADA");
    }

    @Test
    void programar_solapadaConUnaReservaExistente_409() {
        profesorYCanchaExisten(true);
        org.mockito.Mockito.doThrow(new ConflictoException(com.tenisahora.shared.exception.CodigoError.CANCHA_OCUPADA, "reserva"))
                .when(validador).asegurarAgendaLibre(any(), any(), any(), any(), any(), any());

        assertThatThrownBy(() -> service.programar(PEDIDO, EMPLEADO)).hasMessageContaining("reserva");
    }

    @Test
    void modificar_aUnHorarioOcupado_409_A1() {
        Clase existente = clase(10L);
        when(actividadRepository.buscarParaModificar(10L)).thenReturn(Optional.of((Actividad) existente));
        org.mockito.Mockito.doThrow(new ConflictoException(com.tenisahora.shared.exception.CodigoError.CANCHA_OCUPADA, "x"))
                .when(validador).asegurarAgendaLibre(any(), any(), any(), any(), any(), eq(10L));
        when(diasRepository.findByActividadId(10L)).thenReturn(List.of());

        ClaseUpdateRequest cambios = new ClaseUpdateRequest(null, null, null, null, null, null, null, null,
                LocalTime.of(20, 0), LocalTime.of(21, 0), null, null, null, null);

        assertThatThrownBy(() -> service.modificar(10L, cambios, EMPLEADO)).isInstanceOf(ConflictoException.class);
    }

    @Test
    void eliminar_conInscriptosActivos_409_A1() {
        when(claseRepository.findById(10L)).thenReturn(Optional.of(clase(10L)));
        when(inscripcionRepository.existsByActividadIdAndActivaTrue(10L)).thenReturn(true);

        assertThatThrownBy(() -> service.eliminar(10L))
                .isInstanceOf(ConflictoException.class)
                .extracting("codigo").isEqualTo("CLASE_CON_INSCRIPTOS");
        verify(claseRepository, never()).delete(any(Clase.class));
    }

    @Test
    void eliminar_sinInscriptos_borraYLimpiaLosDias() {
        Clase clase = clase(10L);
        when(claseRepository.findById(10L)).thenReturn(Optional.of(clase));
        when(inscripcionRepository.existsByActividadIdAndActivaTrue(10L)).thenReturn(false);

        service.eliminar(10L);

        verify(diasRepository).deleteByActividadId(10L);
        verify(claseRepository).delete(clase);
    }

    @Test
    void imprimir_laFichaTraeHorarioCanchaProfesorYCupo() {
        Clase clase = clase(10L);
        when(claseRepository.findById(10L)).thenReturn(Optional.of(clase));
        when(cupoDeClase.ocupado(10L)).thenReturn(5);
        when(pdfService.generarFicha(any(), any())).thenReturn(new byte[]{1});

        byte[] pdf = service.imprimir(10L);

        assertThat(pdf).isNotEmpty();
        verify(pdfService).generarFicha(eq("Ficha de clase"), argThat((List<PdfService.CampoFicha> campos) -> campos.stream()
                .anyMatch(campo -> campo.etiqueta().equals("Cupo") && campo.valor().equals("5 / 20"))));
    }
}
