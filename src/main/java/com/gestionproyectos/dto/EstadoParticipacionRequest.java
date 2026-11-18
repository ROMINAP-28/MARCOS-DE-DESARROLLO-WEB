package com.gestionproyectos.dto;

import jakarta.validation.constraints.NotNull;

public record EstadoParticipacionRequest(@NotNull String estado) {}
