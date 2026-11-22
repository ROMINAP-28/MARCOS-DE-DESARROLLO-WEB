package com.gestionproyectos.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TareaRequest(
        @NotBlank @Size(max = 150) String nombre,
        @Size(max = 500) String descripcion,
        String proyecto,
        @NotBlank String prioridad,
        String estado,
        @NotNull LocalDate fechaFin,
        String responsable,
        @NotNull @Positive Integer idProyecto,
        @NotNull @Positive Integer idResponsable) {}
