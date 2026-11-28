package com.gestionproyectos.Controller;

import com.gestionproyectos.Reportes.ExportadorExcel;
import com.gestionproyectos.Reportes.ExportadorPdf;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.Arrays;
import com.gestionproyectos.dto.UsuarioDto;
import com.gestionproyectos.dto.UsuarioActualizarRequest;
import com.gestionproyectos.service.IUsuarioServicio;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
// El alta se realiza en /api/auth/registro.
@RequestMapping("/api/miembro3/usuarios")
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
    @PreAuthorize("@accesoProyecto.esUsuario(#id)")
    public UsuarioDto actualizarUsuario(@PathVariable Integer id,
            @Valid @RequestBody UsuarioActualizarRequest request) {
        return usuarioServicio.actualizarUsuario(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@accesoProyecto.esUsuario(#id)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarUsuario(@PathVariable Integer id, HttpServletRequest request,
            HttpServletResponse response, Authentication authentication) {
        usuarioServicio.eliminarUsuario(id);
        new SecurityContextLogoutHandler().logout(request, response, authentication);
    }

    //Generador de Reportes 
    @GetMapping("/reporte/excel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportarUsuariosExcel() throws IOException {
        List<UsuarioDto> usuarios = usuarioServicio.listarUsuarios();

        List<String> encabezados = Arrays.asList(
                "ID", "Nombre", "Correo", "Teléfono",
                "Rol", "Especialidad", "Fecha de registro"
        );

        List<List<Object>> filas = usuarios.stream()
                .map(u -> Arrays.<Object>asList(
                        u.id(),
                        u.nombre(),
                        u.correo(),
                        u.telefono(),
                        u.rol(),
                        u.especialidad(),
                        u.fechaRegistro()
                ))
                .toList();

        byte[] archivo = new ExportadorExcel()
                .generar("Usuarios", encabezados, filas);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"usuarios.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(archivo);
    }

    @GetMapping("/reporte/pdf")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportarUsuariosPdf() {
        List<UsuarioDto> usuarios = usuarioServicio.listarUsuarios();

        List<String> encabezados = Arrays.asList(
                "ID", "Nombre", "Correo", "Teléfono",
                "Rol", "Especialidad", "Fecha de registro"
        );

        List<List<Object>> filas = usuarios.stream()
                .map(u -> Arrays.<Object>asList(
                        u.id(),
                        u.nombre(),
                        u.correo(),
                        u.telefono(),
                        u.rol(),
                        u.especialidad(),
                        u.fechaRegistro()
                ))
                .toList();

        byte[] archivo = new ExportadorPdf()
                .generar("Reporte de usuarios", encabezados, filas);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"usuarios.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(archivo);
    }
}
