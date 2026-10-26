package com.gestionproyectos.dto;

import java.time.LocalDate;

/** Respuesta que consume tareas.js (proyecto y responsable van por nombre). */
public record TareaDto(Integer id, String nombre, String descripcion,
                       String proyecto, String prioridad, String estado,
                       LocalDate fechaFin, String responsable) {}
