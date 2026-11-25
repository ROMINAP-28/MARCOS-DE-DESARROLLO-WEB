package com.gestionproyectos.service;

import com.gestionproyectos.config.UsuarioPrincipal;
import com.gestionproyectos.config.HandshakeSesionInterceptor;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AvisosTiempoReal extends TextWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(AvisosTiempoReal.class);
    private final Map<Integer, Set<WebSocketSession>> porUsuario = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Integer id = usuarioAutenticado(session);
        if (id == null || !sesionHttpVigente(session)) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        porUsuario.computeIfAbsent(id, ignored -> ConcurrentHashMap.newKeySet())
                .add(new ConcurrentWebSocketSessionDecorator(session, 10_000, 16_384));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        retirar(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        retirar(session);
        session.close(CloseStatus.SERVER_ERROR);
    }

    // No se aceptan mensajes ni identificadores de destinatario elegidos por clientes.
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        session.close(CloseStatus.POLICY_VIOLATION);
    }

    public void proyectoCambiado() {
        despuesDelCommit(() -> publicarATodos("{\"tipo\":\"datos_actualizados\",\"recurso\":\"proyectos\"}"));
    }

    public void tareaCambiada() {
        despuesDelCommit(() -> publicarATodos("{\"tipo\":\"datos_actualizados\",\"recurso\":\"tareas\"}"));
    }

    public void equipoCambiado() {
        despuesDelCommit(() -> publicarATodos("{\"tipo\":\"datos_actualizados\",\"recurso\":\"equipo\"}"));
    }

    public void tareaAsignada(Integer idUsuario, Integer idTarea) {
        if (idUsuario == null || idTarea == null) return;
        despuesDelCommit(() -> publicarAUsuario(idUsuario, "{\"tipo\":\"tarea_asignada\","
                + "\"idTarea\":" + idTarea + ",\"mensaje\":\"Se te asignó una tarea.\"}"));
    }

    private void despuesDelCommit(Runnable publicar) {
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() { publicar.run(); }
            });
        } else {
            publicar.run();
        }
    }

    private Integer usuarioAutenticado(WebSocketSession session) {
        return session.getPrincipal() instanceof Authentication auth
                && auth.isAuthenticated() && auth.getPrincipal() instanceof UsuarioPrincipal usuario
                ? usuario.getId() : null;
    }

    private void publicarATodos(String mensaje) {
        for (Integer id : porUsuario.keySet()) publicarAUsuario(id, mensaje);
    }

    private void publicarAUsuario(Integer id, String mensaje) {
        for (WebSocketSession session : porUsuario.getOrDefault(id, Set.of())) {
            if (!session.isOpen() || !sesionHttpVigente(session)) {
                retirar(session);
                if (session.isOpen()) {
                    try { session.close(CloseStatus.POLICY_VIOLATION); } catch (IOException ignored) { }
                }
                continue;
            }
            try {
                session.sendMessage(new TextMessage(mensaje));
            } catch (IOException | IllegalStateException e) {
                logger.debug("No se pudo entregar aviso WebSocket", e);
                retirar(session);
                try { session.close(CloseStatus.SERVER_ERROR); } catch (IOException ignored) { }
            }
        }
    }

    private void retirar(WebSocketSession session) {
        Integer id = usuarioAutenticado(session);
        if (id == null) return;
        Set<WebSocketSession> sesiones = porUsuario.get(id);
        if (sesiones != null) {
            sesiones.removeIf(actual -> actual.getId().equals(session.getId()));
            if (sesiones.isEmpty()) porUsuario.remove(id, sesiones);
        }
    }

    private boolean sesionHttpVigente(WebSocketSession session) {
        Object sesion = session.getAttributes().get(HandshakeSesionInterceptor.SESION_HTTP);
        if (!(sesion instanceof HttpSession http)) return false;
        try {
            http.getCreationTime(); // Una sesión invalidada lanza IllegalStateException.
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }
}
