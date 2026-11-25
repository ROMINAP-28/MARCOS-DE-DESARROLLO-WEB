package com.gestionproyectos;

import com.gestionproyectos.dto.ProyectoRequest;
import com.gestionproyectos.model.Usuario;
import com.gestionproyectos.repository.UsuarioRepository;
import com.gestionproyectos.service.ProyectoServicio;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IntegracionWebSocketTests {
    @Value("${local.server.port}") int puerto;
    @Autowired UsuarioRepository usuarios;
    @Autowired ProyectoServicio proyectos;
    @Autowired PasswordEncoder encoder;

    @Test
    void sesionRealRecibeAvisoTrasCommitSoloElAsignadoRecibeNotificacion() throws Exception {
        Usuario responsable = usuario("Responsable tiempo real");
        Usuario asignado = usuario("Asignado tiempo real");
        int idProyecto = 0;
        WebSocket socketResponsable = null;
        WebSocket socketAsignado = null;
        try {
            idProyecto = proyectos.guardarProyecto(new ProyectoRequest("Tiempo real", "Proyecto de prueba",
                    LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 31), "Alta", null, null,
                    null, null, responsable.getIdUsuario(), List.of(asignado.getIdUsuario()))).id();
            String base = "http://localhost:" + puerto;
            var escuchaResponsable = new Escucha();
            var escuchaAsignado = new Escucha();
            var clienteResponsable = iniciarSesion(base, responsable);
            var clienteAsignado = iniciarSesion(base, asignado);

            // Ni anónimos ni otros orígenes pueden establecer el canal.
            HttpClient anonimo = HttpClient.newHttpClient();
            assertThrows(ExecutionException.class, () -> anonimo.newWebSocketBuilder()
                    .header("Origin", base).buildAsync(URI.create("ws://localhost:" + puerto + "/ws/avisos"),
                            new Escucha()).get(5, TimeUnit.SECONDS));
            assertThrows(ExecutionException.class, () -> clienteAsignado.newWebSocketBuilder()
                    .header("Origin", "https://otro.example")
                    .buildAsync(URI.create("ws://localhost:" + puerto + "/ws/avisos"), new Escucha())
                    .get(5, TimeUnit.SECONDS));

            socketResponsable = clienteResponsable.newWebSocketBuilder().header("Origin", base)
                    .buildAsync(URI.create("ws://localhost:" + puerto + "/ws/avisos"), escuchaResponsable)
                    .get(5, TimeUnit.SECONDS);
            socketAsignado = clienteAsignado.newWebSocketBuilder().header("Origin", base)
                    .buildAsync(URI.create("ws://localhost:" + puerto + "/ws/avisos"), escuchaAsignado)
                    .get(5, TimeUnit.SECONDS);

            String cuerpo = "{\"nombre\":\"Tarea nueva\",\"prioridad\":\"Alta\","
                    + "\"fechaFin\":\"2026-12-01\",\"idProyecto\":" + idProyecto
                    + ",\"idResponsable\":" + asignado.getIdUsuario() + "}";
            String token = tokenDashboard(clienteResponsable, base);
            var creada = clienteResponsable.send(HttpRequest.newBuilder(URI.create(base + "/api/miembro3/tareas"))
                            .header("Content-Type", "application/json").header("X-CSRF-TOKEN", token)
                            .POST(HttpRequest.BodyPublishers.ofString(cuerpo)).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(201, creada.statusCode(), creada.body());
            assertTrue(escuchaResponsable.siguiente().contains("\"recurso\":\"tareas\""));
            assertTrue(escuchaAsignado.siguiente().contains("\"recurso\":\"tareas\""));
            assertTrue(escuchaAsignado.siguiente().contains("\"tipo\":\"tarea_asignada\""));
            assertNull(escuchaResponsable.mensajes.poll(350, TimeUnit.MILLISECONDS));

            var fallida = clienteResponsable.send(HttpRequest.newBuilder(URI.create(base + "/api/miembro3/tareas"))
                            .header("Content-Type", "application/json").header("X-CSRF-TOKEN", token)
                            .POST(HttpRequest.BodyPublishers.ofString(cuerpo.replace("\"prioridad\":\"Alta\"",
                                    "\"prioridad\":\"Desconocida\""))).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(400, fallida.statusCode());
            assertNull(escuchaResponsable.mensajes.poll(350, TimeUnit.MILLISECONDS));
            assertNull(escuchaAsignado.mensajes.poll(350, TimeUnit.MILLISECONDS));

            String tokenAsignado = tokenDashboard(clienteAsignado, base);
            var salida = clienteAsignado.send(HttpRequest.newBuilder(URI.create(base + "/logout"))
                            .header("Content-Type", "application/x-www-form-urlencoded")
                            .POST(HttpRequest.BodyPublishers.ofString("_csrf="
                                    + URLEncoder.encode(tokenAsignado, StandardCharsets.UTF_8))).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(302, salida.statusCode());
            var nueva = clienteResponsable.send(HttpRequest.newBuilder(URI.create(base + "/api/miembro3/tareas"))
                            .header("Content-Type", "application/json").header("X-CSRF-TOKEN", token)
                            .POST(HttpRequest.BodyPublishers.ofString(cuerpo.replace("Tarea nueva", "Otra tarea")))
                            .build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(201, nueva.statusCode(), nueva.body());
            assertTrue(escuchaResponsable.siguiente().contains("\"recurso\":\"tareas\""));
            assertNull(escuchaAsignado.mensajes.poll(350, TimeUnit.MILLISECONDS));
        } finally {
            if (socketResponsable != null) socketResponsable.abort();
            if (socketAsignado != null) socketAsignado.abort();
            if (idProyecto != 0) proyectos.eliminarProyecto(idProyecto);
            usuarios.delete(asignado);
            usuarios.delete(responsable);
        }
    }

    private Usuario usuario(String nombre) {
        Usuario usuario = new Usuario();
        usuario.setNombre(nombre);
        usuario.setEmail(UUID.randomUUID() + "@ejemplo.test");
        usuario.setPassword(encoder.encode("clave-prueba-123"));
        usuario.setRol("USUARIO");
        return usuarios.save(usuario);
    }

    private HttpClient iniciarSesion(String base, Usuario usuario) throws Exception {
        var cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        HttpClient cliente = HttpClient.newBuilder().cookieHandler(cookies)
                .followRedirects(HttpClient.Redirect.NEVER).build();
        String pagina = cliente.send(HttpRequest.newBuilder(URI.create(base + "/login")).GET().build(),
                HttpResponse.BodyHandlers.ofString()).body();
        String token = extraer(pagina, "name=\"_csrf\"[^>]*value=\"([^\"]+)\"");
        String datos = "correo=" + URLEncoder.encode(usuario.getEmail(), StandardCharsets.UTF_8)
                + "&contrasena=clave-prueba-123&_csrf=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
        var respuesta = cliente.send(HttpRequest.newBuilder(URI.create(base + "/login"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(datos)).build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(302, respuesta.statusCode(), respuesta.body());
        assertTrue(respuesta.headers().firstValue("Location").orElseThrow().endsWith("/dashboard"));
        return cliente;
    }

    private String tokenDashboard(HttpClient cliente, String base) throws Exception {
        String pagina = cliente.send(HttpRequest.newBuilder(URI.create(base + "/dashboard")).GET().build(),
                HttpResponse.BodyHandlers.ofString()).body();
        return extraer(pagina, "name=\"_csrf\" content=\"([^\"]+)\"");
    }

    private String extraer(String pagina, String patron) {
        var coincidencia = Pattern.compile(patron).matcher(pagina);
        assertTrue(coincidencia.find(), "Falta el token CSRF en la página");
        return coincidencia.group(1);
    }

    private static class Escucha implements WebSocket.Listener {
        final LinkedBlockingQueue<String> mensajes = new LinkedBlockingQueue<>();
        final StringBuilder fragmentos = new StringBuilder();

        @Override
        public void onOpen(WebSocket socket) { socket.request(1); }

        @Override
        public CompletableFuture<?> onText(WebSocket socket, CharSequence texto, boolean ultimo) {
            fragmentos.append(texto);
            if (ultimo) {
                mensajes.add(fragmentos.toString());
                fragmentos.setLength(0);
            }
            socket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        String siguiente() throws InterruptedException {
            String mensaje = mensajes.poll(5, TimeUnit.SECONDS);
            assertNotNull(mensaje, "No llegó el evento WebSocket");
            return mensaje;
        }
    }
}
