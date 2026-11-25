package com.gestionproyectos.config;

import com.gestionproyectos.service.AvisosTiempoReal;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final AvisosTiempoReal avisos;
    private final HandshakeSesionInterceptor sesion;

    public WebSocketConfig(AvisosTiempoReal avisos, HandshakeSesionInterceptor sesion) {
        this.avisos = avisos;
        this.sesion = sesion;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Sin orígenes adicionales: Spring acepta únicamente el mismo origen.
        registry.addHandler(avisos, "/ws/avisos").addInterceptors(sesion);
    }
}
