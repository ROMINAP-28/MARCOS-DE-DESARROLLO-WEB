package com.gestionproyectos;

import com.gestionproyectos.model.Usuario;
import com.gestionproyectos.repository.RecuperacionPasswordRepository;
import com.gestionproyectos.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"app.recuperacion.remitente=noreply@ejemplo.test",
        "app.url-publica=http://localhost:8080"})
@Import(RecuperacionYPerfilWebTests.CorreoPrueba.class)
class RecuperacionYPerfilWebTests {
    @TestConfiguration
    static class CorreoPrueba {
        @Bean JavaMailSender mailSender() { return mock(JavaMailSender.class); }
    }

    @Autowired WebApplicationContext contexto;
    @Autowired UsuarioRepository usuarios;
    @Autowired RecuperacionPasswordRepository recuperaciones;
    @Autowired PasswordEncoder encoder;
    @Autowired JavaMailSender correo;

    @Test
    void correoPublicoNoEnumeraCuentasTokenExpiraEsUnicoYPermiteNuevoLogin() throws Exception {
        MockMvc mvc = mvc();
        Usuario usuario = crearUsuario();
        try {
            mvc.perform(get("/recuperar-password"))
                    .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                    .andExpect(content().string(containsString("name=\"_csrf\"")));
            mvc.perform(post("/recuperar-password").param("correo", usuario.getEmail()))
                    .andExpect(status().isForbidden());
            var respuesta = mvc.perform(post("/recuperar-password").with(csrf())
                            .param("correo", usuario.getEmail()))
                    .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/recuperar-password"))
                    .andReturn();
            assertTrue(respuesta.getFlashMap().get("mensaje").toString().contains("Si existe"));
            mvc.perform(post("/recuperar-password").with(csrf())
                            .param("correo", UUID.randomUUID() + "@ejemplo.test"))
                    .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/recuperar-password"))
                    .andExpect(flash().attribute("mensaje", respuesta.getFlashMap().get("mensaje")));

            var captor = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(correo, times(1)).send(captor.capture());
            String texto = captor.getValue().getText();
            assertNotNull(texto);
            assertEquals(usuario.getEmail(), captor.getValue().getTo()[0]);
            var patron = Pattern.compile("/restablecer-password\\?token=([A-Za-z0-9_-]{43})").matcher(texto);
            assertTrue(patron.find());
            String token = patron.group(1);
            assertFalse(recuperaciones.findAll().get(0).getTokenHash().equals(token));
            mvc.perform(get("/restablecer-password").param("token", token))
                    .andExpect(status().isOk()).andExpect(header().string("Referrer-Policy", "no-referrer"))
                    .andExpect(content().string(containsString("name=\"token\"")));
            mvc.perform(post("/restablecer-password").param("token", token)
                            .param("nuevaContrasena", "otra-clave-123")
                            .param("confirmarContrasena", "otra-clave-123"))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/restablecer-password").with(csrf()).param("token", token)
                            .param("nuevaContrasena", "otra-clave-123")
                            .param("confirmarContrasena", "diferente"))
                    .andExpect(status().isOk()).andExpect(content().string(containsString("no coinciden")));
            mvc.perform(post("/restablecer-password").with(csrf()).param("token", token)
                            .param("nuevaContrasena", "otra-clave-123")
                            .param("confirmarContrasena", "otra-clave-123"))
                    .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login?restablecida"));
            mvc.perform(post("/restablecer-password").with(csrf()).param("token", token)
                            .param("nuevaContrasena", "tercera-clave-123")
                            .param("confirmarContrasena", "tercera-clave-123"))
                    .andExpect(status().isOk()).andExpect(content().string(containsString("no es válido")));
            mvc.perform(post("/login").with(csrf()).param("correo", usuario.getEmail())
                            .param("contrasena", "clave-inicial-123"))
                    .andExpect(redirectedUrl("/login?error"));
            mvc.perform(post("/login").with(csrf()).param("correo", usuario.getEmail())
                            .param("contrasena", "otra-clave-123"))
                    .andExpect(redirectedUrl("/dashboard"));
            var ultima = recuperaciones.findAll().get(0);
            ultima.setExpira(LocalDateTime.now().minusMinutes(1));
            ultima.setConsumido(null);
            recuperaciones.saveAndFlush(ultima);
            mvc.perform(post("/restablecer-password").with(csrf()).param("token", token)
                            .param("nuevaContrasena", "cuarta-clave-123")
                            .param("confirmarContrasena", "cuarta-clave-123"))
                    .andExpect(status().isOk()).andExpect(content().string(containsString("caducado")));
        } finally {
            usuarios.delete(usuario);
            clearInvocations(correo);
        }
    }

    @Test
    void errorDeSmtpNoExponeQueElCorreoEstaRegistradoNiDejaTokenUtilizable() throws Exception {
        MockMvc mvc = mvc();
        Usuario usuario = crearUsuario();
        try {
            doThrow(new MailSendException("SMTP no disponible"))
                    .when(correo).send(any(SimpleMailMessage.class));
            var conocido = mvc.perform(post("/recuperar-password").with(csrf())
                            .param("correo", usuario.getEmail()))
                    .andExpect(status().is3xxRedirection()).andReturn().getFlashMap().get("mensaje");
            var desconocido = mvc.perform(post("/recuperar-password").with(csrf())
                            .param("correo", UUID.randomUUID() + "@ejemplo.test"))
                    .andExpect(status().is3xxRedirection()).andReturn().getFlashMap().get("mensaje");
            assertEquals(conocido, desconocido);
            assertTrue(recuperaciones.findAll().isEmpty());
        } finally {
            reset(correo);
            usuarios.delete(usuario);
        }
    }

    @Test
    void perfilSeEditaDesdeConfiguracionSoloConIdentidadAutenticadaYCsrf() throws Exception {
        MockMvc mvc = mvc();
        Usuario usuario = crearUsuario();
        Usuario tercero = crearUsuario();
        try {
            MockHttpSession sesion = (MockHttpSession) mvc.perform(post("/login").with(csrf())
                            .param("correo", usuario.getEmail()).param("contrasena", "clave-inicial-123"))
                    .andExpect(redirectedUrl("/dashboard")).andReturn().getRequest().getSession(false);
            mvc.perform(get("/configuracion").session(sesion)).andExpect(status().isOk())
                    .andExpect(content().string(containsString("formularioPerfil")));
            mvc.perform(get("/perfil").session(sesion)).andExpect(redirectedUrl("/configuracion"));
            String cuerpo = "{\"nombre\":\"Nuevo nombre\",\"correo\":\""
                    + usuario.getEmail() + "\",\"telefono\":\"555123456\",\"idEspecialidad\":null}";
            mvc.perform(put("/api/miembro3/usuarios/" + tercero.getIdUsuario())
                            .session(sesion).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isForbidden());
            mvc.perform(put("/api/miembro3/usuarios/" + usuario.getIdUsuario())
                            .session(sesion).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isForbidden());
            mvc.perform(put("/api/miembro3/usuarios/" + usuario.getIdUsuario())
                            .session(sesion).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Nuevo nombre"))
                    .andExpect(jsonPath("$.telefono").value("555123456"));
            mvc.perform(get("/api/auth/yo").session(sesion))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Nuevo nombre"));
        } finally {
            usuarios.delete(usuario);
            usuarios.delete(tercero);
        }
    }

    private MockMvc mvc() {
        return MockMvcBuilders.webAppContextSetup(contexto)
                .apply(SecurityMockMvcConfigurers.springSecurity()).build();
    }

    private Usuario crearUsuario() {
        Usuario usuario = new Usuario();
        usuario.setNombre("Prueba recuperación");
        usuario.setEmail(UUID.randomUUID() + "@ejemplo.test");
        usuario.setPassword(encoder.encode("clave-inicial-123"));
        usuario.setRol("USUARIO");
        return usuarios.save(usuario);
    }
}
