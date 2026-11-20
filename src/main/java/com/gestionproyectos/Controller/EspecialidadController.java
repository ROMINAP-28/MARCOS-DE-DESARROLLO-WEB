package com.gestionproyectos.Controller;

import com.gestionproyectos.dto.EspecialidadDto;
import com.gestionproyectos.service.IEspecialidadServicio;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
// http://localhost:8080/api/miembro3/especialidades
@RequestMapping("/api/miembro3/especialidades")
public class EspecialidadController {

    private final IEspecialidadServicio especialidadServicio;

    public EspecialidadController(IEspecialidadServicio especialidadServicio) {
        this.especialidadServicio = especialidadServicio;
    }

    @GetMapping
    public List<EspecialidadDto> obtenerEspecialidades() {
        return especialidadServicio.listarEspecialidades();
    }

    @GetMapping("/{id}")
    public EspecialidadDto obtenerEspecialidadPorId(@PathVariable Integer id) {
        return especialidadServicio.buscarEspecialidadPorId(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or @accesoProyecto.esResponsableDeAlgunProyecto()")
    @ResponseStatus(HttpStatus.CREATED)
    public EspecialidadDto agregarEspecialidad(@Valid @RequestBody EspecialidadDto especialidad) {
        return especialidadServicio.guardarEspecialidad(especialidad);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public EspecialidadDto actualizarEspecialidad(@PathVariable Integer id,
                                                  @Valid @RequestBody EspecialidadDto especialidad) {
        return especialidadServicio.actualizarEspecialidad(id, especialidad);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarEspecialidad(@PathVariable Integer id) {
        especialidadServicio.eliminarEspecialidad(id);
    }
}
