package com.gestionproyectos.service;

import com.gestionproyectos.dto.UsuarioDto;
import com.gestionproyectos.exception.RecursoNoEncontradoException;
import com.gestionproyectos.model.Usuario;
import com.gestionproyectos.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class UsuarioServicio implements IUsuarioServicio {

    private final UsuarioRepository usuarioRepositorio;

    public UsuarioServicio(UsuarioRepository usuarioRepositorio) {
        this.usuarioRepositorio = usuarioRepositorio;
    }

    @Override
    public List<UsuarioDto> listarUsuarios() {
        return usuarioRepositorio.findAllByOrderByNombreAsc().stream().map(this::aDto).toList();
    }

    @Override
    public UsuarioDto buscarUsuarioPorId(Integer idUsuario) {
        return aDto(usuarioRepositorio.findById(idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado")));
    }

    private UsuarioDto aDto(Usuario u) {
        return new UsuarioDto(u.getIdUsuario(), u.getNombre(), u.getEmail(), u.getTelefono(),
                u.getRol(),
                u.getEspecialidad() == null ? null : u.getEspecialidad().getNombreEspecialidad(),
                u.getFechaRegistro());
    }
}
