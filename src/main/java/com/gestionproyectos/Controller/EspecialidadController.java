package com.gestionproyectos.Controller;

import com.gestionproyectos.dto.EspecialidadDto;
import com.gestionproyectos.service.IEspecialidadServicio;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
// http://localhost:8080/api/miembro3/especialidades
@RequestMapping("/api/miembro3/especialidades")
@CrossOrigin(value = "http://localhost:3000")
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
    @ResponseStatus(HttpStatus.CREATED)
    public EspecialidadDto agregarEspecialidad(@Valid @RequestBody EspecialidadDto especialidad) {
        return especialidadServicio.guardarEspecialidad(especialidad);
    }

    @PutMapping("/{id}")
    public EspecialidadDto actualizarEspecialidad(@PathVariable Integer id,
                                                  @Valid @RequestBody EspecialidadDto especialidad) {
        return especialidadServicio.actualizarEspecialidad(id, especialidad);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarEspecialidad(@PathVariable Integer id) {
        especialidadServicio.eliminarEspecialidad(id);
    }
}
