package com.tenisahora.modules.torneo.service;

public final class ValidadorResultadoTenis {

    private ValidadorResultadoTenis() {
    }

    public record ResultadoValidado(
            boolean valido,
            int setsGanadosA,
            int setsGanadosB,
            Integer ganador,
            String descripcion,
            String error
    ) {
        public static ResultadoValidado invalido(String error) {
            return new ResultadoValidado(false, 0, 0, null, null, error);
        }

        public static ResultadoValidado valido(int setsA, int setsB, int ganador, String descripcion) {
            return new ResultadoValidado(true, setsA, setsB, ganador, descripcion, null);
        }
    }

    public static ResultadoValidado validar(String sets) {
        if (sets == null || sets.trim().isEmpty()) {
            return ResultadoValidado.invalido("El tanteador no puede ser nulo ni vacío");
        }

        String[] tokens = sets.trim().split("\\s+");
        if (tokens.length < 2 || tokens.length > 3) {
            return ResultadoValidado.invalido("Un partido al mejor de 3 sets debe constar de 2 o 3 sets");
        }

        return procesarSets(tokens, sets.trim());
    }

    private static ResultadoValidado procesarSets(String[] tokens, String trimSets) {
        int setsA = 0;
        int setsB = 0;

        for (String token : tokens) {
            if (setsA == 2 || setsB == 2) {
                return ResultadoValidado.invalido("El partido ya había finalizado en sets anteriores");
            }
            int res = evaluarSet(token);
            if (res == 1) setsA++;
            else if (res == 2) setsB++;
            else return ResultadoValidado.invalido("Set inválido según reglas de tenis: " + token);
        }

        if (setsA != 2 && setsB != 2) {
            return ResultadoValidado.invalido("Partido inconcluso: ninguno de los participantes alcanzó 2 sets ganados");
        }

        int ganador = setsA == 2 ? 1 : 2;
        String desc = String.format("Gana Participante %s por %d-%d (%s)",
                ganador == 1 ? "A" : "B", Math.max(setsA, setsB), Math.min(setsA, setsB), trimSets);

        return ResultadoValidado.valido(setsA, setsB, ganador, desc);
    }

    private static int evaluarSet(String set) {
        String[] games = set.split("-");
        if (games.length != 2) return 0;

        try {
            int g1 = Integer.parseInt(games[0]);
            int g2 = Integer.parseInt(games[1]);

            if ((g1 == 6 && g2 <= 4) || (g1 == 7 && (g2 == 5 || g2 == 6))) {
                return 1;
            }
            if ((g2 == 6 && g1 <= 4) || (g2 == 7 && (g1 == 5 || g1 == 6))) {
                return 2;
            }
            return 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
