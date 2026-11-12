package com.tenisahora.shared.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextosTest {

    @Test
    void recortar_sacaLosEspaciosDeLosExtremos() {
        assertThat(Textos.recortar("  ana@correo.com \t")).isEqualTo("ana@correo.com");
    }

    @Test
    void recortar_unNuloSigueSiendoNulo() {
        assertThat(Textos.recortar(null)).isNull();
    }

    @Test
    void recortar_unTextoEnBlancoQuedaVacio() {
        assertThat(Textos.recortar("   ")).isEmpty();
    }

    @Test
    void sinTildesEnMinusculas_quitaLasTildesYLasDieresis() {
        assertThat(Textos.sinTildesEnMinusculas("GÓMEZ Pingüino Álvarez")).isEqualTo("gomez pinguino alvarez");
    }

    @Test
    void sinTildesEnMinusculas_noTocaLaEnie() {
        assertThat(Textos.sinTildesEnMinusculas("Muñoz")).isEqualTo("muñoz");
    }

    @Test
    void lasDosListasDeLetrasTienenElMismoLargo() {
        assertThat(Textos.LETRAS_CON_TILDE).hasSameSizeAs(Textos.LETRAS_SIN_TILDE);
    }

    @Test
    void patronContiene_normalizaYBuscaEnCualquierParte() {
        assertThat(Textos.patronContiene("Gómez")).isEqualTo("%gomez%");
    }

    @Test
    void patronContiene_escapaLosComodinesYElCaracterDeEscape() {
        assertThat(Textos.patronContiene("50%_!")).isEqualTo("%50!%!_!!%");
    }
}
