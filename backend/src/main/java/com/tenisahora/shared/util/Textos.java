package com.tenisahora.shared.util;

import java.util.Locale;

public final class Textos {

    /** Letras con tilde o diéresis y su versión sin ella. La ñ no se toca: es otra letra. */
    public static final String LETRAS_CON_TILDE = "ÁÉÍÓÚÜáéíóúü";
    public static final String LETRAS_SIN_TILDE = "AEIOUUaeiouu";
    private static final String CARACTER_ESCAPE = "!";

    private Textos() {
    }

    /** Saca los espacios del principio y del final. Los nulos quedan nulos. */
    public static String recortar(String texto) {
        return texto == null ? null : texto.strip();
    }

    /** Correo en su forma única de guardar y comparar: sin espacios y en minúsculas. */
    public static String normalizarEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    /** Pasa a minúsculas y quita tildes, para comparar nombres sin distinguir acentos ("Gómez" = "gomez"). */
    public static String sinTildesEnMinusculas(String texto) {
        StringBuilder resultado = new StringBuilder(texto.length());
        for (char letra : texto.toCharArray()) {
            int posicion = LETRAS_CON_TILDE.indexOf(letra);
            resultado.append(posicion >= 0 ? LETRAS_SIN_TILDE.charAt(posicion) : letra);
        }
        return resultado.toString().toLowerCase(Locale.ROOT);
    }

    /**
     * Patrón LIKE para buscar un texto en cualquier parte: sin tildes, en minúsculas y con los comodines escapados con
     * '!' (las consultas usan "escape '!'"), así "%" y "_" se buscan como texto común.
     */
    public static String patronContiene(String texto) {
        String escapado = sinTildesEnMinusculas(texto)
                .replace(CARACTER_ESCAPE, CARACTER_ESCAPE + CARACTER_ESCAPE)
                .replace("%", CARACTER_ESCAPE + "%")
                .replace("_", CARACTER_ESCAPE + "_");
        return "%" + escapado + "%";
    }
}
