    package com.gestionproyectos.Reportes;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class ExportadorExcel {

    public byte[] generar(
            String nombreHoja,
            List<String> encabezados,
            List<List<Object>> filas) throws IOException {

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet hoja = workbook.createSheet(nombreHoja);

            CellStyle estiloEncabezado = workbook.createCellStyle();
            Font fuente = workbook.createFont();
            fuente.setBold(true);
            estiloEncabezado.setFont(fuente);

            Row encabezado = hoja.createRow(0);

            for (int i = 0; i < encabezados.size(); i++) {
                Cell celda = encabezado.createCell(i);
                celda.setCellValue(encabezados.get(i));
                celda.setCellStyle(estiloEncabezado);
            }

            int numeroFila = 1;

            for (List<Object> datos : filas) {
                Row fila = hoja.createRow(numeroFila++);

                for (int i = 0; i < encabezados.size(); i++) {
                    Object valor = i < datos.size() ? datos.get(i) : null;
                    Cell celda = fila.createCell(i);

                    if (valor instanceof Number numero) {
                        celda.setCellValue(numero.doubleValue());
                    } else if (valor instanceof Boolean booleano) {
                        celda.setCellValue(booleano);
                    } else {
                        celda.setCellValue(valor == null ? "" : valor.toString());
                    }
                }
            }

            for (int i = 0; i < encabezados.size(); i++) {
                hoja.autoSizeColumn(i);
            }

            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            workbook.write(salida);
            return salida.toByteArray();
        }
    }
}