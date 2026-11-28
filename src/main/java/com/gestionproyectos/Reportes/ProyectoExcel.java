package com.gestionproyectos.Reportes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.gestionproyectos.dto.ProyectoDto;

public class ProyectoExcel {

    public byte[] exportar(List<ProyectoDto> proyectos) throws IOException {

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet hoja = workbook.createSheet("Proyectos");

            CellStyle estiloEncabezado = workbook.createCellStyle();
            Font fuente = workbook.createFont();
            fuente.setBold(true);
            estiloEncabezado.setFont(fuente);

            String[] columnas = {
                "ID", "Nombre", "Descripción", "Responsable",
                "Fecha de inicio", "Fecha de fin", "Prioridad",
                "Estado", "Avance (%)"
            };

            Row encabezado = hoja.createRow(0);

            for (int i = 0; i < columnas.length; i++) {
                Cell celda = encabezado.createCell(i);
                celda.setCellValue(columnas[i]);
                celda.setCellStyle(estiloEncabezado);
            }

            int numeroFila = 1;

            for (ProyectoDto proyecto : proyectos) {
                Row fila = hoja.createRow(numeroFila++);

                fila.createCell(0).setCellValue(
                        proyecto.id() == null ? "" : proyecto.id().toString()
                );
                fila.createCell(1).setCellValue(texto(proyecto.nombre()));
                fila.createCell(2).setCellValue(texto(proyecto.descripcion()));
                fila.createCell(3).setCellValue(texto(proyecto.responsable()));
                fila.createCell(4).setCellValue(texto(proyecto.fechaInicio()));
                fila.createCell(5).setCellValue(texto(proyecto.fechaFin()));
                fila.createCell(6).setCellValue(texto(proyecto.prioridad()));
                fila.createCell(7).setCellValue(texto(proyecto.estado()));
                fila.createCell(8).setCellValue(proyecto.progreso());
            }

            for (int i = 0; i < columnas.length; i++) {
                hoja.autoSizeColumn(i);
            }

            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            workbook.write(salida);
            return salida.toByteArray();
        }
    }

    private String texto(Object valor) {
        return valor == null ? "" : valor.toString();
    }
}
