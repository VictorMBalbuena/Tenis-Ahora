package com.tenisahora.shared.cuenta;

import com.tenisahora.shared.exception.ConflictoException;
import com.tenisahora.shared.model.Rol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DirectorioDeCuentasTest {

    @Mock
    private RegistroDeCuentas socios;
    @Mock
    private RegistroDeCuentas empleados;

    private DirectorioDeCuentas directorio;

    @BeforeEach
    void setUp() {
        directorio = new DirectorioDeCuentas(List.of(socios, empleados));
        lenient().when(socios.atiende(Rol.SOCIO)).thenReturn(true);
        lenient().when(empleados.atiende(Rol.EMPLEADO)).thenReturn(true);
    }

    private CuentaDeAcceso cuenta(Long id, Rol rol) {
        return new CuentaDeAcceso(id, "a@tenisahora.com", "hash", rol, true);
    }

    @Test
    void porEmail_recorreLosModulosHastaEncontrarla() {
        when(socios.porEmail("a@tenisahora.com")).thenReturn(Optional.empty());
        when(empleados.porEmail("a@tenisahora.com")).thenReturn(Optional.of(cuenta(5L, Rol.EMPLEADO)));

        assertThat(directorio.porEmail("a@tenisahora.com")).contains(cuenta(5L, Rol.EMPLEADO));
    }

    @Test
    void porEmail_sinCoincidencia_devuelveVacio() {
        when(socios.porEmail("x@tenisahora.com")).thenReturn(Optional.empty());
        when(empleados.porEmail("x@tenisahora.com")).thenReturn(Optional.empty());

        assertThat(directorio.porEmail("x@tenisahora.com")).isEmpty();
    }

    @Test
    void porId_consultaSoloElModuloDelRol() {
        when(empleados.porId(5L)).thenReturn(Optional.of(cuenta(5L, Rol.EMPLEADO)));

        assertThat(directorio.porId(5L, Rol.EMPLEADO)).contains(cuenta(5L, Rol.EMPLEADO));
        verify(socios, never()).porId(5L);
    }

    @Test
    void porId_siLaCuentaEsDeOtroRol_noLaDevuelve() {
        when(socios.porId(5L)).thenReturn(Optional.of(cuenta(5L, Rol.EMPLEADO)));

        assertThat(directorio.porId(5L, Rol.SOCIO)).isEmpty();
    }

    @Test
    void asegurarDniLibre_siOtroRolYaLoTiene_lanzaConflicto() {
        when(socios.existeDni("30123456")).thenReturn(false);
        when(empleados.existeDni("30123456")).thenReturn(true);

        assertThatThrownBy(() -> directorio.asegurarDniLibre("30123456"))
                .isInstanceOfSatisfying(ConflictoException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("PERSONA_DNI_DUPLICADO"));
    }

    @Test
    void asegurarEmailLibre_siOtroRolYaLoTiene_lanzaConflicto() {
        when(socios.existeEmail("a@tenisahora.com")).thenReturn(true);

        assertThatThrownBy(() -> directorio.asegurarEmailLibre("a@tenisahora.com"))
                .isInstanceOfSatisfying(ConflictoException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("PERSONA_EMAIL_DUPLICADO"));
    }

    @Test
    void asegurarDniYEmailLibres_sinCoincidencias_noLanzaNada() {
        directorio.asegurarDniLibre("30123456");
        directorio.asegurarEmailLibre("libre@tenisahora.com");
    }
}
