package com.tenisahora.modules.socio.service;

import com.tenisahora.infrastructure.pdf.PdfService;
import com.tenisahora.infrastructure.pdf.PdfService.CampoFicha;
import com.tenisahora.modules.socio.dto.RegistroSocioRequest;
import com.tenisahora.modules.socio.dto.SocioResponse;
import com.tenisahora.modules.socio.entity.Socio;
import com.tenisahora.modules.socio.repository.SocioRepository;
import com.tenisahora.shared.cuenta.DirectorioDeCuentas;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.shared.dto.PageResponse;
import com.tenisahora.shared.dto.PersonaRequest;
import com.tenisahora.shared.dto.PersonaResumenResponse;
import com.tenisahora.shared.dto.PersonaUpdateRequest;
import com.tenisahora.shared.event.PersonaEliminadaEvent;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.ConflictoException;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import com.tenisahora.shared.exception.SolicitudInvalidaException;
import com.tenisahora.shared.model.Rol;
import com.tenisahora.shared.persona.MapeoDePersona;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Cubre la gestión común de personas (la hereda cada rol) a través del servicio de socios. */
@ExtendWith(MockitoExtension.class)
class SocioServiceTest {

    private static final Actor EMPLEADO = new Actor(2L, Rol.EMPLEADO);

    @Mock
    private SocioRepository repositorio;
    @Mock
    private DirectorioDeCuentas directorio;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ApplicationEventPublisher eventos;
    @Mock
    private PdfService pdfService;

    private SocioService servicio;

    @BeforeEach
    void setUp() {
        servicio = new SocioService(repositorio, new MapeoDePersona(), directorio, passwordEncoder, eventos, pdfService);
        lenient().when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hash-de-prueba");
        // Simula la base: al guardar, asigna el id
        lenient().when(repositorio.saveAndFlush(any(Socio.class))).thenAnswer(invocacion -> {
            Socio socio = invocacion.getArgument(0);
            if (socio.getId() == null) {
                socio.setId(12L);
            }
            return socio;
        });
    }

    private RegistroSocioRequest registroValido() {
        return new RegistroSocioRequest("  Ana ", "Gómez", "30123456", "Av. Siempreviva",
                "742", "11", "12345678", "  Ana@Correo.COM ", "Clave-segura-123");
    }

    private PersonaRequest altaValida() {
        return new PersonaRequest("Ana", "Gómez", "30123456", null, "Av. Siempreviva",
                "742", "11", "12345678", "ana@correo.com", "Clave-segura-123");
    }

    private Socio socioExistente(Long id) {
        Socio socio = new Socio();
        socio.setId(id);
        socio.setNombre("Ana");
        socio.setApellido("Gómez");
        socio.setDni("30123456");
        socio.setEmail("ana@correo.com");
        socio.setActivo(true);
        return socio;
    }

    private Socio guardado() {
        ArgumentCaptor<Socio> captor = ArgumentCaptor.forClass(Socio.class);
        verify(repositorio).saveAndFlush(captor.capture());
        return captor.getValue();
    }

    // ---------- CU74 · autorregistro ----------

    @Test
    void registrarse_creaUnSocioConSuNumeroYActivo() {
        SocioResponse respuesta = servicio.registrarse(registroValido());

        Socio socio = guardado();
        assertThat(socio.getRol()).isEqualTo(Rol.SOCIO);
        assertThat(socio.getNumeroSocio()).isEqualTo("S-0012");
        assertThat(socio.getActivo()).isTrue();
        assertThat(socio.getFechaAlta()).isEqualTo(LocalDate.now());
        assertThat(respuesta.rol()).isEqualTo(Rol.SOCIO);
        assertThat(respuesta.numeroSocio()).isEqualTo("S-0012");
    }

    @Test
    void registrarse_guardaLaContraseniaCifradaYNuncaEnClaro() {
        servicio.registrarse(registroValido());

        assertThat(guardado().getPasswordHash()).isEqualTo("$2a$10$hash-de-prueba").isNotEqualTo("Clave-segura-123");
    }

    @Test
    void registrarse_normalizaElCorreoYRecortaLosEspacios() {
        servicio.registrarse(registroValido());

        Socio socio = guardado();
        assertThat(socio.getEmail()).isEqualTo("ana@correo.com");
        assertThat(socio.getNombre()).isEqualTo("Ana");
        verify(directorio).asegurarEmailLibre("ana@correo.com");
    }

    @Test
    void registrarse_dniRepetido_lanzaConflictoSinGuardar() {
        doThrow(new ConflictoException(CodigoError.PERSONA_DNI_DUPLICADO, "Ya existe una persona con ese DNI"))
                .when(directorio).asegurarDniLibre("30123456");

        assertThatThrownBy(() -> servicio.registrarse(registroValido()))
                .isInstanceOfSatisfying(ConflictoException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("PERSONA_DNI_DUPLICADO"));
        verify(repositorio, never()).saveAndFlush(any());
    }

