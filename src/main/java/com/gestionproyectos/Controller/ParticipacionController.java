package com.gestionproyectos.Controller;

import com.gestionproyectos.dto.EstadoParticipacionRequest;
import com.gestionproyectos.dto.ParticipacionDto;
import com.gestionproyectos.dto.ParticipacionRequest;
import com.gestionproyectos.service.ParticipacionServicio;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/miembro3/participaciones")
public class ParticipacionController {
    private final ParticipacionServicio servicio;

    public ParticipacionController(ParticipacionServicio servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<ParticipacionDto> listar() {
        return servicio.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipacionDto agregar(@Valid @RequestBody ParticipacionRequest request) {
        return servicio.agregar(request);
    }

    @PutMapping("/{idProyecto}/{idUsuario}")
    public ParticipacionDto cambiarEstado(@PathVariable Integer idProyecto, @PathVariable Integer idUsuario,
                                          @Valid @RequestBody EstadoParticipacionRequest request) {
        return servicio.cambiarEstado(idProyecto, idUsuario, request);
    }

    @DeleteMapping("/{idProyecto}/{idUsuario}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void retirar(@PathVariable Integer idProyecto, @PathVariable Integer idUsuario) {
        servicio.retirar(idProyecto, idUsuario);
    }
}
