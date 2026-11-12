package com.tenisahora.modules.torneo.service;

import java.util.ArrayList;
import java.util.List;

/**
 * Arma el calendario de una liga: todos contra todos, una sola vez (CU48 adaptado a la liga).
 *
 * <p>Usa el método del círculo, que es el estándar para un round robin: se fija al primer participante y se rota
 * el resto en cada fecha. Garantiza que cada uno juegue exactamente una vez con cada otro y, como mucho, un
 * partido por fecha.
 *
 * <p>Con una cantidad impar se agrega un lugar vacío, y el que queda emparejado con él **descansa** esa fecha. Ese
 * cruce no se devuelve: en el calendario simplemente hay un partido menos.
 *
 * <p>Es lógica pura, sin entidades ni base de datos, para poder probarla sola.
 */
public final class CalendarioDeLiga {

    /** Un cruce del calendario: quiénes juegan, en qué fecha y en qué orden dentro de esa fecha. */
    public record Cruce(int fecha, int orden, int localIndice, int visitanteIndice) {
    }

    private static final int DESCANSA = -1;

    private CalendarioDeLiga() {
    }

    /**
     * @param cantidad cuántos participantes hay
     * @return los cruces, ordenados por fecha y después por orden
     */
    public static List<Cruce> armar(int cantidad) {
        if (cantidad < 2) {
            return List.of();
        }
        // Con cantidad impar entra un lugar vacío: el que le toca ese lugar descansa esa fecha
        int lugares = cantidad % 2 == 0 ? cantidad : cantidad + 1;
        int[] rueda = ruedaInicial(lugares, cantidad);

        List<Cruce> cruces = new ArrayList<>();
        int fechas = lugares - 1;
        for (int fecha = 1; fecha <= fechas; fecha++) {
            agregarCrucesDeLaFecha(cruces, rueda, fecha);
            rotar(rueda);
        }
        return cruces;
    }

    /** Cuántas fechas tiene la liga, contando las de descanso. */
    public static int fechas(int cantidad) {
        if (cantidad < 2) {
            return 0;
        }
        return (cantidad % 2 == 0 ? cantidad : cantidad + 1) - 1;
    }

    /** Cuántos partidos se juegan en total: cada uno contra cada uno, una vez. */
    public static int partidos(int cantidad) {
        return cantidad < 2 ? 0 : cantidad * (cantidad - 1) / 2;
    }

    private static int[] ruedaInicial(int lugares, int cantidad) {
        int[] rueda = new int[lugares];
        for (int i = 0; i < lugares; i++) {
            rueda[i] = i < cantidad ? i : DESCANSA;
        }
        return rueda;
    }

    private static void agregarCrucesDeLaFecha(List<Cruce> cruces, int[] rueda, int fecha) {
        int lugares = rueda.length;
        int orden = 1;
        for (int i = 0; i < lugares / 2; i++) {
            int local = rueda[i];
            int visitante = rueda[lugares - 1 - i];
            if (local == DESCANSA || visitante == DESCANSA) {
                continue;
            }
            // Se alterna quién figura primero para que nadie sea siempre local
            boolean inverso = fecha % 2 == 0;
            cruces.add(new Cruce(fecha, orden++, inverso ? visitante : local, inverso ? local : visitante));
        }
    }

    /** El primer lugar queda fijo y los demás giran una posición. */
    private static void rotar(int[] rueda) {
        int ultimo = rueda[rueda.length - 1];
        System.arraycopy(rueda, 1, rueda, 2, rueda.length - 2);
        rueda[1] = ultimo;
    }
}
