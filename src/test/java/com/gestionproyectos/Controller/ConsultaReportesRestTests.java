package com.gestionproyectos.Controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class ConsultaReportesRestTests {
    @Autowired WebApplicationContext contexto;

    @Test
    void vistaEIndicadoresYConsultaVaciaResponden() throws Exception {
        var mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
        mvc.perform(get("/reportes"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Descargar CSV")));
        mvc.perform(get("/api/miembro3/reportes/indicadores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proyectosActivos").isNumber());
        mvc.perform(post("/api/miembro3/reportes/consulta").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"tareas\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("tareas"))
                .andExpect(jsonPath("$.resumen").isArray());
    }

    @Test
    void erroresDevuelvenMensajesSinGuardarHistorial() throws Exception {
        var mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
        mvc.perform(post("/api/miembro3/reportes/consulta").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"recursos\",\"fechaDesde\":\"2026-11-01\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").exists());
        mvc.perform(post("/api/miembro3/reportes/consulta").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"falso\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/miembro3/reportes/consulta").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"avance\",\"idProyecto\":-3}"))
                .andExpect(status().isBadRequest());
    }
}
