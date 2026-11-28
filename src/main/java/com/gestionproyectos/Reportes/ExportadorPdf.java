package com.gestionproyectos.Reportes;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class ExportadorPdf {

    public byte[] generar(
            String titulo,
            List<String> encabezados,
            List<List<Object>> filas) {

        Document documento = new Document(PageSize.A4.rotate());

        try {
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            PdfWriter.getInstance(documento, salida);
            documento.open();

            Paragraph tituloPdf = new Paragraph(
                    titulo,
                    new Font(Font.HELVETICA, 16, Font.BOLD)
            );
            tituloPdf.setAlignment(Element.ALIGN_CENTER);
            documento.add(tituloPdf);
            documento.add(new Paragraph(" "));

            PdfPTable tabla = new PdfPTable(encabezados.size());
            tabla.setWidthPercentage(100);

            Font fuenteEncabezado =
                    new Font(Font.HELVETICA, 10, Font.BOLD);

            for (String encabezado : encabezados) {
                PdfPCell celda = new PdfPCell(
                        new Phrase(encabezado, fuenteEncabezado)
                );
                tabla.addCell(celda);
            }

            for (List<Object> fila : filas) {
                for (int i = 0; i < encabezados.size(); i++) {
                    Object valor = i < fila.size() ? fila.get(i) : null;
                    tabla.addCell(valor == null ? "" : valor.toString());
                }
            }

            documento.add(tabla);
            documento.close();

            return salida.toByteArray();

        } catch (Exception e) {
            if (documento.isOpen()) {
                documento.close();
            }
            throw new IllegalStateException(
                    "No se pudo generar el reporte PDF", e
            );
        }
    }
}