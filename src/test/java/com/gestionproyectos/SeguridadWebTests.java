package com.gestionproyectos;

import com.gestionproyectos.model.Usuario;
import com.gestionproyectos.repository.UsuarioRepository;
import com.gestionproyectos.repository.EspecialidadRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class SeguridadWebTests {
    @Autowired WebApplicationContext contexto;
    @Autowired UsuarioRepository usuarios;
    @Autowired EspecialidadRepository especialidades;
    @Autowired PasswordEncoder encoder;

    @Test
    void loginConBcryptSesionCsrfYLogoutProtegenVistasYApi() throws Exception {
        var mvc = MockMvcBuilders.webAppContextSetup(contexto)
                .apply(SecurityMockMvcConfigurers.springSecurity()).build();
        Usuario usuario = usuario("Seguridad", "clave-segura-123");
        try {
            mvc.perform(get("/dashboard")).andExpect(status().is3xxRedirection());
            mvc.perform(get("/api/miembro3/proyectos")).andExpect(status().isUnauthorized());
            mvc.perform(post("/api/miembro3/proyectos").contentType(MediaType.APPLICATION_JSON)
                    .content("{}")) .andExpect(status().isUnauthorized());
            mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                    .content("{}")) .andExpect(status().isForbidden());
            mvc.perform(get("/login"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(org.hamcrest.Matchers.matchesPattern(
                            "(?s).*name=\"_csrf\"[^>]+value=\"[^\"]+\".*")));
            mvc.perform(get("/registro"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(org.hamcrest.Matchers.matchesPattern(
                            "(?s).*name=\"_csrf\" content=\"[^\"]+\".*")));
            mvc.perform(post("/login").with(csrf()).param("correo", usuario.getEmail())
                    .param("contrasena", "incorrecta"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login?error"));
            var respuesta = mvc.perform(post("/login").with(csrf())
                            .param("correo", usuario.getEmail()).param("contrasena", "clave-segura-123"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/dashboard"))
                    .andReturn();
            MockHttpSession sesion = (MockHttpSession) respuesta.getRequest().getSession(false);
            mvc.perform(get("/api/auth/yo").session(sesion))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(usuario.getIdUsuario()));
            mvc.perform(get("/dashboard").session(sesion))
                    .andExpect(status().isOk())
                    .andExpect(content().string(org.hamcrest.Matchers.matchesPattern(
                            "(?s).*name=\"_csrf\" content=\"[^\"]+\".*")));
            mvc.perform(post("/logout").session(sesion)).andExpect(status().isForbidden());
            mvc.perform(post("/logout").session(sesion).with(csrf()))
                    .andExpect(status().is3xxRedirection());
            mvc.perform(get("/api/auth/yo").session(sesion)).andExpect(status().isUnauthorized());
        } finally {
            usuarios.delete(usuario);
        }
    }

    @Test
    void registroPublicoRequiereCsrfYCuentaNuevaPuedeIniciarSesion() throws Exception {
        var mvc = MockMvcBuilders.webAppContextSetup(contexto)
                .apply(SecurityMockMvcConfigurers.springSecurity()).build();
        String correo = UUID.randomUUID() + "@ejemplo.test";
        try {
            mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nombre\":\"Nuevo\",\"correo\":\"" + correo
                                    + "\",\"contrasena\":\"secreto-1234\"}"))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/api/auth/registro").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nombre\":\"Nuevo\",\"correo\":\"" + correo
                                    + "\",\"contrasena\":\"secreto-1234\"}"))
                    .andExpect(status().isCreated());
            mvc.perform(post("/login").with(csrf()).param("correo", correo)
                            .param("contrasena", "secreto-1234"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/dashboard"));
            org.junit.jupiter.api.Assertions.assertEquals("USUARIO",
                    usuarios.findByEmailIgnoreCase(correo).orElseThrow().getRol());
        } finally {
            usuarios.findByEmailIgnoreCase(correo).ifPresent(usuarios::delete);
        }
    }

    @Test
    void borrarCuentaPropiaInvalidaLaSesion() throws Exception {
        var mvc = MockMvcBuilders.webAppContextSetup(contexto)
                .apply(SecurityMockMvcConfigurers.springSecurity()).build();
        Usuario usuario = usuario("Baja voluntaria", "clave-segura-123");
        try {
            MockHttpSession sesion = sesion(mvc, usuario, "clave-segura-123");
            mvc.perform(delete("/api/miembro3/usuarios/" + usuario.getIdUsuario())
                            .session(sesion).with(csrf()))
                    .andExpect(status().isNoContent());
            org.junit.jupiter.api.Assertions.assertTrue(sesion.isInvalid());
            org.junit.jupiter.api.Assertions.assertFalse(usuarios.existsById(usuario.getIdUsuario()));
        } finally {
            if (usuarios.existsById(usuario.getIdUsuario())) usuarios.delete(usuario);
        }
    }

    @Test
    void soloResponsablePuedeModificarProyectoYEquipoYUsuarioSuPropioPerfil() throws Exception {
        var mvc = MockMvcBuilders.webAppContextSetup(contexto)
                .apply(SecurityMockMvcConfigurers.springSecurity()).build();
        Usuario responsable = usuario("Responsable", "clave-segura-123");
        Usuario ajeno = usuario("Ajeno", "clave-segura-456");
        MockHttpSession sesionResponsable = sesion(mvc, responsable, "clave-segura-123");
        MockHttpSession sesionAjeno = sesion(mvc, ajeno, "clave-segura-456");
        int idProyecto = 0;
        int idEspecialidad = 0;
        try {
            String cuerpo = "{\"nombre\":\"Privado\",\"descripcion\":\"Proyecto privado\","
                    + "\"fechaInicio\":\"2026-11-01\",\"fechaFin\":\"2026-12-01\","
                    + "\"prioridad\":\"Alta\","
                    + "\"idResponsable\":" + responsable.getIdUsuario() + ",\"idsIntegrantes\":[]}";
            mvc.perform(post("/api/miembro3/proyectos").session(sesionAjeno).with(csrf())
                    .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isForbidden());
            String creado = mvc.perform(post("/api/miembro3/proyectos").session(sesionResponsable)
                            .with(csrf()).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
            idProyecto = com.jayway.jsonpath.JsonPath.read(creado, "$.id");
            mvc.perform(post("/api/miembro3/proyectos").session(sesionResponsable).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo.replace("\"idsIntegrantes\":[]", "\"integrantes\":[\"Ajeno\"]")))
                    .andExpect(status().isBadRequest());
            mvc.perform(put("/api/miembro3/proyectos/" + idProyecto).session(sesionAjeno).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isForbidden());
            mvc.perform(delete("/api/miembro3/proyectos/" + idProyecto).session(sesionAjeno).with(csrf()))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/api/miembro3/participaciones").session(sesionAjeno).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"idProyecto\":" + idProyecto + ",\"idUsuario\":"
                                    + ajeno.getIdUsuario() + ",\"estado\":\"Activo\"}"))
                    .andExpect(status().isForbidden());
            mvc.perform(put("/api/miembro3/usuarios/" + responsable.getIdUsuario())
                            .session(sesionAjeno).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nombre\":\"Intruso\",\"correo\":\"intruso@ejemplo.test\"}"))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/api/miembro3/participaciones").session(sesionResponsable).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"idProyecto\":" + idProyecto + ",\"idUsuario\":"
                                    + ajeno.getIdUsuario() + ",\"estado\":\"Activo\"}"))
                    .andExpect(status().isCreated());
            String tarea = "{\"nombre\":\"Trabajo\",\"proyecto\":\"Privado\","
                    + "\"idProyecto\":" + idProyecto + ",\"responsable\":\"Ajeno\","
                    + "\"idResponsable\":" + ajeno.getIdUsuario() + ",\"prioridad\":\"Alta\","
                    + "\"fechaFin\":\"2026-12-01\"}";
            mvc.perform(post("/api/miembro3/tareas").session(sesionResponsable).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(tarea.replace("\"prioridad\":\"Alta\"", "\"prioridad\":\"Crítica\"")))
                    .andExpect(status().isBadRequest());
            mvc.perform(post("/api/miembro3/tareas").session(sesionResponsable).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(tarea.replace("\"fechaFin\"", "\"estado\":\"Inexistente\",\"fechaFin\"")))
                    .andExpect(status().isBadRequest());
            mvc.perform(post("/api/miembro3/tareas").session(sesionResponsable).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(tarea.replace("\"idResponsable\":" + ajeno.getIdUsuario() + ",", "")))
                    .andExpect(status().isBadRequest());
            mvc.perform(post("/api/miembro3/tareas").session(sesionAjeno).with(csrf())
                    .contentType(MediaType.APPLICATION_JSON).content(tarea))
                    .andExpect(status().isForbidden());
            String creada = mvc.perform(post("/api/miembro3/tareas").session(sesionResponsable).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content(tarea))
                    .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
            int idTarea = com.jayway.jsonpath.JsonPath.read(creada, "$.id");
            mvc.perform(put("/api/miembro3/tareas/" + idTarea).session(sesionResponsable).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(tarea.replace("\"prioridad\":\"Alta\"", "\"prioridad\":\"Crítica\"")))
                    .andExpect(status().isBadRequest());
            mvc.perform(put("/api/miembro3/tareas/" + idTarea).session(sesionResponsable).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(tarea.replace("\"fechaFin\"", "\"estado\":\"Inexistente\",\"fechaFin\"")))
                    .andExpect(status().isBadRequest());
            mvc.perform(patch("/api/miembro3/tareas/" + idTarea + "/estado")
                            .session(sesionResponsable).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"estado\":\"Inexistente\"}"))
                    .andExpect(status().isBadRequest());
            mvc.perform(get("/api/miembro3/tareas/" + idTarea).session(sesionResponsable))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.prioridad").value("Alta"))
                    .andExpect(jsonPath("$.estado").value("Pendiente"));
            mvc.perform(delete("/api/miembro3/tareas/" + idTarea).session(sesionAjeno).with(csrf()))
                    .andExpect(status().isForbidden());
            mvc.perform(patch("/api/miembro3/tareas/" + idTarea + "/estado")
                            .session(sesionAjeno).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"estado\":\"En progreso\"}"))
                    .andExpect(status().isOk());
            String especialidad = "Área " + UUID.randomUUID();
            mvc.perform(post("/api/miembro3/especialidades").session(sesionAjeno).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\""
                                    + especialidad + "\"}"))
                    .andExpect(status().isForbidden());
            String creadaEspecialidad = mvc.perform(post("/api/miembro3/especialidades")
                            .session(sesionResponsable).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nombre\":\"" + especialidad + "\"}"))
                    .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
            idEspecialidad = com.jayway.jsonpath.JsonPath.read(creadaEspecialidad, "$.id");
        } finally {
            if (idEspecialidad != 0) especialidades.deleteById(idEspecialidad);
            if (idProyecto != 0) mvc.perform(delete("/api/miembro3/proyectos/" + idProyecto)
                    .session(sesionResponsable).with(csrf())).andExpect(status().isNoContent());
            usuarios.delete(ajeno);
            usuarios.delete(responsable);
        }
    }

    private MockHttpSession sesion(org.springframework.test.web.servlet.MockMvc mvc,
                                  Usuario usuario, String password) throws Exception {
        var resultado = mvc.perform(post("/login").with(csrf()).param("correo", usuario.getEmail())
                        .param("contrasena", password))
                .andExpect(status().is3xxRedirection()).andReturn();
        return (MockHttpSession) resultado.getRequest().getSession(false);
    }

    private Usuario usuario(String nombre, String password) {
        Usuario usuario = new Usuario();
        usuario.setNombre(nombre);
        usuario.setEmail(UUID.randomUUID() + "@ejemplo.test");
        usuario.setPassword(encoder.encode(password));
        usuario.setRol("USUARIO");
        return usuarios.save(usuario);
    }
}
