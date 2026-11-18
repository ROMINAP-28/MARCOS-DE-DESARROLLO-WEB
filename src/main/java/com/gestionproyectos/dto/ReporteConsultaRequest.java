package com.gestionproyectos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record ReporteConsultaRequest(@NotBlank String tipo, @Positive Integer idProyecto,
                                     LocalDate fechaDesde, LocalDate fechaHasta) {
}
