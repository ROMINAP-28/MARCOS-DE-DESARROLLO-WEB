package com.gestionproyectos.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProyectoRequest(
        @NotBlank @Size(max = 150) String nombre,
        @NotBlank @Size(max = 255) String descripcion,
        @NotNull LocalDate fechaInicio,
        @NotNull LocalDate fechaFin,
        @NotBlank String prioridad,
        String responsable,
        List<String> integrantes,
        String estado,
        Integer progreso,
        @NotNull @Positive Integer idResponsable,
        @NotNull List<@NotNull @Positive Integer> idsIntegrantes) {}
