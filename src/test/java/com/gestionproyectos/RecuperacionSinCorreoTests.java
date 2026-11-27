package com.gestionproyectos;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.mail.host=", "app.recuperacion.remitente="})
class RecuperacionSinCorreoTests {
    @Autowired WebApplicationContext contexto;

    @Test
    void sinSmtpNoAfirmaQueSeEnvioUnEnlace() throws Exception {
        var mvc = MockMvcBuilders.webAppContextSetup(contexto)
                .apply(SecurityMockMvcConfigurers.springSecurity()).build();
        mvc.perform(post("/recuperar-password").with(csrf()).param("correo", "x@ejemplo.test"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string(containsString("no está disponible")));
    }
}
