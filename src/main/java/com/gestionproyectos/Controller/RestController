package com.gestionproyectos.Controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping()
public class RestController {

    private final AtomicLong proyectoId = new AtomicLong(3);
    private final AtomicLong tareaId = new AtomicLong(5);

    private final List<ProyectoDto> proyectos = new ArrayList<>(List.of(
            new ProyectoDto(1L, "Plataforma web colaborativa",
                    "Sistema para organizar equipos, tareas y proyectos.",
                    "2026-09-10", "2026-10-30", "Alta", "María López",
                    List.of("María López", "Carlos Torres", "Ana García"),
                    "En progreso", 65),
            new ProyectoDto(2L, "Sistema de inventario",
                    "Aplicación para administrar productos y existencias.",
                    "2026-09-15", "2026-11-15", "Media", "Carlos Torres",
                    List.of("Carlos Torres", "José Ramírez"),
                    "Planificado", 0),
            new ProyectoDto(3L, "Portal de reportes",
                    "Panel de consulta y generación de informes.",
                    "2026-08-01", "2026-09-20", "Baja", "Ana García",
                    List.of("Ana García", "Brayan Quispe"),
                    "Completado", 100)
    ));

    private final List<TareaDto> tareas = new ArrayList<>(List.of(
            new TareaDto(1L, "Diseñar la interfaz",
                    "Preparar las vistas principales del sistema.",
                    "Plataforma web colaborativa", "Alta", "Pendiente",
                    "2026-09-30", "Ana Torres"),
            new TareaDto(2L, "Conectar base de datos",
                    "Configurar la conexión con PostgreSQL.",
                    "Plataforma web colaborativa", "Urgente", "En progreso",
                    "2026-10-02", "Carlos Ruiz"),
            new TareaDto(3L, "Revisar requisitos",
                    "Validar los requisitos funcionales.",
                    "Sistema de inventario", "Media", "Pendiente",
                    "2026-10-05", "María López"),
            new TareaDto(4L, "Crear estructura HTML",
                    "Implementar la estructura inicial de las páginas.",
                    "Plataforma web colaborativa", "Baja", "Completada",
                    "2026-09-25", "Juan Pérez"),
            new TareaDto(5L, "Implementar autenticación",
                    "Preparar el inicio de sesión de usuarios.",
                    "Portal de reportes", "Alta", "En progreso",
                    "2026-10-08", "Ana Torres")
    ));

    @GetMapping("/proyectos")
    public List<ProyectoDto> listarProyectos(@RequestParam(required = false) String q) {
        if (q == null || q.isBlank()) return proyectos;
        String termino = q.toLowerCase();
        return proyectos.stream()
                .filter(p -> p.nombre().toLowerCase().contains(termino)
                        || p.descripcion().toLowerCase().contains(termino))
                .toList();
    }

    @GetMapping("/proyectos/{id}")
    public ProyectoDto obtenerProyecto(@PathVariable Long id) {
        return proyectos.stream()
                .filter(p -> p.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado"));
    }

    @PostMapping("/proyectos")
    @ResponseStatus(HttpStatus.CREATED)
    public ProyectoDto crearProyecto(@RequestBody ProyectoRequest request) {
        validarProyecto(request);
        ProyectoDto nuevo = new ProyectoDto(
                proyectoId.incrementAndGet(), request.nombre(), request.descripcion(),
                request.fechaInicio(), request.fechaFin(), request.prioridad(),
                request.responsable(), request.integrantes() == null ? List.of() : request.integrantes(),
                "Planificado", 0);
        proyectos.add(0, nuevo);
        return nuevo;
    }

    @GetMapping("/tareas")
    public List<TareaDto> listarTareas() {
        return tareas;
    }

    @PostMapping("/tareas")
    @ResponseStatus(HttpStatus.CREATED)
    public TareaDto crearTarea(@RequestBody TareaDto request) {
        validarTarea(request);
        TareaDto nueva = new TareaDto(
                tareaId.incrementAndGet(), request.nombre(), request.descripcion(),
                request.proyecto(), request.prioridad(),
                request.estado() == null || request.estado().isBlank() ? "Pendiente" : request.estado(),
                request.fechaFin(), request.responsable());
        tareas.add(0, nueva);
        return nueva;
    }

    @PutMapping("/tareas/{id}")
    public TareaDto actualizarTarea(@PathVariable Long id, @RequestBody TareaDto request) {
        validarTarea(request);
        int indice = indiceTarea(id);
        TareaDto actualizada = new TareaDto(id, request.nombre(), request.descripcion(),
                request.proyecto(), request.prioridad(), request.estado(),
                request.fechaFin(), request.responsable());
        tareas.set(indice, actualizada);
        return actualizada;
    }

    @PatchMapping("/tareas/{id}/estado")
    public TareaDto cambiarEstado(@PathVariable Long id, @RequestBody EstadoRequest request) {
        if (request.estado() == null || !List.of("Pendiente", "En progreso", "Completada").contains(request.estado())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado no válido");
        }
        int indice = indiceTarea(id);
        TareaDto anterior = tareas.get(indice);
        TareaDto actualizada = new TareaDto(anterior.id(), anterior.nombre(), anterior.descripcion(),
                anterior.proyecto(), anterior.prioridad(), request.estado(),
                anterior.fechaFin(), anterior.responsable());
        tareas.set(indice, actualizada);
        return actualizada;
    }

    @DeleteMapping("/tareas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarTarea(@PathVariable Long id) {
        int indice = indiceTarea(id);
        tareas.remove(indice);
    }

    private int indiceTarea(Long id) {
        for (int i = 0; i < tareas.size(); i++) {
            if (tareas.get(i).id().equals(id)) return i;
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarea no encontrada");
    }

    private void validarProyecto(ProyectoRequest request) {
        if (request.nombre() == null || request.nombre().isBlank()
                || request.descripcion() == null || request.descripcion().isBlank()
                || request.fechaInicio() == null || request.fechaFin() == null
                || request.prioridad() == null || request.responsable() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Completa los campos obligatorios del proyecto");
        }
    }

    private void validarTarea(TareaDto request) {
        if (request.nombre() == null || request.nombre().isBlank()
                || request.proyecto() == null || request.proyecto().isBlank()
                || request.prioridad() == null || request.fechaFin() == null
                || request.responsable() == null || request.responsable().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Completa los campos obligatorios de la tarea");
        }
    }

    public record ProyectoDto(Long id, String nombre, String descripcion,
                              String fechaInicio, String fechaFin, String prioridad,
                              String responsable, List<String> integrantes,
                              String estado, int progreso) {}

    public record ProyectoRequest(String nombre, String descripcion,
                                  String fechaInicio, String fechaFin,
                                  String prioridad, String responsable,
                                  List<String> integrantes) {}

    public record TareaDto(Long id, String nombre, String descripcion,
                           String proyecto, String prioridad, String estado,
                           String fechaFin, String responsable) {}

    public record EstadoRequest(String estado) {}
}
