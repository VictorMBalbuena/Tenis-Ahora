package com.tenisahora.modules.permiso.model;

import com.tenisahora.modules.permiso.dto.MatrizDePermisos;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** La traducción entre lo que muestra la pantalla y lo que se guarda (CU75). */
class MatrizDePermisosTest {

    @Test
    void laMatrizVaYVuelveSinPerderNada() {
        MatrizDePermisos original = new MatrizDePermisos(
                Map.of(ModuloDelSistema.COBROS, Set.of(AccionSobreModulo.VER, AccionSobreModulo.CREAR),
                        ModuloDelSistema.RESERVAS, Set.of(AccionSobreModulo.VER)),
                Set.of(PermisoEspecial.ANULAR_COBROS));

        MatrizDePermisos vuelta = MatrizDePermisos.deCodigos(original.aCodigos());

        assertThat(vuelta.modulos().get(ModuloDelSistema.COBROS))
                .containsExactlyInAnyOrder(AccionSobreModulo.VER, AccionSobreModulo.CREAR);
        assertThat(vuelta.modulos().get(ModuloDelSistema.RESERVAS)).containsExactly(AccionSobreModulo.VER);
        assertThat(vuelta.especiales()).containsExactly(PermisoEspecial.ANULAR_COBROS);
    }

    @Test
    void unCodigoViejoQueYaNoExisteSeIgnoraEnVezDeRomper() {
        MatrizDePermisos matriz = MatrizDePermisos.deCodigos(
                Set.of("MODULO_BORRADO:VER", "ESPECIAL:PERMISO_BORRADO", "COBROS:VER"));

        assertThat(matriz.modulos()).containsOnlyKeys(ModuloDelSistema.COBROS);
        assertThat(matriz.especiales()).isEmpty();
    }

    @Test
    void unaMatrizVaciaNoEsNula() {
        MatrizDePermisos matriz = new MatrizDePermisos(null, null);

        assertThat(matriz.modulos()).isEmpty();
        assertThat(matriz.especiales()).isEmpty();
        assertThat(matriz.aCodigos()).isEmpty();
    }

    @Test
    void cadaModuloReconoceSusRutas() {
        assertThat(ModuloDelSistema.COBROS.cubre("/api/cobros")).isTrue();
        assertThat(ModuloDelSistema.COBROS.cubre("/api/cobros/7/pdf")).isTrue();
        // No alcanza con que empiece igual: /api/cobrosx no es del módulo de cobros
        assertThat(ModuloDelSistema.COBROS.cubre("/api/cobrosx")).isFalse();
        assertThat(ModuloDelSistema.COBROS.cubre("/api/recibos")).isFalse();
    }

    @Test
    void elMetodoHttpDiceQueAccionEs() {
        assertThat(AccionSobreModulo.deMetodo("GET")).isEqualTo(AccionSobreModulo.VER);
        assertThat(AccionSobreModulo.deMetodo("POST")).isEqualTo(AccionSobreModulo.CREAR);
        assertThat(AccionSobreModulo.deMetodo("PATCH")).isEqualTo(AccionSobreModulo.EDITAR);
        assertThat(AccionSobreModulo.deMetodo("PUT")).isEqualTo(AccionSobreModulo.EDITAR);
        assertThat(AccionSobreModulo.deMetodo("DELETE")).isEqualTo(AccionSobreModulo.ELIMINAR);
    }

    @Test
    void losPermisosEspecialesReconocenElPedidoQueLosEjerce() {
        assertThat(PermisoEspecial.VALIDAR_CERTIFICACIONES.loEjerce("POST", "/api/profesores/3/validacion")).isTrue();
        assertThat(PermisoEspecial.VALIDAR_CERTIFICACIONES.loEjerce("GET", "/api/profesores/3")).isFalse();

        assertThat(PermisoEspecial.ANULAR_COBROS.loEjerce("DELETE", "/api/cobros/7")).isTrue();
        assertThat(PermisoEspecial.ANULAR_COBROS.loEjerce("GET", "/api/cobros/7")).isFalse();

        assertThat(PermisoEspecial.BLOQUEAR_CANCHAS.loEjerce("POST", "/api/canchas/2/bloqueos")).isTrue();
        // Mirar los bloqueos no es bloquear
        assertThat(PermisoEspecial.BLOQUEAR_CANCHAS.loEjerce("GET", "/api/canchas/2/bloqueos")).isFalse();

        assertThat(PermisoEspecial.CONFIGURAR_DESCUENTOS.loEjerce("POST", "/api/descuentos")).isTrue();
        assertThat(PermisoEspecial.GESTIONAR_USUARIOS.loEjerce("PUT", "/api/cuentas/4/rol")).isTrue();
    }
}
