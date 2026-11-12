package com.tenisahora.infrastructure.pdf;

import com.tenisahora.infrastructure.pdf.PdfService.CampoFicha;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PdfServiceTest {

    private final PdfService pdfService = new PdfService();

    @Test
    void generarFicha_devuelveUnPdfValido() {
        byte[] pdf = pdfService.generarFicha("Ficha de persona", List.of(
                new CampoFicha("Nombre", "Ana"),
                new CampoFicha("DNI", "30123456")));

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    }

    @Test
    void generarFicha_conValoresVaciosONulos_noFalla() {
        byte[] pdf = pdfService.generarFicha("Ficha de persona", List.of(
                new CampoFicha("Número de socio", null),
                new CampoFicha("Dirección", " ")));

        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    }

    @Test
    void generarFicha_conTildesYEnies_noFalla() {
        byte[] pdf = pdfService.generarFicha("Ficha de persona", List.of(
                new CampoFicha("Nombre", "Muñoz Ñandú"),
                new CampoFicha("Dirección", "Av. San Martín 123")));

        assertThat(pdf.length).isGreaterThan(500);
    }
}
