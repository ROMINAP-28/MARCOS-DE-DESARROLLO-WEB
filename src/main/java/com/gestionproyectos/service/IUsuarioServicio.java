package com.gestionproyectos.service;

import com.gestionproyectos.dto.UsuarioDto;

import java.util.List;

public interface IUsuarioServicio {

    List<UsuarioDto> listarUsuarios();

    UsuarioDto buscarUsuarioPorId(Integer idUsuario);
}
