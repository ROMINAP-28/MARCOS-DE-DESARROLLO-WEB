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

    private final ProyectoRepository proyectoRepositorio;
    private final ProyectoUsuarioRepository proyectoUsuarioRepositorio;
    private final UsuarioRepository usuarioRepositorio;

    public ProyectoServicio(ProyectoRepository proyectoRepositorio,
                            ProyectoUsuarioRepository proyectoUsuarioRepositorio,
                            UsuarioRepository usuarioRepositorio) {
        this.proyectoRepositorio = proyectoRepositorio;
        this.proyectoUsuarioRepositorio = proyectoUsuarioRepositorio;
        this.usuarioRepositorio = usuarioRepositorio;
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
        Usuario responsable = buscarUsuario(request.responsable());
        Map<Integer, Usuario> equipo = armarEquipo(responsable, request.integrantes());

        Proyecto proyecto = new Proyecto();
        copiarDatos(proyecto, request);
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
        Usuario responsable = buscarUsuario(request.responsable());
        Map<Integer, Usuario> equipo = armarEquipo(responsable, request.integrantes());

        copiarDatos(proyecto, request);
        proyecto.setEstado(estadoValido(request.estado(), proyecto.getEstado()));
        proyecto.setPorcentajeAvance(progresoValido(request.progreso(),
                proyecto.getPorcentajeAvance() == null ? 0 : Math.round(proyecto.getPorcentajeAvance())));
        proyectoRepositorio.saveAndFlush(proyecto);

        // Reemplaza el equipo completo (el delete masivo ejecuta flush + clear).
        proyectoUsuarioRepositorio.eliminarPorProyecto(idProyecto);
        asignarEquipo(proyectoRepositorio.getReferenceById(idProyecto), responsable, equipo);
        return aDto(obtener(idProyecto));
    }

    @Override
    public void eliminarProyecto(Integer idProyecto) {
        // cascade = ALL en el modelo borra también equipo, tareas, comentarios y notificaciones.
        proyectoRepositorio.delete(obtener(idProyecto));
    }

    // ---------------- helpers ----------------

    private Proyecto obtener(Integer idProyecto) {
        return proyectoRepositorio.findById(idProyecto)
                .orElseThrow(() -> new RecursoNoEncontradoException("Proyecto no encontrado"));
    }

    private Usuario buscarUsuario(String nombre) {
        return usuarioRepositorio.findFirstByNombreIgnoreCase(nombre.trim())
                .orElseThrow(() -> new SolicitudInvalidaException(
                        "No existe el usuario \"" + nombre + "\"."));
    }

    /** Responsable primero, luego integrantes sin repetir. */
    private Map<Integer, Usuario> armarEquipo(Usuario responsable, List<String> integrantes) {
        Map<Integer, Usuario> equipo = new LinkedHashMap<>();
        equipo.put(responsable.getIdUsuario(), responsable);
        if (integrantes != null) {
            for (String nombre : integrantes) {
                if (nombre == null || nombre.isBlank()) continue;
                Usuario u = buscarUsuario(nombre);
                equipo.putIfAbsent(u.getIdUsuario(), u);
            }
        }
        return equipo;
    }

    private void asignarEquipo(Proyecto proyecto, Usuario responsable, Map<Integer, Usuario> equipo) {
        LocalDateTime ahora = LocalDateTime.now();
        for (Usuario usuario : equipo.values()) {
            ProyectoUsuarioId id = new ProyectoUsuarioId();
            id.setIdProyecto(proyecto.getIdProyecto());
            id.setIdUsuario(usuario.getIdUsuario());

            ProyectoUsuario pu = new ProyectoUsuario();
            pu.setId(id);
            pu.setProyecto(proyecto);
            pu.setUsuario(usuario);
            pu.setRol(usuario.getIdUsuario().equals(responsable.getIdUsuario())
                    ? ROL_RESPONSABLE : ROL_INTEGRANTE);
            pu.setEstado("Activo");
            pu.setFechaAsignacion(ahora);
            proyectoUsuarioRepositorio.save(pu);
        }
    }

    private void copiarDatos(Proyecto proyecto, ProyectoRequest r) {
        proyecto.setNombre(r.nombre().trim());
        proyecto.setDescripcion(r.descripcion().trim());
        proyecto.setFechaInicio(r.fechaInicio());
        proyecto.setFechaFin(r.fechaFin());
        proyecto.setPrioridad(r.prioridad().trim());
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
        String responsable = equipo.stream()
                .filter(pu -> ROL_RESPONSABLE.equalsIgnoreCase(pu.getRol()))
                .map(pu -> pu.getUsuario().getNombre())
                .findFirst().orElse("");
        List<String> integrantes = equipo.stream()
                .map(pu -> pu.getUsuario().getNombre()).toList();
        int progreso = p.getPorcentajeAvance() == null ? 0 : Math.round(p.getPorcentajeAvance());
        return new ProyectoDto(p.getIdProyecto(), p.getNombre(), p.getDescripcion(),
                p.getFechaInicio(), p.getFechaFin(), p.getPrioridad(),
                responsable, integrantes, p.getEstado(), progreso);
    }
}
