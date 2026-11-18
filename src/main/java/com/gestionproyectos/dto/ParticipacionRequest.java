package com.gestionproyectos.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ParticipacionRequest(
        @NotNull @Positive Integer idProyecto,
        @NotNull @Positive Integer idUsuario,
        @NotNull String estado) {}
