package com.gestionproyectos.dto;

import java.time.LocalDateTime;

public record ParticipacionDto(Integer idProyecto, Integer idUsuario, String proyecto,
                               String nombre, String correo, String rol, String especialidad,
                               String estado, LocalDateTime fechaAsignacion, long tareasAsignadas) {}
