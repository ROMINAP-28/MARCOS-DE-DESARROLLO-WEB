package com.gestionproyectos;

import com.gestionproyectos.config.UsuarioPrincipal;
import com.gestionproyectos.dto.EstadoParticipacionRequest;
import com.gestionproyectos.dto.ProyectoRequest;
import com.gestionproyectos.dto.TareaRequest;
import com.gestionproyectos.model.Usuario;
import com.gestionproyectos.repository.UsuarioRepository;
import com.gestionproyectos.service.ParticipacionServicio;
import com.gestionproyectos.service.ProyectoServicio;
import com.gestionproyectos.service.TareaServicio;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class AlcanceLecturaRestTests {
    @Autowired WebApplicationContext contexto;
    @Autowired UsuarioRepository usuarios;
    @Autowired ProyectoServicio proyectos;
    @Autowired TareaServicio tareas;
    @Autowired ParticipacionServicio participaciones;

    @Test
    void soloResponsableEIntegranteActivoVenProyectosTareasEquipoYReportesPropios() throws Exception {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(contexto)
                .apply(SecurityMockMvcConfigurers.springSecurity()).build();
        Usuario responsable = usuario("Responsable alcance");
        Usuario integrante = usuario("Integrante alcance");
        Usuario ajeno = usuario("Ajeno alcance");
        Integer propio = null;
        Integer privado = null;
        try {
            propio = proyecto("Proyecto compartido", responsable, List.of(integrante.getIdUsuario()));
            privado = proyecto("Proyecto privado", ajeno, List.of());
            int tareaPropia = tarea("Tarea compartida", propio, responsable.getIdUsuario());
            int tareaColega = tarea("Tarea de otro miembro", propio, integrante.getIdUsuario());
            int tareaPrivada = tarea("Tarea privada", privado, ajeno.getIdUsuario());

            mvc.perform(get("/api/miembro3/proyectos").with(cuenta(integrante)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(propio));
            mvc.perform(get("/api/miembro3/proyectos?q=privado").with(cuenta(integrante)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
            mvc.perform(get("/api/miembro3/proyectos/" + privado).with(cuenta(integrante)))
                    .andExpect(status().isForbidden());
            mvc.perform(get("/api/miembro3/tareas").with(cuenta(integrante)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[*].id", containsInAnyOrder(tareaPropia, tareaColega)));
            mvc.perform(get("/api/miembro3/tareas/" + tareaPrivada).with(cuenta(integrante)))
                    .andExpect(status().isForbidden());
            mvc.perform(get("/api/miembro3/participaciones").with(cuenta(integrante)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[*].idProyecto", everyItem(is(propio))));
            mvc.perform(get("/api/miembro3/reportes/indicadores").with(cuenta(integrante)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.proyectosTotal").value(1))
                    .andExpect(jsonPath("$.tareasTotal").value(2));
            for (String tipo : List.of("avance", "tareas", "equipo", "recursos")) {
                mvc.perform(post("/api/miembro3/reportes/consulta").with(cuenta(integrante))
                                .with(csrf()).contentType(MediaType.APPLICATION_JSON)
                                .content("{\"tipo\":\"" + tipo + "\"}"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.resumen[0].valor").value(tipo.equals("tareas") ? "2"
                                : tipo.equals("equipo") ? "2" : tipo.equals("avance") ? "1" : "0"));
            }
            mvc.perform(post("/api/miembro3/reportes/consulta").with(cuenta(integrante))
                            .with(csrf()).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"tipo\":\"avance\",\"idProyecto\":" + privado + "}"))
                    .andExpect(status().isForbidden());

            // La baja de la participación retira las lecturas, incluso de tareas previamente asignadas.
            participaciones.cambiarEstado(propio, integrante.getIdUsuario(),
                    new EstadoParticipacionRequest("Inactivo"));
            mvc.perform(get("/api/miembro3/proyectos").with(cuenta(integrante)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
            mvc.perform(get("/api/miembro3/tareas").with(cuenta(integrante)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
            mvc.perform(get("/api/miembro3/tareas/" + tareaColega).with(cuenta(integrante)))
                    .andExpect(status().isForbidden());
            mvc.perform(patch("/api/miembro3/tareas/" + tareaColega + "/estado")
                            .with(cuenta(integrante)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"estado\":\"Completada\"}"))
                    .andExpect(status().isForbidden());
            mvc.perform(get("/api/miembro3/reportes/indicadores").with(cuenta(integrante)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.proyectosTotal").value(0));
            mvc.perform(get("/api/miembro3/proyectos").with(cuenta(responsable)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
            mvc.perform(get("/api/miembro3/proyectos").with(cuenta(ajeno)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(privado));
        } finally {
            if (propio != null) proyectos.eliminarProyecto(propio);
            if (privado != null) proyectos.eliminarProyecto(privado);
            usuarios.delete(integrante);
            usuarios.delete(responsable);
            usuarios.delete(ajeno);
        }
    }

    private RequestPostProcessor cuenta(Usuario usuario) {
        var principal = new UsuarioPrincipal(usuario);
        return SecurityMockMvcRequestPostProcessors.authentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private Usuario usuario(String nombre) {
        Usuario usuario = new Usuario();
        usuario.setNombre(nombre);
        usuario.setEmail(UUID.randomUUID() + "@ejemplo.test");
        usuario.setPassword("hash-de-prueba");
        usuario.setRol("USUARIO");
        return usuarios.save(usuario);
    }

    private int proyecto(String nombre, Usuario responsable, List<Integer> equipo) {
        return proyectos.guardarProyecto(new ProyectoRequest(nombre, "Descripción de prueba",
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 31), "Alta", null, null,
                null, null, responsable.getIdUsuario(), equipo)).id();
    }

    private int tarea(String nombre, int idProyecto, int idResponsable) {
        return tareas.guardarTarea(new TareaRequest(nombre, "Prueba de alcance", null,
                "Alta", null, LocalDate.of(2026, 12, 1), null, idProyecto, idResponsable, null)).id();
    }
}