    @Test
    void registrarse_emailRepetido_lanzaConflictoSinGuardar() {
        doThrow(new ConflictoException(CodigoError.PERSONA_EMAIL_DUPLICADO, "Ya existe una persona con ese correo"))
                .when(directorio).asegurarEmailLibre("ana@correo.com");

        assertThatThrownBy(() -> servicio.registrarse(registroValido()))
                .isInstanceOfSatisfying(ConflictoException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("PERSONA_EMAIL_DUPLICADO"));
        verify(repositorio, never()).saveAndFlush(any());
    }

    @Test
    void registrarse_contraseniaDeMasDe72Bytes_seRechaza() {
        // 40 letras "ñ" son 40 caracteres pero 80 bytes: BCrypt ignoraría lo que pasa de 72
        RegistroSocioRequest largo = new RegistroSocioRequest("Ana", "Gómez", "30123456", "Calle 1",
                "100", "11", "12345678", "ana@correo.com", "ñ".repeat(40));

        assertThatThrownBy(() -> servicio.registrarse(largo))
                .isInstanceOfSatisfying(ReglaDeNegocioException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("CONTRASENIA_DEMASIADO_LARGA"));
        verify(repositorio, never()).saveAndFlush(any());
    }

    @Test
    void registrarse_siLaBaseDetectaUnDuplicadoSimultaneo_lanzaConflicto() {
        when(repositorio.saveAndFlush(any(Socio.class))).thenThrow(new DataIntegrityViolationException("unique"));

        assertThatThrownBy(() -> servicio.registrarse(registroValido()))
                .isInstanceOfSatisfying(ConflictoException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("PERSONA_DUPLICADA"));
    }

    // ---------- CU01 · alta por un empleado ----------

    @Test
    void registrar_unSocio_recibeSuNumeroDeSocio() {
        SocioResponse respuesta = servicio.registrar(altaValida(), EMPLEADO);

        assertThat(respuesta.rol()).isEqualTo(Rol.SOCIO);
        assertThat(respuesta.numeroSocio()).isEqualTo("S-0012");
        assertThat(guardado()).isInstanceOf(Socio.class);
    }

    // ---------- CU03 · consulta ----------

    @Test
    void consultar_existente_devuelveSusDatos() {
        when(repositorio.findById(12L)).thenReturn(Optional.of(socioExistente(12L)));

        assertThat(servicio.consultar(12L).dni()).isEqualTo("30123456");
    }

