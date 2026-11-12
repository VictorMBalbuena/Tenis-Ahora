package com.tenisahora.modules.permiso.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/** Qué rutas controla cada módulo. De esto depende a quién le corta el paso el filtro de permisos. */
class ModuloDelSistemaTest {

    /** El módulo que reclama esa ruta, o vacío si no la reclama ninguno. */
    private static String moduloDe(String ruta) {
        return Arrays.stream(ModuloDelSistema.values())
                .filter(m -> m.cubre(ruta))
                .map(Enum::name)
                .reduce((a, b) -> a + "+" + b)
                .orElse("");
    }

    @Test
    @DisplayName("Cada módulo cubre su ruta y las que cuelgan de ella")
    void cubreSuPropiaRuta() {
        assertThat(moduloDe("/api/descuentos")).isEqualTo("DESCUENTOS");
        assertThat(moduloDe("/api/descuentos/5")).isEqualTo("DESCUENTOS");
        assertThat(moduloDe("/api/reservas/9/insumos")).isEqualTo("RESERVAS");
    }

    @Test
    @DisplayName("Validar un código de descuento no es administrar descuentos")
    void validarUnCodigoNoLoCubreNingunModulo() {
        // Si lo cubriera DESCUENTOS, un socio no podría usar su código al reservar: no tiene
        // permiso sobre ese módulo. La seguridad de esa ruta la pone su propio controlador.
        assertThat(moduloDe("/api/descuentos/codigo")).isEmpty();
        assertThat(moduloDe("/api/descuentos/codigo/CYBERWEEK")).isEmpty();
    }

    @Test
    @DisplayName("Una ruta que apenas se parece sigue siendo del módulo")
    void noSeEscapaPorParecido() {
        // "codigos" no es "codigo": no entra por la excepción.
        assertThat(moduloDe("/api/descuentos/codigos")).isEqualTo("DESCUENTOS");
    }

    @Test
    @DisplayName("Una ruta de otro módulo no la cubre descuentos")
    void noSePisanEntreModulos() {
        assertThat(ModuloDelSistema.DESCUENTOS.cubre("/api/descuentos-viejos")).isFalse();
        assertThat(ModuloDelSistema.DESCUENTOS.cubre("/api/cobros")).isFalse();
    }

    @Test
    @DisplayName("Ninguna ruta cae en dos módulos a la vez")
    void sinSolapamientos() {
        for (ModuloDelSistema modulo : ModuloDelSistema.values()) {
            for (String ruta : new String[]{"/api/usuarios", "/api/canchas", "/api/reservas", "/api/insumos",
                    "/api/profesores", "/api/entrenadores", "/api/clases", "/api/entrenamientos",
                    "/api/actividades", "/api/torneos", "/api/cobros", "/api/recibos", "/api/descuentos",
                    "/api/reportes", "/api/roles"}) {
                if (modulo.cubre(ruta)) {
                    assertThat(moduloDe(ruta)).doesNotContain("+");
                }
            }
        }
    }

    @Test
    @DisplayName("Pagar es del modulo Cobros, cuelgue de donde cuelgue")
    void lasRutasDePagoSonDeCobros() {
        // Si salieran del prefijo, apagar "Cobros" no impediria ningun pago y para dejar pagar
        // una clase habria que tocar "Actividades".
        assertThat(moduloDe("/api/reservas/5/cobros")).isEqualTo("COBROS");
        assertThat(moduloDe("/api/actividades/inscripciones/4/cobros")).isEqualTo("COBROS");
        assertThat(moduloDe("/api/torneos/inscripciones/7/cobros")).isEqualTo("COBROS");
        assertThat(moduloDe("/api/cobros")).isEqualTo("COBROS");
        assertThat(moduloDe("/api/cobros/9/qr")).isEqualTo("COBROS");
    }

    @Test
    @DisplayName("Lo que no es un pago sigue siendo de su modulo")
    void elRestoNoSeMueve() {
        assertThat(moduloDe("/api/reservas/5")).isEqualTo("RESERVAS");
        assertThat(moduloDe("/api/actividades/inscripciones/4")).isEqualTo("ACTIVIDADES");
        assertThat(moduloDe("/api/torneos/inscripciones/7")).isEqualTo("TORNEOS");
        // "cobrosx" no es "cobros": no se escapa por parecido
        assertThat(moduloDe("/api/reservas/5/cobrosx")).isEqualTo("RESERVAS");
    }
}
