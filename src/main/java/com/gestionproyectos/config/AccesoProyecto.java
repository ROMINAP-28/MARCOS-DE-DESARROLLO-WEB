package com.gestionproyectos.config;

import com.gestionproyectos.dto.ProyectoRequest;
import com.gestionproyectos.dto.TareaRequest;
import com.gestionproyectos.model.Proyecto;
import com.gestionproyectos.model.Tarea;
import com.gestionproyectos.model.Usuario;
import com.gestionproyectos.repository.ProyectoRepository;
import com.gestionproyectos.repository.ProyectoUsuarioRepository;
import com.gestionproyectos.repository.TareaRepository;
import com.gestionproyectos.repository.UsuarioRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component("accesoProyecto")
public class AccesoProyecto {
    private final ProyectoRepository proyectos;
    private final TareaRepository tareas;
    private final UsuarioRepository usuarios;
    private final ProyectoUsuarioRepository participaciones;

    public AccesoProyecto(ProyectoRepository proyectos, TareaRepository tareas,
                          UsuarioRepository usuarios, ProyectoUsuarioRepository participaciones) {
        this.proyectos = proyectos;
        this.tareas = tareas;
        this.usuarios = usuarios;
        this.participaciones = participaciones;
    }

    public boolean esUsuario(Integer id) {
        return id != null && Objects.equals(id, idActual());
    }

    public boolean puedeCrearProyecto(ProyectoRequest request) {
        // Las solicitudes nuevas deben identificar al responsable por ID; el nombre no otorga permiso.
        return request != null && esUsuario(request.idResponsable());
    }

    public boolean esResponsable(Integer idProyecto) {
        return idProyecto != null && proyectos.findById(idProyecto)
                .map(p -> esUsuario(p.getResponsable().getIdUsuario())).orElse(false);
    }

    public boolean esResponsableDeAlgunProyecto() {
        return idActual() != null && proyectos.findAll().stream()
                .anyMatch(p -> esUsuario(p.getResponsable().getIdUsuario()));
    }

    public boolean puedeCrearTarea(TareaRequest request) {
        if (request == null || request.idProyecto() == null || request.idResponsable() == null) return false;
        Proyecto proyecto = proyectos.findById(request.idProyecto()).orElse(null);
        if (proyecto == null || !esResponsable(proyecto.getIdProyecto())) return false;
        Usuario responsable = usuarios.findById(request.idResponsable()).orElse(null);
        return responsable != null && participaciones.buscarPorProyecto(proyecto.getIdProyecto()).stream()
                .anyMatch(p -> p.getUsuario().getIdUsuario().equals(responsable.getIdUsuario())
                        && "Activo".equals(p.getEstado()));
    }

    public boolean esResponsableDeTarea(Integer idTarea) {
        Tarea tarea = tarea(idTarea);
        return tarea != null && esResponsable(tarea.getProyecto().getIdProyecto());
    }

    public boolean puedeCambiarEstado(Integer idTarea) {
        Tarea tarea = tarea(idTarea);
        return tarea != null && (esResponsable(tarea.getProyecto().getIdProyecto())
                || esUsuario(tarea.getResponsable().getIdUsuario()));
    }

    private Tarea tarea(Integer id) {
        return id == null ? null : tareas.findById(id).orElse(null);
    }

    private Integer idActual() {
        Object principal = SecurityContextHolder.getContext().getAuthentication() == null ? null
                : SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return principal instanceof UsuarioPrincipal usuario ? usuario.getId() : null;
    }
}