    @Test
    void consultar_inexistente_lanzaNoEncontrado() {
        when(repositorio.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.consultar(99L))
                .isInstanceOfSatisfying(RecursoNoEncontradoException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("PERSONA_NO_ENCONTRADA"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void buscar_porDni_filtraYDevuelveElResumen() {
        Page<Socio> pagina = new PageImpl<>(List.of(socioExistente(12L)));
        when(repositorio.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pagina);

        PageResponse<PersonaResumenResponse> respuesta = servicio.buscar(" 30123456 ", null, 0, 20);

        assertThat(respuesta.contenido()).hasSize(1);
        assertThat(respuesta.contenido().get(0).dni()).isEqualTo("30123456");
    }

    @Test
    void buscar_dniInvalido_lanzaSolicitudInvalida() {
        assertThatThrownBy(() -> servicio.buscar("12ab", null, 0, 20))
                .isInstanceOfSatisfying(SolicitudInvalidaException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("CRITERIO_DE_BUSQUEDA_INVALIDO"));
    }

    @Test
    void buscar_nombreDeUnaLetra_lanzaSolicitudInvalida() {
        assertThatThrownBy(() -> servicio.buscar(null, "a", 0, 20)).isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void buscar_conDniYNombre_mandaElDni() {
        when(repositorio.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

        // El nombre de una sola letra sería inválido, pero como viene el DNI no se mira
        servicio.buscar("30123456", "a", 0, 20);

        verify(repositorio).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void buscar_sinCriterio_listaTodosOrdenadosPorApellidoYNombre() {
        when(repositorio.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

        servicio.buscar(null, " ", 2, 10);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repositorio).findAll(any(Specification.class), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(10);
        assertThat(pageable.getValue().getSort().stream().map(o -> o.getProperty()))
                .containsExactly("apellido", "nombre", "id");
    }

    @Test
    @SuppressWarnings("unchecked")
    void buscar_elTamanioDePaginaTieneTope() {
        when(repositorio.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

        servicio.buscar(null, null, 0, 1000);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repositorio).findAll(any(Specification.class), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(50);
    }

    // ---------- CU02 · modificación ----------

    @Test
    void modificar_soloCambiaLosCamposEnviados() {
        Socio socio = socioExistente(12L);
        socio.setCodigoArea("11");
        socio.setNumeroTelefono("11111111");
        when(repositorio.findById(12L)).thenReturn(Optional.of(socio));

        servicio.modificar(12L, new PersonaUpdateRequest(null, null, null, null, null, null, null, "55556666", null), EMPLEADO);

        assertThat(socio.getNumeroTelefono()).isEqualTo("55556666");
        assertThat(socio.getCodigoArea()).isEqualTo("11");
        assertThat(socio.getNombre()).isEqualTo("Ana");
        assertThat(socio.getDni()).isEqualTo("30123456");
    }

    @Test
    void modificar_dniDeOtraPersona_lanzaConflicto() {
        when(repositorio.findById(12L)).thenReturn(Optional.of(socioExistente(12L)));
        doThrow(new ConflictoException(CodigoError.PERSONA_DNI_DUPLICADO, "Ya existe una persona con ese DNI"))
                .when(directorio).asegurarDniLibre("40999888");

        assertThatThrownBy(() -> servicio.modificar(12L,
                new PersonaUpdateRequest(null, null, "40999888", null, null, null, null, null, null), EMPLEADO))
                .isInstanceOfSatisfying(ConflictoException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("PERSONA_DNI_DUPLICADO"));
    }

    @Test
    void modificar_conSuPropioDni_noChocaConsigoMismo() {
        when(repositorio.findById(12L)).thenReturn(Optional.of(socioExistente(12L)));

        servicio.modificar(12L, new PersonaUpdateRequest(null, null, "30123456", null, null, null, null, null, null), EMPLEADO);

        verify(directorio, never()).asegurarDniLibre(any());
    }

    @Test
    void modificar_emailDeOtraPersona_lanzaConflicto() {
        when(repositorio.findById(12L)).thenReturn(Optional.of(socioExistente(12L)));
        doThrow(new ConflictoException(CodigoError.PERSONA_EMAIL_DUPLICADO, "Ya existe una persona con ese correo"))
                .when(directorio).asegurarEmailLibre("otro@correo.com");

        assertThatThrownBy(() -> servicio.modificar(12L,
                new PersonaUpdateRequest(null, null, null, null, null, null, null, null, "Otro@Correo.com"), EMPLEADO))
                .isInstanceOfSatisfying(ConflictoException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("PERSONA_EMAIL_DUPLICADO"));
    }

    @Test
    void modificar_inexistente_lanzaNoEncontrado() {
        when(repositorio.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.modificar(99L,
                new PersonaUpdateRequest("Ana", null, null, null, null, null, null, null, null), EMPLEADO))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    // ---------- CU04 · eliminación ----------

    @Test
    void eliminar_borraALaPersonaYAvisaAntesParaCerrarSusSesiones() {
        Socio socio = socioExistente(12L);
        when(repositorio.findById(12L)).thenReturn(Optional.of(socio));

        servicio.eliminar(12L, EMPLEADO);

        InOrder orden = inOrder(eventos, repositorio);
        orden.verify(eventos).publishEvent(new PersonaEliminadaEvent(12L));
        orden.verify(repositorio).delete(socio);
        orden.verify(repositorio).flush();
    }

    @Test
    void eliminar_inexistente_lanzaNoEncontrado() {
        when(repositorio.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.eliminar(99L, EMPLEADO)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void eliminar_laPropiaCuenta_seRechaza() {
        when(repositorio.findById(2L)).thenReturn(Optional.of(socioExistente(2L)));

        assertThatThrownBy(() -> servicio.eliminar(2L, EMPLEADO))
                .isInstanceOfSatisfying(ReglaDeNegocioException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("PERSONA_ELIMINAR_PROPIA_CUENTA"));
        verify(repositorio, never()).delete(any(Socio.class));
    }

    @Test
    void eliminar_conHistorial_lanzaConflictoYSugiereDeshabilitar() {
        when(repositorio.findById(12L)).thenReturn(Optional.of(socioExistente(12L)));
        doThrow(new DataIntegrityViolationException("fk")).when(repositorio).flush();

        assertThatThrownBy(() -> servicio.eliminar(12L, EMPLEADO))
                .isInstanceOfSatisfying(ConflictoException.class, e -> {
                    assertThat(e.getCodigo()).isEqualTo("PERSONA_CON_HISTORIAL");
                    assertThat(e.getMessage()).contains("Deshabilitala");
                });
    }

    // ---------- CU05 · impresión ----------

    @Test
    @SuppressWarnings("unchecked")
    void imprimir_armaLaFichaSinLaContrasenia() {
        Socio socio = socioExistente(12L);
        socio.setPasswordHash("$2a$10$hash-secreto");
        socio.setNumeroSocio("S-0012");
        when(repositorio.findById(12L)).thenReturn(Optional.of(socio));
        when(pdfService.generarFicha(anyString(), any())).thenReturn(new byte[]{1, 2, 3});

        byte[] pdf = servicio.imprimir(12L);

        assertThat(pdf).containsExactly(1, 2, 3);
        ArgumentCaptor<List<CampoFicha>> campos = ArgumentCaptor.forClass(List.class);
        verify(pdfService).generarFicha(eq("Ficha de socio"), campos.capture());
        assertThat(campos.getValue()).extracting(CampoFicha::valor).contains("Ana", "30123456", "S-0012");
        assertThat(campos.getValue()).extracting(CampoFicha::valor).noneMatch(v -> v != null && v.contains("hash"));
    }

    @Test
    void imprimir_inexistente_lanzaNoEncontrado() {
        when(repositorio.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.imprimir(99L)).isInstanceOf(RecursoNoEncontradoException.class);
        verify(pdfService, never()).generarFicha(any(), any());
    }
}
