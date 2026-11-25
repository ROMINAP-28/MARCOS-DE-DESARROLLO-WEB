package com.gestionproyectos.Controller;

import com.gestionproyectos.dto.ProyectoDto;
import com.gestionproyectos.dto.ProyectoRequest;
import com.gestionproyectos.service.IProyectoServicio;
import com.gestionproyectos.service.AvisosTiempoReal;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
// http://localhost:8080/api/miembro3/proyectos
@RequestMapping("/api/miembro3")
public class ProyectoController {

    private static final Logger logger = LoggerFactory.getLogger(ProyectoController.class);

    private final IProyectoServicio proyectoServicio;
    private final AvisosTiempoReal avisos;

    public ProyectoController(IProyectoServicio proyectoServicio, AvisosTiempoReal avisos) {
        this.proyectoServicio = proyectoServicio;
        this.avisos = avisos;
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
    @PreAuthorize("@accesoProyecto.puedeCrearProyecto(#request)")
    @ResponseStatus(HttpStatus.CREATED)
    public ProyectoDto agregarProyecto(@Valid @RequestBody ProyectoRequest request) {
        logger.info("Proyecto a agregar: {}", request.nombre());
        ProyectoDto proyecto = proyectoServicio.guardarProyecto(request);
        avisos.proyectoCambiado();
        avisos.equipoCambiado();
        return proyecto;
    }

    // PUT http://localhost:8080/api/miembro3/proyectos/1
    @PutMapping("/proyectos/{id}")
    @PreAuthorize("@accesoProyecto.esResponsable(#id)")
    public ProyectoDto actualizarProyecto(@PathVariable Integer id,
                                          @Valid @RequestBody ProyectoRequest request) {
        ProyectoDto proyecto = proyectoServicio.actualizarProyecto(id, request);
        avisos.proyectoCambiado();
        avisos.equipoCambiado();
        return proyecto;
    }

    // DELETE http://localhost:8080/api/miembro3/proyectos/1
    @DeleteMapping("/proyectos/{id}")
    @PreAuthorize("@accesoProyecto.esResponsable(#id)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarProyecto(@PathVariable Integer id) {
        proyectoServicio.eliminarProyecto(id);
        avisos.proyectoCambiado();
        avisos.tareaCambiada();
        avisos.equipoCambiado();
    }
}
