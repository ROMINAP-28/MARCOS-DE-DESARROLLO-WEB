package com.gestionproyectos.Controller;

import com.gestionproyectos.model.Usuario;
import com.gestionproyectos.repository.ProyectoRepository;
import com.gestionproyectos.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class ProyectoEdicionRestTests {
    @Autowired WebApplicationContext contexto;
    @Autowired UsuarioRepository usuarios;
    @Autowired ProyectoRepository proyectos;

    @Test
    void vistaRenderizaYApiExponeIdsParaSeleccionarUsuariosSinAmbiguedad() throws Exception {
        var mvc = MockMvcBuilders.webAppContextSetup(contexto)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity())
                .build();
        mvc.perform(get("/proyectos").with(
                        org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("test")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Eliminar")))
                .andExpect(content().string(org.hamcrest.Matchers.matchesPattern(
                        "(?s).*<textarea id=\"descripcionProyecto\"[^>]*maxlength=\"255\".*")));

        // La respuesta incluye los IDs necesarios para usar usuarios con el mismo nombre.
        Usuario usuario = new Usuario();
        usuario.setNombre("Prueba proyecto");
        usuario.setEmail(UUID.randomUUID() + "@ejemplo.test");
        usuario.setPassword("hash-de-prueba");
        usuario.setRol("USUARIO");
        usuario = usuarios.save(usuario);
        try {
            var principal = new com.gestionproyectos.config.UsuarioPrincipal(usuario);
            var autenticacion = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                    principal, null, principal.getAuthorities());
            String cuerpo = "{\"nombre\":\"Prueba\",\"descripcion\":\"Proyecto de prueba\","
                    + "\"fechaInicio\":\"2026-11-01\",\"fechaFin\":\"2026-12-01\","
                    + "\"prioridad\":\"Alta\",\"responsable\":\"Prueba proyecto\","
                    + "\"idResponsable\":" + usuario.getIdUsuario() + ",\"idsIntegrantes\":[]}";
            long antes = proyectos.count();
            mvc.perform(post("/api/miembro3/proyectos")
                            .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(autenticacion))
                            .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo.replace("\"prioridad\":\"Alta\"", "\"prioridad\":\"Crítica\"")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.mensaje", org.hamcrest.Matchers.containsString("Prioridad")));
            mvc.perform(post("/api/miembro3/proyectos")
                            .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(autenticacion))
                            .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo.replace("\"idsIntegrantes\":[]",
                                    "\"idsIntegrantes\":[],\"estado\":\"Inexistente\"")))
                    .andExpect(status().isBadRequest());
            org.junit.jupiter.api.Assertions.assertEquals(antes, proyectos.count());
            mvc.perform(post("/api/miembro3/proyectos")
                            .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(autenticacion))
                            .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo.replace("Proyecto de prueba", "x".repeat(256))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.mensaje", org.hamcrest.Matchers.containsString("descripcion")));
            org.junit.jupiter.api.Assertions.assertEquals(antes, proyectos.count());
            String creado = mvc.perform(post("/api/miembro3/proyectos")
                            .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(autenticacion))
                            .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                            .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.idResponsable").value(usuario.getIdUsuario()))
                    .andReturn().getResponse().getContentAsString();
            int id = com.jayway.jsonpath.JsonPath.read(creado, "$.id");
            try {
                mvc.perform(put("/api/miembro3/proyectos/" + id)
                                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(autenticacion))
                                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(cuerpo.replace("\"prioridad\":\"Alta\"", "\"prioridad\":\"Crítica\"")))
                        .andExpect(status().isBadRequest());
                mvc.perform(put("/api/miembro3/proyectos/" + id)
                                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(autenticacion))
                                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(cuerpo.replace("\"idsIntegrantes\":[]",
                                        "\"idsIntegrantes\":[],\"estado\":\"Inexistente\"")))
                        .andExpect(status().isBadRequest());
                org.junit.jupiter.api.Assertions.assertEquals("Alta",
                        proyectos.findById(id).orElseThrow().getPrioridad());
                org.junit.jupiter.api.Assertions.assertEquals("Planificado",
                        proyectos.findById(id).orElseThrow().getEstado());
                mvc.perform(put("/api/miembro3/proyectos/" + id)
                                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(autenticacion))
                                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(cuerpo.replace("Proyecto de prueba", "x".repeat(256))))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.mensaje", org.hamcrest.Matchers.containsString("descripcion")));
                org.junit.jupiter.api.Assertions.assertEquals("Proyecto de prueba",
                        proyectos.findById(id).orElseThrow().getDescripcion());
                mvc.perform(put("/api/miembro3/proyectos/" + id)
                                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(autenticacion))
                                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(cuerpo.replace("\"nombre\":\"Prueba\"",
                                        "\"nombre\":\"Actualizado\"")))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.id").value(id))
                        .andExpect(jsonPath("$.nombre").value("Actualizado"));
                mvc.perform(put("/api/miembro3/proyectos/" + id)
                                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(autenticacion))
                                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(cuerpo.replace("Proyecto de prueba", "x".repeat(255))))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.descripcion").value("x".repeat(255)));
            } finally {
                mvc.perform(delete("/api/miembro3/proyectos/" + id)
                                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(autenticacion))
                                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                        .andExpect(status().isNoContent());
            }
        } finally {
            usuarios.delete(usuario);
        }
    }
}
