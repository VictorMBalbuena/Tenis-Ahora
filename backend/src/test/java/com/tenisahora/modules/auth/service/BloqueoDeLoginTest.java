package com.tenisahora.modules.auth.service;

import com.tenisahora.shared.exception.DemasiadosIntentosException;
import com.tenisahora.support.RelojFalso;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BloqueoDeLoginTest {

    private static final String CUENTA = "socio@tenisahora.com";

    private RelojFalso reloj;
    private BloqueoDeLogin bloqueo;

    @BeforeEach
    void setUp() {
        reloj = new RelojFalso(Instant.parse("2026-10-10T12:00:00Z"));
        bloqueo = new BloqueoDeLogin(reloj, 5, 15);
    }

    private void hacerIntentos(String cuenta, int cantidad) {
        for (int i = 0; i < cantidad; i++) {
            bloqueo.iniciarIntento(cuenta);
        }
    }

    @Test
    void losPrimeros5Intentos_sePermitenYSeNumeran() {
        for (int esperado = 1; esperado <= 5; esperado++) {
            assertThat(bloqueo.iniciarIntento(CUENTA)).isEqualTo(esperado);
        }
    }

    @Test
    void elSextoIntento_estaBloqueadoConElCodigoYLaEspera() {
        hacerIntentos(CUENTA, 5);

        assertThatThrownBy(() -> bloqueo.iniciarIntento(CUENTA))
                .isInstanceOfSatisfying(DemasiadosIntentosException.class, e -> {
                    assertThat(e.getCodigo()).isEqualTo("LOGIN_BLOQUEADO");
                    assertThat(e.segundosDeEspera()).isEqualTo(900);
                    assertThat(e.getMessage()).contains("15 minuto");
                });
    }

    @Test
    void elBloqueoDura15Minutos() {
        hacerIntentos(CUENTA, 5);

        reloj.avanzar(Duration.ofMinutes(14).plusSeconds(59));
        assertThatThrownBy(() -> bloqueo.iniciarIntento(CUENTA)).isInstanceOf(DemasiadosIntentosException.class);

        reloj.avanzar(Duration.ofSeconds(1));
        assertThat(bloqueo.iniciarIntento(CUENTA)).isEqualTo(1);
    }

    @Test
    void insistirMientrasEstaBloqueada_noAlargaElBloqueo() {
        hacerIntentos(CUENTA, 5);

        reloj.avanzar(Duration.ofMinutes(10));
        assertThatThrownBy(() -> bloqueo.iniciarIntento(CUENTA)).isInstanceOf(DemasiadosIntentosException.class);

        reloj.avanzar(Duration.ofMinutes(5));
        assertThat(bloqueo.iniciarIntento(CUENTA)).isEqualTo(1);
    }

    @Test
    void laEsperaQueSeInformaVaBajando() {
        hacerIntentos(CUENTA, 5);
        reloj.avanzar(Duration.ofMinutes(10));

        assertThatThrownBy(() -> bloqueo.iniciarIntento(CUENTA))
                .isInstanceOfSatisfying(DemasiadosIntentosException.class,
                        e -> assertThat(e.segundosDeEspera()).isEqualTo(300));
    }

    @Test
    void unIngresoExitoso_reiniciaElContador() {
        hacerIntentos(CUENTA, 4);
        bloqueo.intentoExitoso(CUENTA);

        for (int esperado = 1; esperado <= 5; esperado++) {
            assertThat(bloqueo.iniciarIntento(CUENTA)).isEqualTo(esperado);
        }
    }

    @Test
    void elQuintoIntentoPuedeSerExitoso_yNoBloquea() {
        hacerIntentos(CUENTA, 5);
        bloqueo.intentoExitoso(CUENTA);

        assertThat(bloqueo.iniciarIntento(CUENTA)).isEqualTo(1);
    }

    @Test
    void cadaCuentaTieneSuPropioContador() {
        hacerIntentos(CUENTA, 5);

        assertThat(bloqueo.iniciarIntento("empleado@tenisahora.com")).isEqualTo(1);
    }

    @Test
    void losFallosViejos_noSeAcumulanParaSiempre() {
        hacerIntentos(CUENTA, 3);
        reloj.avanzar(Duration.ofMinutes(16));

        for (int esperado = 1; esperado <= 5; esperado++) {
            assertThat(bloqueo.iniciarIntento(CUENTA)).isEqualTo(esperado);
        }
    }

    @Test
    void descontarUnIntento_lePermiteUnoMas() {
        hacerIntentos(CUENTA, 2);
        bloqueo.descontarIntento(CUENTA);

        assertThat(bloqueo.iniciarIntento(CUENTA)).isEqualTo(2);
    }

    @Test
    void descontarEnUnaCuentaSinIntentos_noFalla() {
        bloqueo.descontarIntento("nadie@tenisahora.com");

        assertThat(bloqueo.iniciarIntento("nadie@tenisahora.com")).isEqualTo(1);
    }

    @Test
    void conMuchosPedidosAlMismoTiempo_soloLosPrimeros5LlegaAVerificarLaContrasenia() throws Exception {
        int pedidos = 50;
        ExecutorService pool = Executors.newFixedThreadPool(pedidos);
        CountDownLatch salida = new CountDownLatch(1);
        AtomicInteger permitidos = new AtomicInteger();
        AtomicInteger bloqueados = new AtomicInteger();

        for (int i = 0; i < pedidos; i++) {
            pool.submit(() -> {
                salida.await();
                try {
                    bloqueo.iniciarIntento(CUENTA);
                    permitidos.incrementAndGet();
                } catch (DemasiadosIntentosException e) {
                    bloqueados.incrementAndGet();
                }
                return null;
            });
        }
        salida.countDown();
        pool.shutdown();
        pool.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS);

        assertThat(permitidos.get()).isEqualTo(5);
        assertThat(bloqueados.get()).isEqualTo(45);
    }

    @Test
    void purgar_borraLasCuentasVencidasYDejaLasRecientes() {
        hacerIntentos("vieja@tenisahora.com", 2);
        reloj.avanzar(Duration.ofMinutes(16));
        hacerIntentos("reciente@tenisahora.com", 2);

        bloqueo.purgar();

        assertThat(bloqueo.cantidadDeCuentas()).isEqualTo(1);
    }

    @Test
    void laClave_noDistingueMayusculasNiEspacios() {
        assertThat(BloqueoDeLogin.claveDe("  Socio@TenisAhora.COM ")).isEqualTo("socio@tenisahora.com");
    }

    @Test
    void laHuellaParaLosLogs_noContieneElCorreo() {
        String huella = BloqueoDeLogin.huellaDe(CUENTA);

        assertThat(huella).hasSize(8).matches("[0-9a-f]+");
        assertThat(huella).doesNotContain("socio").doesNotContain("@");
    }

    @Test
    void sinLugarParaContar_dejaPasarEnVezDeTirarAbajoElServicio() {
        for (int i = 0; i < BloqueoDeLogin.MAXIMO_DE_CUENTAS; i++) {
            bloqueo.iniciarIntento("cuenta" + i + "@x.com");
        }

        assertThat(bloqueo.iniciarIntento("una-mas@x.com")).isEqualTo(1);
        assertThat(bloqueo.cantidadDeCuentas()).isEqualTo(BloqueoDeLogin.MAXIMO_DE_CUENTAS);
    }
}
