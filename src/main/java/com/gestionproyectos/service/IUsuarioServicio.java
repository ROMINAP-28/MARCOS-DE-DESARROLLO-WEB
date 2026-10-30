package com.gestionproyectos.service;

import com.gestionproyectos.dto.UsuarioDto;
import com.gestionproyectos.dto.RegistroRequest;
import com.gestionproyectos.dto.UsuarioActualizarRequest;

import java.util.List;

public interface IUsuarioServicio {

    List<UsuarioDto> listarUsuarios();

    UsuarioDto buscarUsuarioPorId(Integer idUsuario);

    void registrar(RegistroRequest request);

    UsuarioDto actualizarUsuario(Integer idUsuario, UsuarioActualizarRequest request);

    void eliminarUsuario(Integer idUsuario);
}
