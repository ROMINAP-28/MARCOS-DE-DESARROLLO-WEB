package com.gestionproyectos.Controller;

import com.gestionproyectos.dto.UsuarioDto;
import com.gestionproyectos.service.IUsuarioServicio;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
// http://localhost:8080/api/miembro3/usuarios  (solo lectura; el alta se hace en /api/auth/registro)
@RequestMapping("/api/miembro3/usuarios")
@CrossOrigin(value = "http://localhost:3000")
public class UsuarioController {

    private final IUsuarioServicio usuarioServicio;

    public UsuarioController(IUsuarioServicio usuarioServicio) {
        this.usuarioServicio = usuarioServicio;
    }

    @GetMapping
    public List<UsuarioDto> obtenerUsuarios() {
        return usuarioServicio.listarUsuarios();
    }

    @GetMapping("/{id}")
    public UsuarioDto obtenerUsuarioPorId(@PathVariable Integer id) {
        return usuarioServicio.buscarUsuarioPorId(id);
    }
}
