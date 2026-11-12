package com.tenisahora.modules.torneo.service;

import com.tenisahora.modules.torneo.dto.PartidoResponse;
import com.tenisahora.modules.torneo.dto.PosicionResponse;
import com.tenisahora.modules.torneo.entity.Inscripcion;
import com.tenisahora.modules.torneo.entity.Partido;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Calcula la tabla de posiciones de una liga a partir de los partidos que ya tienen resultado.
 *
 * <p>Se ordena por partidos ganados, y si hay empate por diferencia de sets, después por diferencia de games y
 * por último por nombre, para que el orden sea siempre el mismo con los mismos datos. No se inventa ninguna
 * escala de puntos: en tenis no hay empate, así que los ganados son la puntuación.
 *
 * <p>Los participantes que todavía no jugaron aparecen igual, en cero: una tabla a la que le faltan los que
 * todavía no debutaron se lee mal.
 */
public final class TablaDePosiciones {

    private TablaDePosiciones() {
    }

    public static List<PosicionResponse> calcular(List<Inscripcion> inscriptos, List<Partido> partidos) {
        Map<Long, Acumulado> porInscripcion = new LinkedHashMap<>();
        inscriptos.forEach(i -> porInscripcion.put(i.getId(), new Acumulado(i)));

        partidos.stream().filter(TablaDePosiciones::seJugo).forEach(p -> sumar(porInscripcion, p));

        List<Acumulado> ordenados = new ArrayList<>(porInscripcion.values());
        ordenados.sort(ORDEN);

        List<PosicionResponse> tabla = new ArrayList<>();
        for (int i = 0; i < ordenados.size(); i++) {
            tabla.add(ordenados.get(i).aRespuesta(i + 1));
        }
        return tabla;
    }

    private static final Comparator<Acumulado> ORDEN = Comparator
            .comparingInt((Acumulado a) -> a.ganados).reversed()
            .thenComparing(Comparator.comparingInt((Acumulado a) -> a.setsAFavor - a.setsEnContra).reversed())
            .thenComparing(Comparator.comparingInt((Acumulado a) -> a.gamesAFavor - a.gamesEnContra).reversed())
            .thenComparing(a -> a.nombre);

    private static boolean seJugo(Partido partido) {
        return partido.getResultado() != null
                && partido.getParticipanteA() != null
                && partido.getParticipanteB() != null;
    }

    private static void sumar(Map<Long, Acumulado> porInscripcion, Partido partido) {
        Acumulado a = porInscripcion.get(partido.getParticipanteA().getId());
        Acumulado b = porInscripcion.get(partido.getParticipanteB().getId());
        if (a == null || b == null) {
            return;
        }
        Tanteador tanteador = Tanteador.de(partido.getSets());
        a.registrar(tanteador.setsA, tanteador.setsB, tanteador.gamesA, tanteador.gamesB);
        b.registrar(tanteador.setsB, tanteador.setsA, tanteador.gamesB, tanteador.gamesA);
    }

    /** Lo que suma un partido para cada lado, leído del tanteador guardado ("6-4 3-6 7-5"). */
    private record Tanteador(int setsA, int setsB, int gamesA, int gamesB) {

        static Tanteador de(String sets) {
            if (sets == null || sets.isBlank()) {
                return new Tanteador(0, 0, 0, 0);
            }
            int setsA = 0, setsB = 0, gamesA = 0, gamesB = 0;
            for (String set : sets.trim().split("\\s+")) {
                String[] games = set.split("-");
                if (games.length != 2) {
                    continue;
                }
                try {
                    int ga = Integer.parseInt(games[0]);
                    int gb = Integer.parseInt(games[1]);
                    gamesA += ga;
                    gamesB += gb;
                    if (ga > gb) setsA++;
                    else if (gb > ga) setsB++;
                } catch (NumberFormatException e) {
                    // Un set mal escrito no invalida el resto del tanteador
                }
            }
            return new Tanteador(setsA, setsB, gamesA, gamesB);
        }
    }

    /** Lo que lleva acumulado un participante mientras se recorren los partidos. */
    private static final class Acumulado {
        private final Long inscripcionId;
        private final String nombre;
        private int jugados;
        private int ganados;
        private int perdidos;
        private int setsAFavor;
        private int setsEnContra;
        private int gamesAFavor;
        private int gamesEnContra;

        Acumulado(Inscripcion inscripcion) {
            this.inscripcionId = inscripcion.getId();
            this.nombre = PartidoResponse.formatearParticipante(inscripcion);
        }

        void registrar(int setsPropios, int setsAjenos, int gamesPropios, int gamesAjenos) {
            jugados++;
            if (setsPropios > setsAjenos) ganados++;
            else perdidos++;
            setsAFavor += setsPropios;
            setsEnContra += setsAjenos;
            gamesAFavor += gamesPropios;
            gamesEnContra += gamesAjenos;
        }

        PosicionResponse aRespuesta(int puesto) {
            return new PosicionResponse(puesto, inscripcionId, nombre, jugados, ganados, perdidos,
                    setsAFavor, setsEnContra, gamesAFavor, gamesEnContra);
        }
    }
}
