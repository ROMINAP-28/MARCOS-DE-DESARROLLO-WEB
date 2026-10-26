package com.gestionproyectos.dto;

import java.time.LocalDate;
import java.util.List;

/** Respuesta que consume proyectos.js (responsable e integrantes van por nombre). */
public record ProyectoDto(Integer id, String nombre, String descripcion,
                          LocalDate fechaInicio, LocalDate fechaFin, String prioridad,
                          String responsable, List<String> integrantes,
                          String estado, int progreso) {}
