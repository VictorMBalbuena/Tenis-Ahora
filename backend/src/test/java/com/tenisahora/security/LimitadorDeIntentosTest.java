package com.tenisahora.security;

import com.tenisahora.support.RelojFalso;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class LimitadorDeIntentosTest {

    private static final Duration VENTANA = Duration.ofMinutes(15);

    private RelojFalso reloj;
    private LimitadorDeIntentos limitador;

    @BeforeEach
    void setUp() {
        reloj = new RelojFalso(Instant.parse("2026-10-10T12:00:00Z"));
        limitador = new LimitadorDeIntentos(reloj);
    }

    @Test
    void permiteHastaElMaximo() {
        for (int i = 0; i < 3; i++) {
            assertThat(limitador.intentar("login|1.1.1.1", 3, VENTANA)).isEmpty();
        }
    }

    @Test
    void elPedidoQueSuperaElMaximo_sedeniegaConLaEspera() {
        for (int i = 0; i < 3; i++) {
            limitador.intentar("login|1.1.1.1", 3, VENTANA);
        }

        assertThat(limitador.intentar("login|1.1.1.1", 3, VENTANA)).contains(Duration.ofMinutes(15));
    }

    @Test
    void laEsperaVaBajandoConElTiempo() {
        for (int i = 0; i < 3; i++) {
            limitador.intentar("login|1.1.1.1", 3, VENTANA);
        }
        reloj.avanzar(Duration.ofMinutes(10));

        assertThat(limitador.intentar("login|1.1.1.1", 3, VENTANA)).contains(Duration.ofMinutes(5));
    }

    @Test
    void laVentanaSeVaCorriendo_yLiberaLugares() {
        limitador.intentar("login|1.1.1.1", 2, VENTANA);
        reloj.avanzar(Duration.ofMinutes(10));
        limitador.intentar("login|1.1.1.1", 2, VENTANA);
        assertThat(limitador.intentar("login|1.1.1.1", 2, VENTANA)).isPresent();

        // Pasan 5 minutos más: el primer pedido sale de la ventana y se libera un lugar
        reloj.avanzar(Duration.ofMinutes(5));

        assertThat(limitador.intentar("login|1.1.1.1", 2, VENTANA)).isEmpty();
    }

    @Test
    void cadaClaveTieneSuPropioContador() {
        limitador.intentar("login|1.1.1.1", 1, VENTANA);

        assertThat(limitador.intentar("login|2.2.2.2", 1, VENTANA)).isEmpty();
        assertThat(limitador.intentar("registro|1.1.1.1", 1, VENTANA)).isEmpty();
    }

    @Test
    void purgar_borraLasClavesSinPedidosVigentes() {
        limitador.intentar("login|1.1.1.1", 5, VENTANA);
        reloj.avanzar(Duration.ofMinutes(16));
        limitador.intentar("login|2.2.2.2", 5, VENTANA);

        limitador.purgar();

        assertThat(limitador.cantidadDeClaves()).isEqualTo(1);
    }

    @Test
    void sinLugarParaContarClavesNuevas_dejaPasarEnVezDeTirarAbajoElServicio() {
        for (int i = 0; i < LimitadorDeIntentos.MAXIMO_DE_CLAVES; i++) {
            limitador.intentar("login|ip" + i, 5, VENTANA);
        }

        assertThat(limitador.intentar("login|una-mas", 1, VENTANA)).isEmpty();
        assertThat(limitador.intentar("login|una-mas", 1, VENTANA)).isEmpty();
        assertThat(limitador.cantidadDeClaves()).isEqualTo(LimitadorDeIntentos.MAXIMO_DE_CLAVES);
    }
}
