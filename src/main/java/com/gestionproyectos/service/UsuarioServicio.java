package com.gestionproyectos.service;

import com.gestionproyectos.dto.UsuarioDto;
import com.gestionproyectos.dto.RegistroRequest;
import com.gestionproyectos.dto.UsuarioActualizarRequest;
import com.gestionproyectos.exception.RecursoDuplicadoException;
import com.gestionproyectos.exception.RecursoNoEncontradoException;
import com.gestionproyectos.model.Usuario;
import com.gestionproyectos.repository.EspecialidadRepository;
import com.gestionproyectos.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class UsuarioServicio implements IUsuarioServicio {

    private final UsuarioRepository usuarioRepositorio;
    private final EspecialidadRepository especialidadRepositorio;
    private final PasswordEncoder passwordEncoder;

    public UsuarioServicio(UsuarioRepository usuarioRepositorio,
                           EspecialidadRepository especialidadRepositorio,
                           PasswordEncoder passwordEncoder) {
        this.usuarioRepositorio = usuarioRepositorio;
        this.especialidadRepositorio = especialidadRepositorio;
        this.passwordEncoder = passwordEncoder;
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

    @Override
    @Transactional
    public void registrar(RegistroRequest request) {
        String correo = normalizarCorreo(request.correo());
        if (usuarioRepositorio.existsByEmailIgnoreCase(correo)) {
            throw new RecursoDuplicadoException("Este correo ya está registrado.");
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre().trim());
        usuario.setEmail(correo);
        usuario.setPassword(passwordEncoder.encode(request.contrasena()));
        usuario.setRol("USUARIO");
        usuarioRepositorio.save(usuario);
    }

    @Override
    @Transactional
    public UsuarioDto actualizarUsuario(Integer idUsuario, UsuarioActualizarRequest request) {
        Usuario usuario = usuarioRepositorio.findById(idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        String correo = normalizarCorreo(request.correo());
        if (usuarioRepositorio.existsByEmailIgnoreCaseAndIdUsuarioNot(correo, idUsuario)) {
            throw new RecursoDuplicadoException("Este correo ya está registrado.");
        }
        usuario.setNombre(request.nombre().trim());
        usuario.setEmail(correo);
        usuario.setTelefono(request.telefono());
        usuario.setEspecialidad(request.idEspecialidad() == null ? null
                : especialidadRepositorio.findById(request.idEspecialidad())
                        .orElseThrow(() -> new RecursoNoEncontradoException("Especialidad no encontrada")));
        return aDto(usuarioRepositorio.save(usuario));
    }

    @Override
    @Transactional
    public void eliminarUsuario(Integer idUsuario) {
        Usuario usuario = usuarioRepositorio.findById(idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        usuarioRepositorio.delete(usuario);
        usuarioRepositorio.flush();
    }

    private String normalizarCorreo(String correo) {
        return correo.trim().toLowerCase(Locale.ROOT);
    }

    private UsuarioDto aDto(Usuario u) {
        return new UsuarioDto(u.getIdUsuario(), u.getNombre(), u.getEmail(), u.getTelefono(),
                u.getRol(),
                u.getEspecialidad() == null ? null : u.getEspecialidad().getNombreEspecialidad(),
                u.getFechaRegistro());
    }
}
