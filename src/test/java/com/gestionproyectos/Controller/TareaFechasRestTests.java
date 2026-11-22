package com.gestionproyectos.Controller;

import com.gestionproyectos.dto.ProyectoRequest;
import com.gestionproyectos.model.Usuario;
import com.gestionproyectos.repository.TareaRepository;
import com.gestionproyectos.repository.UsuarioRepository;
import com.gestionproyectos.service.ProyectoServicio;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class TareaFechasRestTests {
    @Autowired WebApplicationContext contexto;
    @Autowired UsuarioRepository usuarios;
    @Autowired ProyectoServicio proyectos;
    @Autowired TareaRepository tareas;

    @Test
    void fechaInicioOpcionalEditableConIntervaloYDescripcionValidadosEnApi() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setNombre("Responsable fechas");
        usuario.setEmail(UUID.randomUUID() + "@ejemplo.test");
        usuario.setPassword("hash-de-prueba");
        usuario.setRol("USUARIO");
        usuario = usuarios.save(usuario);
        int idProyecto = proyectos.guardarProyecto(new ProyectoRequest("Fechas", "Descripción",
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 31), "Alta",
                null, null, null, null, usuario.getIdUsuario(), List.of())).id();
        var principal = new com.gestionproyectos.config.UsuarioPrincipal(usuario);
        var autenticado = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        var mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        String cuerpo = "{\"nombre\":\"Verificar\",\"descripcion\":\"Breve\","
                + "\"prioridad\":\"Alta\",\"fechaFin\":\"2026-12-01\","
                + "\"idProyecto\":" + idProyecto + ",\"idResponsable\":" + usuario.getIdUsuario() + "}";

        mvc.perform(get("/tareas").with(authentication(autenticado)))
                .andExpect(status().isOk())
                .andExpect(content().string(matchesPattern("(?s).*<textarea id=\"descripcionTarea\"[^>]*maxlength=\"500\".*")))
                .andExpect(content().string(containsString("id=\"fechaInicioTarea\"")));
        mvc.perform(get("/equipo").with(authentication(autenticado)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"fechaInicioTareaEquipo\"")));

        long antes = tareas.count();
        mvc.perform(post("/api/miembro3/tareas").with(authentication(autenticado)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo.replace("\"fechaFin\"", "\"fechaInicio\":\"2026-12-02\",\"fechaFin\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje", containsString("fecha de fin")));
        mvc.perform(post("/api/miembro3/tareas").with(authentication(autenticado)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo.replace("Breve", "x".repeat(501))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje", containsString("descripcion")));
        assertEquals(antes, tareas.count());

        String creada = mvc.perform(post("/api/miembro3/tareas").with(authentication(autenticado)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fechaInicio").value(org.hamcrest.Matchers.nullValue()))
                .andReturn().getResponse().getContentAsString();
        int id = com.jayway.jsonpath.JsonPath.read(creada, "$.id");

        String conInicio = cuerpo.replace("\"fechaFin\"", "\"fechaInicio\":\"2026-11-10\",\"fechaFin\"");
        mvc.perform(put("/api/miembro3/tareas/" + id).with(authentication(autenticado)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(conInicio.replace("Breve", "x".repeat(500))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fechaInicio").value("2026-11-10"))
                .andExpect(jsonPath("$.descripcion").value("x".repeat(500)));

        mvc.perform(put("/api/miembro3/tareas/" + id).with(authentication(autenticado)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(conInicio.replace("2026-11-10", "2026-12-02")))
                .andExpect(status().isBadRequest());
        assertEquals(LocalDate.of(2026, 11, 10), tareas.findById(id).orElseThrow().getFechaInicio());
        assertEquals(LocalDate.of(2026, 12, 1), tareas.findById(id).orElseThrow().getFechaFin());

        mvc.perform(put("/api/miembro3/tareas/" + id).with(authentication(autenticado)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(conInicio.replace("2026-11-10", "2026-11-15")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fechaInicio").value("2026-11-15"));
        mvc.perform(put("/api/miembro3/tareas/" + id).with(authentication(autenticado)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fechaInicio").value(org.hamcrest.Matchers.nullValue()));
        assertNull(tareas.findById(id).orElseThrow().getFechaInicio());
    }
}
