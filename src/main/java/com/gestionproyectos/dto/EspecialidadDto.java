package com.gestionproyectos.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EspecialidadDto(Integer id,
                              @NotBlank @Size(max = 100) String nombre,
                              @Size(max = 255) String descripcion) {}
