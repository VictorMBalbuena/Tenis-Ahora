package com.tenisahora.modules.administrador.service;

import com.tenisahora.infrastructure.pdf.PdfService;
import com.tenisahora.modules.administrador.entity.Administrador;
import com.tenisahora.modules.administrador.repository.AdministradorRepository;
import com.tenisahora.shared.cuenta.DirectorioDeCuentas;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.shared.dto.PersonaRequest;
import com.tenisahora.shared.dto.PersonaResponse;
import com.tenisahora.shared.dto.PersonaUpdateRequest;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import com.tenisahora.shared.model.Rol;
import com.tenisahora.shared.persona.MapeoDePersona;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Lo que distingue a los administradores: solo otro administrador los gestiona y el club no puede quedarse sin ninguno. */
@ExtendWith(MockitoExtension.class)
class AdministradorServiceTest {

    private static final Actor EMPLEADO = new Actor(2L, Rol.EMPLEADO);
    private static final Actor ADMINISTRADOR = new Actor(1L, Rol.ADMINISTRADOR);

    @Mock
    private AdministradorRepository repositorio;
    @Mock
    private DirectorioDeCuentas directorio;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ApplicationEventPublisher eventos;
    @Mock
    private PdfService pdfService;

    private AdministradorService servicio;

    @BeforeEach
    void setUp() {
        servicio = new AdministradorService(repositorio, new MapeoDePersona(), directorio, passwordEncoder, eventos, pdfService);
        lenient().when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hash-de-prueba");
        lenient().when(repositorio.saveAndFlush(any(Administrador.class))).thenAnswer(invocacion -> {
            Administrador administrador = invocacion.getArgument(0);
            administrador.setId(12L);
            return administrador;
        });
    }

    private PersonaRequest altaValida() {
        return new PersonaRequest("Ana", "Gómez", "30123456", null, "Av. Siempreviva",
                "742", "11", "12345678", "ana@correo.com", "Clave-segura-123");
    }

    private Administrador administradorExistente(Long id) {
        Administrador administrador = new Administrador();
        administrador.setId(id);
        administrador.setActivo(true);
        return administrador;
    }

    @Test
    void registrar_loPuedeHacerUnAdministrador() {
        PersonaResponse respuesta = servicio.registrar(altaValida(), ADMINISTRADOR);

        assertThat(respuesta.rol()).isEqualTo(Rol.ADMINISTRADOR);
    }

    @Test
    void registrar_unEmpleadoNoPuede() {
        assertThatThrownBy(() -> servicio.registrar(altaValida(), EMPLEADO)).isInstanceOf(AccessDeniedException.class);
        verify(repositorio, never()).saveAndFlush(any());
    }

    @Test
    void modificar_unEmpleadoNoPuede() {
        assertThatThrownBy(() -> servicio.modificar(1L,
                new PersonaUpdateRequest("Otro", null, null, null, null, null, null, null, null), EMPLEADO))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void modificar_otroAdministradorSiPuede() {
        Administrador otro = administradorExistente(5L);
        otro.setNombre("Viejo");
        when(repositorio.findById(5L)).thenReturn(Optional.of(otro));

        servicio.modificar(5L, new PersonaUpdateRequest("Nuevo", null, null, null, null, null, null, null, null), ADMINISTRADOR);

        assertThat(otro.getNombre()).isEqualTo("Nuevo");
    }

    @Test
    void eliminar_alUnicoAdministrador_seRechaza() {
        when(repositorio.findById(5L)).thenReturn(Optional.of(administradorExistente(5L)));
        when(repositorio.countByActivoTrue()).thenReturn(1L);

        assertThatThrownBy(() -> servicio.eliminar(5L, ADMINISTRADOR))
                .isInstanceOfSatisfying(ReglaDeNegocioException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("PERSONA_ULTIMO_ADMINISTRADOR"));
        verify(repositorio, never()).delete(any(Administrador.class));
    }

    @Test
    void eliminar_habiendoOtros_sePermite() {
        Administrador administrador = administradorExistente(5L);
        when(repositorio.findById(5L)).thenReturn(Optional.of(administrador));
        when(repositorio.countByActivoTrue()).thenReturn(2L);

        servicio.eliminar(5L, ADMINISTRADOR);

        verify(repositorio).delete(administrador);
    }

    @Test
    void eliminar_laPropiaCuenta_seRechaza() {
        when(repositorio.findById(1L)).thenReturn(Optional.of(administradorExistente(1L)));

        assertThatThrownBy(() -> servicio.eliminar(1L, ADMINISTRADOR))
                .isInstanceOfSatisfying(ReglaDeNegocioException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("PERSONA_ELIMINAR_PROPIA_CUENTA"));
    }

    @Test
    void eliminar_unEmpleadoNoPuede() {
        assertThatThrownBy(() -> servicio.eliminar(5L, EMPLEADO)).isInstanceOf(AccessDeniedException.class);
        verify(repositorio, never()).delete(any(Administrador.class));
    }
}
