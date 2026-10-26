package com.gestionproyectos.Controller;

import com.gestionproyectos.dto.EstadoRequest;
import com.gestionproyectos.dto.TareaDto;
import com.gestionproyectos.dto.TareaRequest;
import com.gestionproyectos.service.ITareaServicio;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
// http://localhost:8080/api/miembro3/tareas
@RequestMapping("/api/miembro3")
@CrossOrigin(value = "http://localhost:3000")
public class TareaController {

    private static final Logger logger = LoggerFactory.getLogger(TareaController.class);

    private final ITareaServicio tareaServicio;

    public TareaController(ITareaServicio tareaServicio) {
        this.tareaServicio = tareaServicio;
    }

    // GET http://localhost:8080/api/miembro3/tareas
    @GetMapping("/tareas")
    public List<TareaDto> obtenerTareas() {
        List<TareaDto> tareas = tareaServicio.listarTareas();
        logger.info("Tareas encontradas: {}", tareas.size());
        return tareas;
    }

    // GET http://localhost:8080/api/miembro3/tareas/1
    @GetMapping("/tareas/{id}")
    public TareaDto obtenerTareaPorId(@PathVariable Integer id) {
        return tareaServicio.buscarTareaPorId(id);
    }

    // POST http://localhost:8080/api/miembro3/tareas
    @PostMapping("/tareas")
    @ResponseStatus(HttpStatus.CREATED)
    public TareaDto agregarTarea(@Valid @RequestBody TareaRequest request) {
        logger.info("Tarea a agregar: {}", request.nombre());
        return tareaServicio.guardarTarea(request);
    }

    // PUT http://localhost:8080/api/miembro3/tareas/1
    @PutMapping("/tareas/{id}")
    public TareaDto actualizarTarea(@PathVariable Integer id,
                                    @Valid @RequestBody TareaRequest request) {
        return tareaServicio.actualizarTarea(id, request);
    }

    // PATCH http://localhost:8080/api/miembro3/tareas/1/estado   (lo usa el tablero Kanban)
    @PatchMapping("/tareas/{id}/estado")
    public TareaDto cambiarEstado(@PathVariable Integer id, @RequestBody EstadoRequest request) {
        return tareaServicio.cambiarEstado(id, request.estado());
    }

    // DELETE http://localhost:8080/api/miembro3/tareas/1
    @DeleteMapping("/tareas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarTarea(@PathVariable Integer id) {
        tareaServicio.eliminarTarea(id);
    }
}
