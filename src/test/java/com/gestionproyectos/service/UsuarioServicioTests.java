package com.gestionproyectos.service;

import com.gestionproyectos.dto.RegistroRequest;
import com.gestionproyectos.dto.UsuarioActualizarRequest;
import com.gestionproyectos.exception.RecursoDuplicadoException;
import com.gestionproyectos.model.Especialidad;
import com.gestionproyectos.model.Usuario;
import com.gestionproyectos.repository.EspecialidadRepository;
import com.gestionproyectos.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UsuarioServicioTests {

    private UsuarioRepository usuarios;
    private EspecialidadRepository especialidades;
    private PasswordEncoder encoder;
    private UsuarioServicio servicio;

    @BeforeEach
    void preparar() {
        usuarios = mock(UsuarioRepository.class);
        especialidades = mock(EspecialidadRepository.class);
        encoder = mock(PasswordEncoder.class);
        servicio = new UsuarioServicio(usuarios, especialidades, encoder);
    }

    @Test
    void registroNormalizaCorreoYGuardaSoloHash() {
        when(encoder.encode("clave-segura")).thenReturn("hash-de-prueba");

        servicio.registrar(new RegistroRequest(" Ana ", "ANA@EJEMPLO.COM", "clave-segura"));

        var captor = org.mockito.ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).save(captor.capture());
        Usuario guardado = captor.getValue();
        assertEquals("Ana", guardado.getNombre());
        assertEquals("ana@ejemplo.com", guardado.getEmail());
        assertEquals("hash-de-prueba", guardado.getPassword());
        assertEquals("USUARIO", guardado.getRol());
    }

    @Test
    void actualizarNoCambiaContrasenaNiRol() {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(1);
        usuario.setPassword("hash-original");
        usuario.setRol("USUARIO");
        Especialidad especialidad = new Especialidad();
        especialidad.setNombreEspecialidad("Backend");
        when(usuarios.findById(1)).thenReturn(Optional.of(usuario));
        when(especialidades.findById(2)).thenReturn(Optional.of(especialidad));
        when(usuarios.save(any(Usuario.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        var resultado = servicio.actualizarUsuario(1,
                new UsuarioActualizarRequest(" Ana ", "ANA@EJEMPLO.COM", "123456", 2));

        assertEquals("Ana", resultado.nombre());
        assertEquals("ana@ejemplo.com", resultado.correo());
        assertEquals("Backend", resultado.especialidad());
        assertEquals("hash-original", usuario.getPassword());
        assertEquals("USUARIO", usuario.getRol());
    }

    @Test
    void correoDuplicadoNoActualiza() {
        Usuario usuario = new Usuario();
        when(usuarios.findById(1)).thenReturn(Optional.of(usuario));
        when(usuarios.existsByEmailIgnoreCaseAndIdUsuarioNot("usado@ejemplo.com", 1))
                .thenReturn(true);

        assertThrows(RecursoDuplicadoException.class, () -> servicio.actualizarUsuario(1,
                new UsuarioActualizarRequest("Ana", "usado@ejemplo.com", null, null)));
        verify(usuarios, never()).save(any());
    }
}
