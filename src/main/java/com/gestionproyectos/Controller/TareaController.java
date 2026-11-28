package com.gestionproyectos.Controller;

import com.gestionproyectos.Reportes.ExportadorExcel;
import com.gestionproyectos.Reportes.ExportadorPdf;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.Arrays;
import com.gestionproyectos.config.AlcanceLectura;
import com.gestionproyectos.dto.EstadoRequest;
import com.gestionproyectos.dto.TareaDto;
import com.gestionproyectos.dto.TareaRequest;
import com.gestionproyectos.service.ITareaServicio;
import com.gestionproyectos.service.AvisosTiempoReal;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.Set;

@RestController
// http://localhost:8080/api/miembro3/tareas
@RequestMapping("/api/miembro3")
public class TareaController {

    private static final Logger logger = LoggerFactory.getLogger(TareaController.class);

    private final ITareaServicio tareaServicio;
    private final AvisosTiempoReal avisos;
    private final AlcanceLectura alcance;

    public TareaController(ITareaServicio tareaServicio, AvisosTiempoReal avisos,
            AlcanceLectura alcance) {
        this.tareaServicio = tareaServicio;
        this.avisos = avisos;
        this.alcance = alcance;
    }

    // GET http://localhost:8080/api/miembro3/tareas
    @GetMapping("/tareas")
    public List<TareaDto> obtenerTareas() {
        Set<Integer> visibles = alcance.idsProyectosVisibles();
        List<TareaDto> tareas = tareaServicio.listarTareas().stream()
                .filter(t -> visibles.contains(t.idProyecto())).toList();
        logger.info("Tareas encontradas: {}", tareas.size());
        return tareas;
    }

    // GET http://localhost:8080/api/miembro3/tareas/1
    @GetMapping("/tareas/{id}")
    @PreAuthorize("@accesoProyecto.puedeVerTarea(#id)")
    public TareaDto obtenerTareaPorId(@PathVariable Integer id) {
        return tareaServicio.buscarTareaPorId(id);
    }

    // POST http://localhost:8080/api/miembro3/tareas
    @PostMapping("/tareas")
    @PreAuthorize("@accesoProyecto.puedeCrearTarea(#request)")
    @ResponseStatus(HttpStatus.CREATED)
    public TareaDto agregarTarea(@Valid @RequestBody TareaRequest request) {
        logger.info("Tarea a agregar: {}", request.nombre());
        TareaDto tarea = tareaServicio.guardarTarea(request);
        avisos.tareaCambiada();
        avisos.tareaAsignada(tarea.idResponsable(), tarea.id());
        return tarea;
    }

    // PUT http://localhost:8080/api/miembro3/tareas/1
    @PutMapping("/tareas/{id}")
    @PreAuthorize("@accesoProyecto.esResponsableDeTarea(#id) && @accesoProyecto.puedeCrearTarea(#request)")
    public TareaDto actualizarTarea(@PathVariable Integer id,
            @Valid @RequestBody TareaRequest request) {
        Integer responsableAnterior = tareaServicio.buscarTareaPorId(id).idResponsable();
        TareaDto tarea = tareaServicio.actualizarTarea(id, request);
        avisos.tareaCambiada();
        if (!Objects.equals(responsableAnterior, tarea.idResponsable())) {
            avisos.tareaAsignada(tarea.idResponsable(), tarea.id());
        }
        return tarea;
    }

    // PATCH http://localhost:8080/api/miembro3/tareas/1/estado   (lo usa el tablero Kanban)
    @PatchMapping("/tareas/{id}/estado")
    @PreAuthorize("@accesoProyecto.puedeCambiarEstado(#id)")
    public TareaDto cambiarEstado(@PathVariable Integer id, @Valid @RequestBody EstadoRequest request) {
        TareaDto tarea = tareaServicio.cambiarEstado(id, request.estado());
        avisos.tareaCambiada();
        return tarea;
    }

    // DELETE http://localhost:8080/api/miembro3/tareas/1
    @DeleteMapping("/tareas/{id}")
    @PreAuthorize("@accesoProyecto.esResponsableDeTarea(#id)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarTarea(@PathVariable Integer id) {
        tareaServicio.eliminarTarea(id);
        avisos.tareaCambiada();
    }

    @GetMapping("/tareas/excel")
    public ResponseEntity<byte[]> exportarTareasExcel() throws IOException {
        Set<Integer> visibles = alcance.idsProyectosVisibles();

        List<TareaDto> tareas = tareaServicio.listarTareas().stream()
                .filter(t -> visibles.contains(t.idProyecto()))
                .toList();

        List<String> encabezados = Arrays.asList(
                "ID", "Tarea", "Descripción", "Proyecto",
                "Prioridad", "Estado", "Fecha de inicio",
                "Fecha de fin", "Responsable"
        );

        List<List<Object>> filas = tareas.stream()
                .map(t -> Arrays.<Object>asList(
                        t.id(), t.nombre(), t.descripcion(), t.proyecto(),
                        t.prioridad(), t.estado(), t.fechaInicio(),
                        t.fechaFin(), t.responsable()
                ))
                .toList();

        byte[] archivo = new ExportadorExcel()
                .generar("Tareas", encabezados, filas);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"tareas.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(archivo);
    }

    @GetMapping("/tareas/pdf")
    public ResponseEntity<byte[]> exportarTareasPdf() {
        Set<Integer> visibles = alcance.idsProyectosVisibles();

        List<TareaDto> tareas = tareaServicio.listarTareas().stream()
                .filter(t -> visibles.contains(t.idProyecto()))
                .toList();

        List<String> encabezados = Arrays.asList(
                "ID", "Tarea", "Descripción", "Proyecto",
                "Prioridad", "Estado", "Fecha de inicio",
                "Fecha de fin", "Responsable"
        );

        List<List<Object>> filas = tareas.stream()
                .map(t -> Arrays.<Object>asList(
                        t.id(), t.nombre(), t.descripcion(), t.proyecto(),
                        t.prioridad(), t.estado(), t.fechaInicio(),
                        t.fechaFin(), t.responsable()
                ))
                .toList();

        byte[] archivo = new ExportadorPdf()
                .generar("Reporte de tareas", encabezados, filas);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"tareas.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(archivo);
    }
}
