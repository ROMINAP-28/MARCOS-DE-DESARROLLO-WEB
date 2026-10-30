package com.gestionproyectos.Controller;

import com.gestionproyectos.dto.RegistroRequest;
import com.gestionproyectos.service.IUsuarioServicio;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final IUsuarioServicio usuarioServicio;

    public AuthController(IUsuarioServicio usuarioServicio) {
        this.usuarioServicio = usuarioServicio;
    }

    @PostMapping("/registro")
    public ResponseEntity<Map<String, String>> registrar(@Valid @RequestBody RegistroRequest request) {
        usuarioServicio.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                Map.of("mensaje", "Cuenta creada correctamente. Ya puedes iniciar sesión."));
    }
}
