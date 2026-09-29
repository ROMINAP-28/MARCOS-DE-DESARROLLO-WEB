
package com.gestionproyectos.Controller;

import com.gestionproyectos.model.Usuario;
import com.gestionproyectos.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public record RegistroRequest(
            String nombre,
            String correo,
            String contrasena
    ) {}

    @PostMapping("/registro")
    public ResponseEntity<Map<String, String>> registrar(
            @RequestBody RegistroRequest request) {

        if (request.nombre() == null
                || request.correo() == null
                || request.contrasena() == null
                || request.nombre().isBlank()
                || request.correo().isBlank()
                || request.contrasena().isBlank()) {
            return ResponseEntity.badRequest().body(
                    Map.of("mensaje", "Completa todos los campos.")
            );
        }

        String nombre = request.nombre().trim();
        String correo = request.correo().trim().toLowerCase();

        if (nombre.length() > 100 || correo.length() > 150) {
            return ResponseEntity.badRequest().body(
                    Map.of("mensaje", "El nombre o correo es demasiado largo.")
            );
        }

        if (!correo.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            return ResponseEntity.badRequest().body(
                    Map.of("mensaje", "Ingresa un correo válido.")
            );
        }

        if (request.contrasena().length() < 8
                || request.contrasena().length() > 72) {
            return ResponseEntity.badRequest().body(
                    Map.of("mensaje",
                            "La contraseña debe tener entre 8 y 72 caracteres.")
            );
        }

        if (usuarioRepository.existsByEmail(correo)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    Map.of("mensaje",
                            "Este correo ya está registrado.")
            );
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(nombre);
        usuario.setEmail(correo);
        usuario.setPassword(
                passwordEncoder.encode(request.contrasena())
        );
        usuario.setRol("USUARIO");
        usuario.setNotifTareas(true);
        usuario.setRecordatoriosFechas(true);
        usuario.setMencionesComentarios(true);
        usuario.setFechaRegistro(LocalDateTime.now());

        usuarioRepository.save(usuario);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                Map.of("mensaje",
                        "Cuenta creada correctamente. Ya puedes iniciar sesión.")
        );
    }
}