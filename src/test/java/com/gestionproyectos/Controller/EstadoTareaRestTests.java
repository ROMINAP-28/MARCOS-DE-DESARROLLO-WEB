package com.gestionproyectos.Controller;

import com.gestionproyectos.exception.ManejadorGlobalExcepciones;
import com.gestionproyectos.service.ITareaServicio;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EstadoTareaRestTests {

    @Test
    void estadoVacioResponde400SinLlamarAlServicio() throws Exception {
        ITareaServicio servicio = mock(ITareaServicio.class);
        var validador = new LocalValidatorFactoryBean();
        validador.afterPropertiesSet();
        var mvc = MockMvcBuilders.standaloneSetup(new TareaController(servicio))
                .setControllerAdvice(new ManejadorGlobalExcepciones())
                .setValidator(validador)
                .build();

        mvc.perform(patch("/api/miembro3/tareas/1/estado")
                        .contentType("application/json").content("{\"estado\":\" \"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(servicio);
    }
}
