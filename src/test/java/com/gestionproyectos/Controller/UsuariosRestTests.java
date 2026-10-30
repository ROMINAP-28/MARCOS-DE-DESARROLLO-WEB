package com.gestionproyectos.Controller;

import com.gestionproyectos.dto.UsuarioDto;
import com.gestionproyectos.exception.ManejadorGlobalExcepciones;
import com.gestionproyectos.exception.RecursoDuplicadoException;
import com.gestionproyectos.exception.RecursoNoEncontradoException;
import com.gestionproyectos.service.IUsuarioServicio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UsuariosRestTests {

    private IUsuarioServicio servicio;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        servicio = mock(IUsuarioServicio.class);
        var validador = new LocalValidatorFactoryBean();
        validador.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new AuthController(servicio), new UsuarioController(servicio))
                .setControllerAdvice(new ManejadorGlobalExcepciones())
                .setValidator(validador)
                .build();
    }

    @Test
    void registroResponde201YRechazaDatosInvalidosCon400() throws Exception {
        mvc.perform(post("/api/auth/registro").contentType("application/json")
                        .content("""
                                {"nombre":"Ana","correo":"ana@ejemplo.com","contrasena":"clave-segura"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mensaje").exists());
        verify(servicio).registrar(any());

        mvc.perform(post("/api/auth/registro").contentType("application/json")
                        .content("""
                                {"nombre":" ","correo":"correo-invalido","contrasena":"123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").exists());
        verifyNoMoreInteractions(servicio);
    }

    @Test
    void usuarioResponde200404Y204() throws Exception {
        var usuario = new UsuarioDto(1, "Ana", "ana@ejemplo.com", null,
                "USUARIO", null, LocalDateTime.of(2026, 1, 1, 12, 0));
        when(servicio.listarUsuarios()).thenReturn(List.of(usuario));
        when(servicio.buscarUsuarioPorId(1)).thenReturn(usuario);
        when(servicio.buscarUsuarioPorId(999))
                .thenThrow(new RecursoNoEncontradoException("Usuario no encontrado"));
        when(servicio.actualizarUsuario(eq(1), any())).thenReturn(usuario);

        mvc.perform(get("/api/miembro3/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Ana"));
        mvc.perform(get("/api/miembro3/usuarios/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ana"))
                .andExpect(jsonPath("$.password").doesNotExist());
        mvc.perform(get("/api/miembro3/usuarios/999"))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/miembro3/usuarios/1").contentType("application/json")
                        .content("""
                                {"nombre":"Ana","correo":"ana@ejemplo.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("ana@ejemplo.com"));
        mvc.perform(delete("/api/miembro3/usuarios/1"))
                .andExpect(status().isNoContent());
        verify(servicio).eliminarUsuario(1);
    }

    @Test
    void solicitudesInvalidasYConflictosResponden400Y409() throws Exception {
        mvc.perform(put("/api/miembro3/usuarios/1").contentType("application/json")
                        .content("""
                                {"nombre":"Ana","correo":"invalido"}
                                """))
                .andExpect(status().isBadRequest());
        verify(servicio, never()).actualizarUsuario(any(), any());

        when(servicio.actualizarUsuario(eq(1), any()))
                .thenThrow(new RecursoDuplicadoException("Este correo ya está registrado."));
        mvc.perform(put("/api/miembro3/usuarios/1").contentType("application/json")
                        .content("""
                                {"nombre":"Ana","correo":"usado@ejemplo.com"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Este correo ya está registrado."));

        doThrow(new DataIntegrityViolationException("Referencia existente"))
                .when(servicio).eliminarUsuario(1);
        mvc.perform(delete("/api/miembro3/usuarios/1"))
                .andExpect(status().isConflict());

        doThrow(new RecursoNoEncontradoException("Usuario no encontrado"))
                .when(servicio).eliminarUsuario(999);
        mvc.perform(delete("/api/miembro3/usuarios/999"))
                .andExpect(status().isNotFound());
    }
}
