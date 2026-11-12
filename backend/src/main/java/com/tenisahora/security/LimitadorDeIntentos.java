package com.tenisahora.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Límite de pedidos en una ventana de tiempo que se va corriendo ("ventana deslizante"), por clave (por ejemplo
 * "login|IP"). Vive en memoria: sirve para una sola instancia del backend. Para varias instancias habría que mover
 * el contador a un lugar compartido (Redis, la base).
 */
@Slf4j
@Component
public class LimitadorDeIntentos {

    /** Tope de claves guardadas: quien ataca controla las claves (IPs), así que la memoria no puede crecer sin límite. */
    static final int MAXIMO_DE_CLAVES = 100_000;

    /** Los pedidos recientes de una clave. */
    private static final class Registro {
        private final Deque<Instant> pedidos = new ArrayDeque<>();
        private Duration ventana;

        /** Cuenta un pedido. Devuelve vacío si entra, o cuánto falta para que se libere un lugar. */
        synchronized Optional<Duration> contar(Instant ahora, int maximo, Duration nuevaVentana) {
            ventana = nuevaVentana;
            descartarLosQueSalieronDeLaVentana(ahora);
            if (pedidos.size() >= maximo) {
                return Optional.of(Duration.between(ahora, pedidos.peekFirst().plus(ventana)));
            }
            pedidos.addLast(ahora);
            return Optional.empty();
        }

        synchronized boolean vencido(Instant ahora) {
            return pedidos.isEmpty() || !pedidos.peekLast().plus(ventana).isAfter(ahora);
        }

        private void descartarLosQueSalieronDeLaVentana(Instant ahora) {
            Instant desde = ahora.minus(ventana);
            while (!pedidos.isEmpty() && !pedidos.peekFirst().isAfter(desde)) {
                pedidos.pollFirst();
            }
        }
    }

    private final Clock reloj;
    private final ConcurrentHashMap<String, Registro> registros = new ConcurrentHashMap<>();

    public LimitadorDeIntentos(Clock reloj) {
        this.reloj = reloj;
    }

    /**
     * Cuenta un pedido de esta clave.
     *
     * @return vacío si se permite; si se superó el máximo, cuánto falta para que se libere un lugar.
     */
    public Optional<Duration> intentar(String clave, int maximo, Duration ventana) {
        if (sinLugarPara(clave)) {
            // Mejor dejar pasar que tirar abajo el servicio: se avisa para que alguien lo mire
            log.warn("Límite de pedidos: se alcanzó el tope de {} claves, no se cuenta este pedido", MAXIMO_DE_CLAVES);
            return Optional.empty();
        }
        return registros.computeIfAbsent(clave, k -> new Registro()).contar(reloj.instant(), maximo, ventana);
    }

    /** Borra las claves cuyos pedidos ya salieron todos de la ventana. */
    public void purgar() {
        Instant ahora = reloj.instant();
        registros.values().removeIf(registro -> registro.vencido(ahora));
    }

    int cantidadDeClaves() {
        return registros.size();
    }

    private boolean sinLugarPara(String clave) {
        if (registros.size() < MAXIMO_DE_CLAVES || registros.containsKey(clave)) {
            return false;
        }
        purgar();
        return registros.size() >= MAXIMO_DE_CLAVES;
    }
}
