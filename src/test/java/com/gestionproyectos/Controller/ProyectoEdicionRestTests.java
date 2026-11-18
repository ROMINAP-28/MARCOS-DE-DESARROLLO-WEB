package com.gestionproyectos.Controller;

import com.gestionproyectos.model.Usuario;
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

    @Test
    void vistaRenderizaYApiExponeIdsParaSeleccionarUsuariosSinAmbiguedad() throws Exception {
        var mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
        mvc.perform(get("/proyectos"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Eliminar")));

        // La respuesta incluye los IDs necesarios para usar usuarios con el mismo nombre.
        Usuario usuario = new Usuario();
        usuario.setNombre("Prueba proyecto");
        usuario.setEmail(UUID.randomUUID() + "@ejemplo.test");
        usuario.setPassword("hash-de-prueba");
        usuario.setRol("USUARIO");
        usuario = usuarios.save(usuario);
        try {
            String cuerpo = "{\"nombre\":\"Prueba\",\"descripcion\":\"Proyecto de prueba\","
                    + "\"fechaInicio\":\"2026-11-01\",\"fechaFin\":\"2026-12-01\","
                    + "\"prioridad\":\"Alta\",\"responsable\":\"Prueba proyecto\","
                    + "\"idResponsable\":" + usuario.getIdUsuario() + ",\"idsIntegrantes\":[]}";
            String creado = mvc.perform(post("/api/miembro3/proyectos")
                            .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.idResponsable").value(usuario.getIdUsuario()))
                    .andReturn().getResponse().getContentAsString();
            int id = com.jayway.jsonpath.JsonPath.read(creado, "$.id");
            try {
                mvc.perform(put("/api/miembro3/proyectos/" + id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(cuerpo.replace("\"nombre\":\"Prueba\"",
                                        "\"nombre\":\"Actualizado\"")))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.id").value(id))
                        .andExpect(jsonPath("$.nombre").value("Actualizado"));
            } finally {
                mvc.perform(delete("/api/miembro3/proyectos/" + id))
                        .andExpect(status().isNoContent());
            }
        } finally {
            usuarios.delete(usuario);
        }
    }
}
