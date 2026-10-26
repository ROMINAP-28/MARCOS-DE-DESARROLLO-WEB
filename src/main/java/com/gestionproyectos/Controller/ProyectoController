package com.gestionproyectos.Controller;

import com.gestionproyectos.dto.ProyectoDto;
import com.gestionproyectos.dto.ProyectoRequest;
import com.gestionproyectos.service.IProyectoServicio;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
// http://localhost:8080/api/miembro3/proyectos
@RequestMapping("/api/miembro3")
@CrossOrigin(value = "http://localhost:3000")
public class ProyectoController {

    private static final Logger logger = LoggerFactory.getLogger(ProyectoController.class);

    private final IProyectoServicio proyectoServicio;

    public ProyectoController(IProyectoServicio proyectoServicio) {
        this.proyectoServicio = proyectoServicio;
    }

    // GET http://localhost:8080/api/miembro3/proyectos?q=texto
    @GetMapping("/proyectos")
    public List<ProyectoDto> obtenerProyectos(@RequestParam(required = false) String q) {
        List<ProyectoDto> proyectos = proyectoServicio.listarProyectos(q);
        logger.info("Proyectos encontrados: {}", proyectos.size());
        return proyectos;
    }

    // GET http://localhost:8080/api/miembro3/proyectos/1
    @GetMapping("/proyectos/{id}")
    public ProyectoDto obtenerProyectoPorId(@PathVariable Integer id) {
        return proyectoServicio.buscarProyectoPorId(id);
    }

    // POST http://localhost:8080/api/miembro3/proyectos
    @PostMapping("/proyectos")
    @ResponseStatus(HttpStatus.CREATED)
    public ProyectoDto agregarProyecto(@Valid @RequestBody ProyectoRequest request) {
        logger.info("Proyecto a agregar: {}", request.nombre());
        return proyectoServicio.guardarProyecto(request);
    }

    // PUT http://localhost:8080/api/miembro3/proyectos/1
    @PutMapping("/proyectos/{id}")
    public ProyectoDto actualizarProyecto(@PathVariable Integer id,
                                          @Valid @RequestBody ProyectoRequest request) {
        return proyectoServicio.actualizarProyecto(id, request);
    }

    // DELETE http://localhost:8080/api/miembro3/proyectos/1
    @DeleteMapping("/proyectos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarProyecto(@PathVariable Integer id) {
        proyectoServicio.eliminarProyecto(id);
    }
}
