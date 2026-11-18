package com.gestionproyectos.service;

import com.gestionproyectos.dto.EstadoParticipacionRequest;
import com.gestionproyectos.dto.ParticipacionDto;
import com.gestionproyectos.dto.ParticipacionRequest;
import com.gestionproyectos.exception.RecursoDuplicadoException;
import com.gestionproyectos.exception.RecursoNoEncontradoException;
import com.gestionproyectos.exception.SolicitudInvalidaException;
import com.gestionproyectos.model.ProyectoUsuario;
import com.gestionproyectos.model.ProyectoUsuarioId;
import com.gestionproyectos.repository.ProyectoRepository;
import com.gestionproyectos.repository.ProyectoUsuarioRepository;
import com.gestionproyectos.repository.TareaRepository;
import com.gestionproyectos.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class ParticipacionServicio {
    private static final Set<String> ESTADOS = Set.of("Activo", "Inactivo", "Pendiente");

    private final ProyectoUsuarioRepository participaciones;
    private final ProyectoRepository proyectos;
    private final UsuarioRepository usuarios;
    private final TareaRepository tareas;

    public ParticipacionServicio(ProyectoUsuarioRepository participaciones,
                                 ProyectoRepository proyectos, UsuarioRepository usuarios,
                                 TareaRepository tareas) {
        this.participaciones = participaciones;
        this.proyectos = proyectos;
        this.usuarios = usuarios;
        this.tareas = tareas;
    }

    @Transactional(readOnly = true)
    public List<ParticipacionDto> listar() {
        return participaciones.listarParticipaciones().stream().map(this::aDto).toList();
    }

    public ParticipacionDto agregar(ParticipacionRequest request) {
        String estado = validarEstado(request.estado());
        ProyectoUsuarioId id = id(request.idProyecto(), request.idUsuario());
        if (participaciones.existsById(id)) {
            throw new RecursoDuplicadoException("El usuario ya pertenece a este proyecto.");
        }
        var proyecto = proyectos.findById(request.idProyecto())
                .orElseThrow(() -> new RecursoNoEncontradoException("Proyecto no encontrado"));
        var usuario = usuarios.findById(request.idUsuario())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        if (proyecto.getResponsable().getIdUsuario().equals(usuario.getIdUsuario())) {
            throw new SolicitudInvalidaException("El responsable ya debe pertenecer al proyecto.");
        }

        ProyectoUsuario participacion = new ProyectoUsuario();
        participacion.setId(id);
        participacion.setProyecto(proyecto);
        participacion.setUsuario(usuario);
        participacion.setRol("Integrante");
        participacion.setEstado(estado);
        participacion.setFechaAsignacion(LocalDateTime.now());
        return aDto(participaciones.save(participacion));
    }

    public ParticipacionDto cambiarEstado(Integer idProyecto, Integer idUsuario,
                                           EstadoParticipacionRequest request) {
        ProyectoUsuario participacion = obtener(idProyecto, idUsuario);
        participacion.setEstado(validarEstado(request.estado()));
        return aDto(participacion);
    }

    public void retirar(Integer idProyecto, Integer idUsuario) {
        ProyectoUsuario participacion = obtener(idProyecto, idUsuario);
        if (participacion.getProyecto().getResponsable().getIdUsuario().equals(idUsuario)) {
            throw new SolicitudInvalidaException("No se puede retirar al responsable del proyecto.");
        }
        if (tareas.countByProyecto_IdProyectoAndResponsable_IdUsuario(idProyecto, idUsuario) > 0) {
            throw new SolicitudInvalidaException("Reasigna sus tareas antes de retirarlo del proyecto.");
        }
        participaciones.delete(participacion);
    }

    private ProyectoUsuario obtener(Integer idProyecto, Integer idUsuario) {
        return participaciones.findById(id(idProyecto, idUsuario))
                .orElseThrow(() -> new RecursoNoEncontradoException("Participación no encontrada"));
    }

    private ProyectoUsuarioId id(Integer idProyecto, Integer idUsuario) {
        ProyectoUsuarioId id = new ProyectoUsuarioId();
        id.setIdProyecto(idProyecto);
        id.setIdUsuario(idUsuario);
        return id;
    }

    private String validarEstado(String estado) {
        if (estado == null || !ESTADOS.contains(estado)) {
            throw new SolicitudInvalidaException("Estado de participación no válido.");
        }
        return estado;
    }

    private ParticipacionDto aDto(ProyectoUsuario pu) {
        var usuario = pu.getUsuario();
        return new ParticipacionDto(pu.getProyecto().getIdProyecto(), usuario.getIdUsuario(),
                pu.getProyecto().getNombre(), usuario.getNombre(), usuario.getEmail(), pu.getRol(),
                usuario.getEspecialidad() == null ? null : usuario.getEspecialidad().getNombreEspecialidad(),
                pu.getEstado(), pu.getFechaAsignacion(),
                tareas.countByProyecto_IdProyectoAndResponsable_IdUsuario(
                        pu.getProyecto().getIdProyecto(), usuario.getIdUsuario()));
    }
}
