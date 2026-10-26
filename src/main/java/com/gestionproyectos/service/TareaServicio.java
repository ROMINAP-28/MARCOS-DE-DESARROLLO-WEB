package com.gestionproyectos.service;

import com.gestionproyectos.dto.TareaDto;
import com.gestionproyectos.dto.TareaRequest;
import com.gestionproyectos.exception.RecursoNoEncontradoException;
import com.gestionproyectos.exception.SolicitudInvalidaException;
import com.gestionproyectos.model.Proyecto;
import com.gestionproyectos.model.Tarea;
import com.gestionproyectos.model.Usuario;
import com.gestionproyectos.repository.ProyectoRepository;
import com.gestionproyectos.repository.TareaRepository;
import com.gestionproyectos.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class TareaServicio implements ITareaServicio {

    private static final List<String> ESTADOS = List.of("Pendiente", "En progreso", "Completada");
    private static final List<String> PRIORIDADES = List.of("Baja", "Media", "Alta", "Urgente");

    private final TareaRepository tareaRepositorio;
    private final ProyectoRepository proyectoRepositorio;
    private final UsuarioRepository usuarioRepositorio;

    public TareaServicio(TareaRepository tareaRepositorio,
                         ProyectoRepository proyectoRepositorio,
                         UsuarioRepository usuarioRepositorio) {
        this.tareaRepositorio = tareaRepositorio;
        this.proyectoRepositorio = proyectoRepositorio;
        this.usuarioRepositorio = usuarioRepositorio;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TareaDto> listarTareas() {
        return tareaRepositorio.findAllByOrderByIdTareaDesc().stream().map(this::aDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TareaDto buscarTareaPorId(Integer idTarea) {
        return aDto(obtener(idTarea));
    }

    @Override
    public TareaDto guardarTarea(TareaRequest request) {
        Tarea tarea = new Tarea();
        tarea.setFechaCreacion(LocalDateTime.now());
        copiarDatos(tarea, request, "Pendiente");
        return aDto(tareaRepositorio.save(tarea));
    }

    @Override
    public TareaDto actualizarTarea(Integer idTarea, TareaRequest request) {
        Tarea tarea = obtener(idTarea);
        copiarDatos(tarea, request, tarea.getEstado());
        return aDto(tareaRepositorio.save(tarea));
    }

    @Override
    public TareaDto cambiarEstado(Integer idTarea, String estado) {
        if (estado == null || !ESTADOS.contains(estado)) {
            throw new SolicitudInvalidaException("Estado no válido");
        }
        Tarea tarea = obtener(idTarea);
        tarea.setEstado(estado);
        return aDto(tareaRepositorio.save(tarea));
    }

    @Override
    public void eliminarTarea(Integer idTarea) {
        tareaRepositorio.delete(obtener(idTarea));
    }

    // ---------------- helpers ----------------

    private Tarea obtener(Integer idTarea) {
        return tareaRepositorio.findById(idTarea)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tarea no encontrada"));
    }

    private void copiarDatos(Tarea tarea, TareaRequest r, String estadoPorDefecto) {
        if (!PRIORIDADES.contains(r.prioridad())) {
            throw new SolicitudInvalidaException("Prioridad no válida");
        }
        String estado = (r.estado() == null || r.estado().isBlank()) ? estadoPorDefecto : r.estado();
        if (!ESTADOS.contains(estado)) {
            throw new SolicitudInvalidaException("Estado no válido");
        }
        Proyecto proyecto = proyectoRepositorio.findFirstByNombreIgnoreCase(r.proyecto().trim())
                .orElseThrow(() -> new SolicitudInvalidaException(
                        "No existe el proyecto \"" + r.proyecto() + "\"."));
        Usuario responsable = usuarioRepositorio.findFirstByNombreIgnoreCase(r.responsable().trim())
                .orElseThrow(() -> new SolicitudInvalidaException(
                        "No existe el usuario \"" + r.responsable() + "\"."));

        tarea.setNombre(r.nombre().trim());
        tarea.setDescripcion(r.descripcion());
        tarea.setPrioridad(r.prioridad());
        tarea.setEstado(estado);
        tarea.setFechaFin(r.fechaFin());
        tarea.setProyecto(proyecto);
        tarea.setResponsable(responsable);
    }

    private TareaDto aDto(Tarea t) {
        return new TareaDto(t.getIdTarea(), t.getNombre(), t.getDescripcion(),
                t.getProyecto().getNombre(), t.getPrioridad(), t.getEstado(),
                t.getFechaFin(), t.getResponsable().getNombre());
    }
}
