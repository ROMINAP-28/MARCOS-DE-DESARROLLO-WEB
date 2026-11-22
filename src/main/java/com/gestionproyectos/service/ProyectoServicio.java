package com.gestionproyectos.service;

import com.gestionproyectos.dto.ProyectoDto;
import com.gestionproyectos.dto.ProyectoRequest;
import com.gestionproyectos.exception.RecursoNoEncontradoException;
import com.gestionproyectos.exception.SolicitudInvalidaException;
import com.gestionproyectos.model.Proyecto;
import com.gestionproyectos.model.ProyectoUsuario;
import com.gestionproyectos.model.ProyectoUsuarioId;
import com.gestionproyectos.model.Usuario;
import com.gestionproyectos.repository.ProyectoRepository;
import com.gestionproyectos.repository.ProyectoUsuarioRepository;
import com.gestionproyectos.repository.TareaRepository;
import com.gestionproyectos.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class ProyectoServicio implements IProyectoServicio {

    static final String ROL_RESPONSABLE = "Responsable";
    static final String ROL_INTEGRANTE = "Integrante";
    private static final List<String> ESTADOS =
            List.of("Planificado", "En progreso", "Completado", "Atrasado");
    private static final List<String> PRIORIDADES = List.of("Alta", "Media", "Baja");

    private final ProyectoRepository proyectoRepositorio;
    private final ProyectoUsuarioRepository proyectoUsuarioRepositorio;
    private final UsuarioRepository usuarioRepositorio;
    private final TareaRepository tareaRepositorio;

    public ProyectoServicio(ProyectoRepository proyectoRepositorio,
                            ProyectoUsuarioRepository proyectoUsuarioRepositorio,
                            UsuarioRepository usuarioRepositorio, TareaRepository tareaRepositorio) {
        this.proyectoRepositorio = proyectoRepositorio;
        this.proyectoUsuarioRepositorio = proyectoUsuarioRepositorio;
        this.usuarioRepositorio = usuarioRepositorio;
        this.tareaRepositorio = tareaRepositorio;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProyectoDto> listarProyectos(String busqueda) {
        List<Proyecto> proyectos = (busqueda == null || busqueda.isBlank())
                ? proyectoRepositorio.findAllByOrderByIdProyectoDesc()
                : proyectoRepositorio.buscar(busqueda.trim());
        return proyectos.stream().map(this::aDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProyectoDto buscarProyectoPorId(Integer idProyecto) {
        return aDto(obtener(idProyecto));
    }

    @Override
    public ProyectoDto guardarProyecto(ProyectoRequest request) {
        validarPeriodo(request);
        Usuario responsable = buscarResponsable(request);
        Map<Integer, Usuario> equipo = armarEquipo(responsable, request);

        Proyecto proyecto = new Proyecto();
        copiarDatos(proyecto, request);
        proyecto.setResponsable(responsable);
        proyecto.setEstado(estadoValido(request.estado(), "Planificado"));
        proyecto.setPorcentajeAvance(progresoValido(request.progreso(), 0));
        proyecto = proyectoRepositorio.save(proyecto);

        asignarEquipo(proyecto, responsable, equipo);
        return aDto(proyecto);
    }

    @Override
    public ProyectoDto actualizarProyecto(Integer idProyecto, ProyectoRequest request) {
        Proyecto proyecto = obtener(idProyecto);
        validarPeriodo(request);
        Usuario responsable = buscarResponsable(request);
        Map<Integer, Usuario> equipo = armarEquipo(responsable, request);

        List<ProyectoUsuario> actuales = proyectoUsuarioRepositorio.buscarPorProyecto(idProyecto);
        for (ProyectoUsuario miembro : actuales) {
            if (!equipo.containsKey(miembro.getId().getIdUsuario())
                    && tareaRepositorio.countByProyecto_IdProyectoAndResponsable_IdUsuario(
                            idProyecto, miembro.getId().getIdUsuario()) > 0) {
                throw new SolicitudInvalidaException(
                        "Reasigna las tareas del integrante antes de retirarlo del proyecto.");
            }
        }

        copiarDatos(proyecto, request);
        proyecto.setResponsable(responsable);
        proyecto.setEstado(estadoValido(request.estado(), proyecto.getEstado()));
        proyecto.setPorcentajeAvance(progresoValido(request.progreso(),
                proyecto.getPorcentajeAvance() == null ? 0 : Math.round(proyecto.getPorcentajeAvance())));
        proyectoRepositorio.save(proyecto);

        Map<Integer, ProyectoUsuario> porUsuario = new LinkedHashMap<>();
        for (ProyectoUsuario miembro : actuales) {
            porUsuario.put(miembro.getId().getIdUsuario(), miembro);
            if (!equipo.containsKey(miembro.getId().getIdUsuario())) {
                proyectoUsuarioRepositorio.delete(miembro);
            }
        }
        for (Usuario usuario : equipo.values()) {
            ProyectoUsuario miembro = porUsuario.get(usuario.getIdUsuario());
            if (miembro == null) {
                proyectoUsuarioRepositorio.save(crearMiembro(proyecto, usuario, responsable));
            } else {
                // Conservar fecha de asignación y estado de quien sigue en el proyecto.
                miembro.setRol(usuario.getIdUsuario().equals(responsable.getIdUsuario())
                        ? ROL_RESPONSABLE : ROL_INTEGRANTE);
            }
        }
        return aDto(proyecto);
    }

    @Override
    public void eliminarProyecto(Integer idProyecto) {
        obtener(idProyecto);
        // Eliminar primero el vínculo compuesto: @MapsId no permite que quede
        // una participación gestionada referenciando al proyecto borrado.
        proyectoUsuarioRepositorio.eliminarPorProyecto(idProyecto);
        // cascade = ALL borra tareas, comentarios, notificaciones, recursos y reportes.
        proyectoRepositorio.delete(obtener(idProyecto));
    }

    // ---------------- helpers ----------------

    private Proyecto obtener(Integer idProyecto) {
        return proyectoRepositorio.findById(idProyecto)
                .orElseThrow(() -> new RecursoNoEncontradoException("Proyecto no encontrado"));
    }

    private Usuario buscarResponsable(ProyectoRequest request) {
        if (request.idResponsable() == null) {
            throw new SolicitudInvalidaException("Indica el ID del responsable.");
        }
        return buscarUsuarioPorId(request.idResponsable());
    }

    private Usuario buscarUsuarioPorId(Integer id) {
        return usuarioRepositorio.findById(id)
                .orElseThrow(() -> new SolicitudInvalidaException("No existe el usuario con ID " + id + "."));
    }

    /** Responsable primero, luego integrantes sin repetir. */
    private Map<Integer, Usuario> armarEquipo(Usuario responsable, ProyectoRequest request) {
        if (request.idsIntegrantes() == null) {
            throw new SolicitudInvalidaException("Indica los IDs de los integrantes (puede ser una lista vacía).");
        }
        Map<Integer, Usuario> equipo = new LinkedHashMap<>();
        equipo.put(responsable.getIdUsuario(), responsable);
        for (Integer id : request.idsIntegrantes()) {
            Usuario u = buscarUsuarioPorId(id);
            equipo.putIfAbsent(u.getIdUsuario(), u);
        }
        return equipo;
    }

    private void asignarEquipo(Proyecto proyecto, Usuario responsable, Map<Integer, Usuario> equipo) {
        LocalDateTime ahora = LocalDateTime.now();
        for (Usuario usuario : equipo.values()) {
            proyectoUsuarioRepositorio.save(crearMiembro(proyecto, usuario, responsable, ahora));
        }
    }

    private ProyectoUsuario crearMiembro(Proyecto proyecto, Usuario usuario, Usuario responsable) {
        return crearMiembro(proyecto, usuario, responsable, LocalDateTime.now());
    }

    private ProyectoUsuario crearMiembro(Proyecto proyecto, Usuario usuario, Usuario responsable,
                                         LocalDateTime fecha) {
        ProyectoUsuarioId id = new ProyectoUsuarioId();
        id.setIdProyecto(proyecto.getIdProyecto());
        id.setIdUsuario(usuario.getIdUsuario());
        ProyectoUsuario miembro = new ProyectoUsuario();
        miembro.setId(id);
        miembro.setProyecto(proyecto);
        miembro.setUsuario(usuario);
        miembro.setRol(usuario.getIdUsuario().equals(responsable.getIdUsuario())
                ? ROL_RESPONSABLE : ROL_INTEGRANTE);
        miembro.setEstado("Activo");
        miembro.setFechaAsignacion(fecha);
        return miembro;
    }

    private void copiarDatos(Proyecto proyecto, ProyectoRequest r) {
        if (r.prioridad() == null || !PRIORIDADES.contains(r.prioridad())) {
            throw new SolicitudInvalidaException("Prioridad de proyecto no válida.");
        }
        proyecto.setNombre(r.nombre().trim());
        proyecto.setDescripcion(r.descripcion().trim());
        proyecto.setFechaInicio(r.fechaInicio());
        proyecto.setFechaFin(r.fechaFin());
        proyecto.setPrioridad(r.prioridad());
    }

    private void validarPeriodo(ProyectoRequest r) {
        if (r.fechaFin().isBefore(r.fechaInicio())) {
            throw new SolicitudInvalidaException(
                    "La fecha de fin no puede ser anterior a la fecha de inicio.");
        }
    }

    private String estadoValido(String estado, String porDefecto) {
        if (estado == null || estado.isBlank()) return porDefecto;
        if (!ESTADOS.contains(estado)) {
            throw new SolicitudInvalidaException("Estado de proyecto no válido.");
        }
        return estado;
    }

    private Float progresoValido(Integer progreso, int porDefecto) {
        int valor = progreso == null ? porDefecto : progreso;
        if (valor < 0 || valor > 100) {
            throw new SolicitudInvalidaException("El progreso debe estar entre 0 y 100.");
        }
        return (float) valor;
    }

    private ProyectoDto aDto(Proyecto p) {
        List<ProyectoUsuario> equipo = proyectoUsuarioRepositorio.buscarPorProyecto(p.getIdProyecto());
        String responsable = p.getResponsable().getNombre();
        List<String> integrantes = equipo.stream()
                .map(pu -> pu.getUsuario().getNombre()).toList();
        int progreso = p.getPorcentajeAvance() == null ? 0 : Math.round(p.getPorcentajeAvance());
        return new ProyectoDto(p.getIdProyecto(), p.getNombre(), p.getDescripcion(),
                p.getFechaInicio(), p.getFechaFin(), p.getPrioridad(),
                responsable, integrantes, p.getEstado(), progreso,
                p.getResponsable().getIdUsuario(), equipo.stream()
                        .map(pu -> pu.getUsuario().getIdUsuario()).toList());
    }
}
