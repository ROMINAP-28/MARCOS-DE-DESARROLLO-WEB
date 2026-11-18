package com.gestionproyectos.Controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class EquipoVistaTests {
    @Autowired WebApplicationContext contexto;

    @Test
    void equipoRenderizaYListaParticipacionesSinExponerEntidades() throws Exception {
        var mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
        mvc.perform(get("/equipo"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Usuario registrado")));
        mvc.perform(get("/api/miembro3/participaciones"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"));
    }
}
