package com.gestionproyectos.dto;

import java.time.LocalDateTime;

/** Vista pública de un usuario: nunca incluye la contraseña. */
public record UsuarioDto(Integer id, String nombre, String correo, String telefono,
                         String rol, String especialidad, LocalDateTime fechaRegistro) {}
