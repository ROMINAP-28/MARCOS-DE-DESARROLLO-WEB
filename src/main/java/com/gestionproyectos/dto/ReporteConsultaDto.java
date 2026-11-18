package com.gestionproyectos.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ReporteConsultaDto(String tipo, String proyecto, LocalDateTime fechaConsulta,
                                 LocalDate fechaDesde, LocalDate fechaHasta,
                                 List<IndicadorDto> resumen) {
    public record IndicadorDto(String nombre, String valor) {
    }
}
