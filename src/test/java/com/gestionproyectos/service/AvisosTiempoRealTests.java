package com.gestionproyectos.service;

import com.gestionproyectos.config.UsuarioPrincipal;
import com.gestionproyectos.config.HandshakeSesionInterceptor;
import com.gestionproyectos.model.Usuario;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.security.Principal;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AvisosTiempoRealTests {
    @Test
    void avisoDirigidoSoloLlegaAlUsuarioAsignadoYElGlobalALosAutenticados() throws Exception {
        var avisos = new AvisosTiempoReal();
        WebSocketSession uno = sesion(1, "uno");
        WebSocketSession dos = sesion(2, "dos");
        avisos.afterConnectionEstablished(uno);
        avisos.afterConnectionEstablished(dos);

        avisos.tareaAsignada(2, 17);
        verify(uno, never()).sendMessage(any());
        verify(dos).sendMessage(argThat(m -> m instanceof TextMessage texto
                && texto.getPayload().contains("\"idTarea\":17")));
        clearInvocations(uno, dos);
        avisos.tareaCambiada();
        verify(uno).sendMessage(any(TextMessage.class));
        verify(dos).sendMessage(any(TextMessage.class));

        avisos.afterConnectionClosed(dos, CloseStatus.NORMAL);
        clearInvocations(uno, dos);
        avisos.proyectoCambiado();
        verify(uno).sendMessage(any(TextMessage.class));
        verify(dos, never()).sendMessage(any());
    }

    @Test
    void sinPrincipalAutenticadoSeRechazaYNoSeAceptanMensajesEntrantes() throws Exception {
        var avisos = new AvisosTiempoReal();
        WebSocketSession anonima = mock(WebSocketSession.class);
        avisos.afterConnectionEstablished(anonima);
        verify(anonima).close(CloseStatus.POLICY_VIOLATION);
        avisos.handleMessage(anonima, new TextMessage("{\"idUsuario\":1}"));
        verify(anonima, times(2)).close(CloseStatus.POLICY_VIOLATION);
        avisos.tareaAsignada(1, 1);
        verify(anonima, never()).sendMessage(any());
    }

    @Test
    void noSePublicaAntesDeConfirmarNiCuandoHayRollback() throws Exception {
        var avisos = new AvisosTiempoReal();
        WebSocketSession sesion = sesion(1, "usuario");
        avisos.afterConnectionEstablished(sesion);
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            avisos.tareaAsignada(1, 17);
            avisos.proyectoCambiado();
            verify(sesion, never()).sendMessage(any());
            // Un rollback no ejecuta afterCommit.
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(s -> s.afterCompletion(1));
            verify(sesion, never()).sendMessage(any());
            TransactionSynchronizationManager.clearSynchronization();
            TransactionSynchronizationManager.initSynchronization();
            avisos.tareaAsignada(1, 18);
            TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCommit());
            verify(sesion).sendMessage(argThat(m -> m instanceof TextMessage texto
                    && texto.getPayload().contains("\"idTarea\":18")));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }

    @Test
    void invalidaSesionHttpAntesDeEntregarUnAvisoPendiente() throws Exception {
        var avisos = new AvisosTiempoReal();
        WebSocketSession sesion = sesion(1, "caducada");
        avisos.afterConnectionEstablished(sesion);
        HttpSession http = (HttpSession) sesion.getAttributes().get(HandshakeSesionInterceptor.SESION_HTTP);
        when(http.getCreationTime()).thenThrow(new IllegalStateException("Sesión invalidada"));
        avisos.tareaAsignada(1, 5);
        verify(sesion, never()).sendMessage(any());
        verify(sesion).close(CloseStatus.POLICY_VIOLATION);
    }

    private WebSocketSession sesion(int id, String sessionId) {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(id);
        usuario.setEmail(id + "@ejemplo.test");
        usuario.setPassword("hash-de-prueba");
        usuario.setRol("USUARIO");
        UsuarioPrincipal principal = new UsuarioPrincipal(usuario);
        Principal autenticacion = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        WebSocketSession sesion = mock(WebSocketSession.class);
        when(sesion.getPrincipal()).thenReturn(autenticacion);
        when(sesion.getId()).thenReturn(sessionId);
        when(sesion.isOpen()).thenReturn(true);
        when(sesion.getAttributes()).thenReturn(Map.of(HandshakeSesionInterceptor.SESION_HTTP,
                mock(HttpSession.class)));
        return sesion;
    }
}
