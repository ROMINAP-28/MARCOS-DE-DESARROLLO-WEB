package com.gestionproyectos.config;

import com.gestionproyectos.repository.ProyectoRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component("alcanceLectura")
public class AlcanceLectura {
    private final ProyectoRepository proyectos;

    public AlcanceLectura(ProyectoRepository proyectos) {
        this.proyectos = proyectos;
    }

    public Set<Integer> idsProyectosVisibles() {
        var autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null || !(autenticacion.getPrincipal() instanceof UsuarioPrincipal usuario)) {
            return Set.of();
        }
        return Set.copyOf(proyectos.idsVisibles(usuario.getId()));
    }

    public boolean puedeVerProyecto(Integer idProyecto) {
        return idProyecto != null && idsProyectosVisibles().contains(idProyecto);
    }
}
