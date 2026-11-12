package com.tenisahora.infrastructure.pdf;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Genera los documentos de los métodos imprimir(). No conoce ningún módulo: recibe el título y los campos ya armados. */
@Service
public class PdfService {

    /** Un renglón de la ficha: etiqueta y valor. */
    public record CampoFicha(String etiqueta, String valor) {
    }

    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Font FUENTE_MARCA = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
    private static final Font FUENTE_TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20);
    private static final Font FUENTE_ETIQUETA = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
    private static final Font FUENTE_VALOR = FontFactory.getFont(FontFactory.HELVETICA, 11);
    private static final Font FUENTE_PIE = FontFactory.getFont(FontFactory.HELVETICA, 9);

    public byte[] generarFicha(String titulo, List<CampoFicha> campos) {
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Document documento = new Document(PageSize.A4, 56, 56, 56, 56);
            PdfWriter.getInstance(documento, salida);
            documento.open();
            documento.add(encabezado(titulo));
            documento.add(tablaDe(campos));
            documento.add(pie());
            documento.close();
            return salida.toByteArray();
        } catch (DocumentException | IOException e) {
            throw new IllegalStateException("No se pudo generar el PDF", e);
        }
    }

    private Paragraph encabezado(String titulo) {
        Paragraph encabezado = new Paragraph();
        encabezado.add(new Chunk("Tenis Ahora", FUENTE_MARCA));
        encabezado.add(Chunk.NEWLINE);
        encabezado.add(new Chunk(titulo, FUENTE_TITULO));
        encabezado.setSpacingAfter(18);
        return encabezado;
    }

    private PdfPTable tablaDe(List<CampoFicha> campos) {
        PdfPTable tabla = new PdfPTable(new float[]{1f, 2f});
        tabla.setWidthPercentage(100);
        for (CampoFicha campo : campos) {
            tabla.addCell(celda(campo.etiqueta(), FUENTE_ETIQUETA));
            tabla.addCell(celda(campo.valor() == null || campo.valor().isBlank() ? "-" : campo.valor(), FUENTE_VALOR));
        }
        return tabla;
    }

    private Paragraph pie() {
        Paragraph pie = new Paragraph("Generado el " + LocalDateTime.now().format(FORMATO_FECHA_HORA), FUENTE_PIE);
        pie.setSpacingBefore(24);
        pie.setAlignment(Element.ALIGN_RIGHT);
        return pie;
    }

    private PdfPCell celda(String texto, Font fuente) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, fuente));
        celda.setBorder(Rectangle.BOTTOM);
        celda.setPadding(7);
        return celda;
    }
}
