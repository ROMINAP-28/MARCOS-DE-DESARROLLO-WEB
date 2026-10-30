package com.gestionproyectos.Controller;

import com.gestionproyectos.dto.UsuarioDto;
import com.gestionproyectos.dto.UsuarioActualizarRequest;
import com.gestionproyectos.service.IUsuarioServicio;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
// El alta se realiza en /api/auth/registro.
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

    @PutMapping("/{id}")
    public UsuarioDto actualizarUsuario(@PathVariable Integer id,
                                        @Valid @RequestBody UsuarioActualizarRequest request) {
        return usuarioServicio.actualizarUsuario(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarUsuario(@PathVariable Integer id) {
        usuarioServicio.eliminarUsuario(id);
    }
}
