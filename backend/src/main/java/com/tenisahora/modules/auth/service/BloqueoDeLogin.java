package com.tenisahora.modules.auth.service;

import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.DemasiadosIntentosException;
import com.tenisahora.shared.util.HashUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bloqueo del ingreso tras varios intentos fallidos seguidos (CU71 A3 / RF71): por defecto 5 intentos y 15 minutos.
 *
 * <p>Se cuenta por correo, exista o no la cuenta, para que el bloqueo no revele qué cuentas existen. El intento se
 * cuenta antes de verificar la contraseña: así, aunque lleguen muchos pedidos a la vez, solo los primeros llegan a
 * verificarla. Vive en memoria (una sola instancia del backend).
 */
@Component
public class BloqueoDeLogin {

    static final int MAXIMO_DE_CUENTAS = 100_000;

    /** Los intentos de una cuenta y cuándo fue el último que se contó. */
    private static final class Estado {
        int intentos;
        Instant ultimoIntento;
    }

    private final Clock reloj;
    private final int maximoDeIntentos;
    private final Duration duracionDelBloqueo;
    private final ConcurrentHashMap<String, Estado> estados = new ConcurrentHashMap<>();

    public BloqueoDeLogin(Clock reloj,
                          @Value("${app.login.max-intentos-fallidos:5}") int maximoDeIntentos,
                          @Value("${app.login.bloqueo-minutos:15}") int minutosDeBloqueo) {
        this.reloj = reloj;
        this.maximoDeIntentos = maximoDeIntentos;
        this.duracionDelBloqueo = Duration.ofMinutes(minutosDeBloqueo);
    }

    /** Misma clave para "Ana@Correo.com " y "ana@correo.com". */
    public static String claveDe(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    /** Código corto para los logs: sirve para seguir una cuenta sin escribir su correo. */
    public static String huellaDe(String clave) {
        return HashUtils.sha256Hex(clave).substring(0, 8);
    }

    public int maximoDeIntentos() {
        return maximoDeIntentos;
    }

    public long minutosDeBloqueo() {
        return duracionDelBloqueo.toMinutes();
    }

    /**
     * Empieza un intento de ingreso y lo cuenta.
     *
     * @return el número de intento (1, 2, 3...).
     * @throws DemasiadosIntentosException si la cuenta está bloqueada.
     */
    public int iniciarIntento(String clave) {
        if (sinLugarPara(clave)) {
            return 1; // sin lugar para contar: se deja pasar antes que tirar abajo el servicio
        }
        Instant ahora = reloj.instant();
        Estado estado = estados.computeIfAbsent(clave, k -> new Estado());
        synchronized (estado) {
            reiniciarSiPasoElTiempo(estado, ahora);
            estado.intentos++;
            estado.ultimoIntento = ahora;
            return estado.intentos;
        }
    }

    /** El ingreso salió bien: se olvidan los intentos fallidos. */
    public void intentoExitoso(String clave) {
        estados.remove(clave);
    }

    /** El intento no llegó a verificarse (por ejemplo, falló la base): no tiene que contar contra la persona. */
    public void descontarIntento(String clave) {
        Estado estado = estados.get(clave);
        if (estado == null) {
            return;
        }
        synchronized (estado) {
            estado.intentos = Math.max(0, estado.intentos - 1);
        }
    }

    /** Borra las cuentas cuyo último intento ya quedó fuera del período de bloqueo. */
    public void purgar() {
        Instant ahora = reloj.instant();
        estados.entrySet().removeIf(entrada -> {
            Estado estado = entrada.getValue();
            synchronized (estado) {
                return estado.ultimoIntento == null || !ahora.isBefore(libreDesde(estado));
            }
        });
    }

    int cantidadDeCuentas() {
        return estados.size();
    }

    /**
     * Si la cuenta está bloqueada, corta. Si el período ya pasó (con o sin bloqueo), empieza de cero: los fallos viejos
     * no se acumulan para siempre.
     */
    private void reiniciarSiPasoElTiempo(Estado estado, Instant ahora) {
        if (estado.intentos == 0) {
            return;
        }
        Instant libreDesde = libreDesde(estado);
        if (estado.intentos >= maximoDeIntentos && ahora.isBefore(libreDesde)) {
            throw bloqueada(Duration.between(ahora, libreDesde));
        }
        if (!ahora.isBefore(libreDesde)) {
            estado.intentos = 0;
        }
    }

    private Instant libreDesde(Estado estado) {
        return estado.ultimoIntento.plus(duracionDelBloqueo);
    }

    private boolean sinLugarPara(String clave) {
        if (estados.size() < MAXIMO_DE_CUENTAS || estados.containsKey(clave)) {
            return false;
        }
        purgar();
        return estados.size() >= MAXIMO_DE_CUENTAS;
    }

    private DemasiadosIntentosException bloqueada(Duration espera) {
        long minutos = (espera.toSeconds() + 59) / 60;
        return new DemasiadosIntentosException(CodigoError.LOGIN_BLOQUEADO,
                "Demasiados intentos fallidos. Esperá " + minutos + " minuto(s) antes de volver a intentar.", espera);
    }
}
