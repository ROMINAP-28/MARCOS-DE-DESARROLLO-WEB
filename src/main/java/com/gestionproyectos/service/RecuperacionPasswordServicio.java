package com.gestionproyectos.service;

import com.gestionproyectos.model.RecuperacionPassword;
import com.gestionproyectos.repository.RecuperacionPasswordRepository;
import com.gestionproyectos.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class RecuperacionPasswordServicio {
    private static final Logger logger = LoggerFactory.getLogger(RecuperacionPasswordServicio.class);
    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static final String ERROR_ENLACE = "El enlace no es válido o ha caducado.";
    private final UsuarioRepository usuarios;
    private final RecuperacionPasswordRepository recuperaciones;
    private final PasswordEncoder encoder;
    private final ObjectProvider<JavaMailSender> proveedoresCorreo;
    private final String remitente;
    private final String urlPublica;
    private final ConcurrentHashMap<String, LocalDateTime> solicitudes = new ConcurrentHashMap<>();

    public RecuperacionPasswordServicio(UsuarioRepository usuarios,
                                       RecuperacionPasswordRepository recuperaciones,
                                       PasswordEncoder encoder,
                                       ObjectProvider<JavaMailSender> proveedoresCorreo,
                                       @Value("${app.recuperacion.remitente:}") String remitente,
                                       @Value("${app.url-publica:http://localhost:8080}") String urlPublica) {
        this.usuarios = usuarios;
        this.recuperaciones = recuperaciones;
        this.encoder = encoder;
        this.proveedoresCorreo = proveedoresCorreo;
        this.remitente = remitente;
        this.urlPublica = urlPublica;
    }

    @Transactional
    public void solicitar(String correo) {
        JavaMailSender emisor = proveedoresCorreo.getIfAvailable();
        if (emisor == null || remitente.isBlank() || urlPublica.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "La recuperación por correo no está configurada.");
        }
        if (correo == null || correo.isBlank() || correo.length() > 150) return;
        String normalizado = correo.trim().toLowerCase(Locale.ROOT);
        LocalDateTime ahora = LocalDateTime.now();
        if (solicitudes.size() > 10_000) {
            solicitudes.entrySet().removeIf(e -> e.getValue().isBefore(ahora.minusHours(1)));
        }
        if (solicitudes.size() >= 10_000 && !solicitudes.containsKey(normalizado)) return;
        AtomicBoolean permitido = new AtomicBoolean(false);
        solicitudes.compute(normalizado, (clave, anterior) -> {
            if (anterior != null && anterior.isAfter(ahora.minusMinutes(1))) return anterior;
            permitido.set(true);
            return ahora;
        });
        if (!permitido.get()) return; // La respuesta no distingue si existe la cuenta.

        usuarios.findByEmailIgnoreCase(normalizado).ifPresent(usuario -> {
            byte[] bytes = new byte[32];
            ALEATORIO.nextBytes(bytes);
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            recuperaciones.deleteByUsuario_IdUsuario(usuario.getIdUsuario());
            RecuperacionPassword solicitud = new RecuperacionPassword();
            solicitud.setTokenHash(hash(token));
            solicitud.setCorreoSolicitud(usuario.getEmail());
            solicitud.setUsuario(usuario);
            solicitud.setExpira(ahora.plusMinutes(30));
            recuperaciones.saveAndFlush(solicitud);

            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(remitente);
            mensaje.setTo(usuario.getEmail());
            mensaje.setSubject("Restablecer contraseña - Gestión de Proyectos");
            mensaje.setText("Para restablecer tu contraseña, abre este enlace (válido 30 minutos y una sola vez):\n"
                    + urlPublica.replaceAll("/+$", "") + "/restablecer-password?token=" + token
                    + "\nSi no lo solicitaste, ignora este correo.");
            try {
                emisor.send(mensaje);
            } catch (MailException e) {
                recuperaciones.delete(solicitud);
                solicitudes.remove(normalizado, ahora);
                // Misma respuesta pública para cuenta existente o desconocida.
                logger.warn("No se pudo entregar un correo de recuperación");
                logger.debug("Detalle del fallo de entrega", e);
            }
        });
    }

    @Transactional
    public void restablecer(String token, String nuevaContrasena) {
        if (nuevaContrasena == null || nuevaContrasena.length() < 8 || nuevaContrasena.length() > 72) {
            throw new IllegalArgumentException("La contraseña debe tener entre 8 y 72 caracteres.");
        }
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) {
            throw new IllegalArgumentException(ERROR_ENLACE);
        }
        RecuperacionPassword solicitud = recuperaciones.findByTokenHash(hash(token))
                .orElseThrow(() -> new IllegalArgumentException(ERROR_ENLACE));
        if (solicitud.getConsumido() != null || !solicitud.getExpira().isAfter(LocalDateTime.now())
                || !solicitud.getCorreoSolicitud().equalsIgnoreCase(solicitud.getUsuario().getEmail())) {
            throw new IllegalArgumentException(ERROR_ENLACE);
        }
        solicitud.setConsumido(LocalDateTime.now());
        solicitud.getUsuario().setPassword(encoder.encode(nuevaContrasena));
    }

    private String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no está disponible", e);
        }
    }
}
