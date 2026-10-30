package com.gestionproyectos.service;

import com.gestionproyectos.dto.ProyectoRequest;
import com.gestionproyectos.model.Proyecto;
import com.gestionproyectos.model.ProyectoUsuario;
import com.gestionproyectos.model.Usuario;
import com.gestionproyectos.repository.ProyectoRepository;
import com.gestionproyectos.repository.ProyectoUsuarioRepository;
import com.gestionproyectos.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProyectoServicioTests {

    @Test
    void guardaResponsableEnProyectoYComoMiembroDelEquipo() {
        ProyectoRepository proyectos = mock(ProyectoRepository.class);
        ProyectoUsuarioRepository equipos = mock(ProyectoUsuarioRepository.class);
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        ProyectoServicio servicio = new ProyectoServicio(proyectos, equipos, usuarios);

        Usuario responsable = new Usuario();
        responsable.setIdUsuario(7);
        responsable.setNombre("Ana");
        when(usuarios.findFirstByNombreIgnoreCase("Ana")).thenReturn(Optional.of(responsable));
        when(proyectos.save(any(Proyecto.class))).thenAnswer(invocacion -> {
            Proyecto proyecto = invocacion.getArgument(0);
            proyecto.setIdProyecto(3);
            return proyecto;
        });
        List<ProyectoUsuario> miembros = new ArrayList<>();
        when(equipos.save(any(ProyectoUsuario.class))).thenAnswer(invocacion -> {
            ProyectoUsuario miembro = invocacion.getArgument(0);
            miembros.add(miembro);
            return miembro;
        });
        when(equipos.buscarPorProyecto(3)).thenAnswer(invocacion -> miembros);

        ProyectoRequest solicitud = new ProyectoRequest("Proyecto", "Descripción",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                "Alta", "Ana", List.of(), null, null);
        var respuesta = servicio.guardarProyecto(solicitud);

        assertEquals("Ana", respuesta.responsable());
        assertEquals(List.of("Ana"), respuesta.integrantes());
        assertEquals(1, miembros.size());
        assertEquals("Responsable", miembros.getFirst().getRol());
        assertSame(responsable, miembros.getFirst().getProyecto().getResponsable());
    }
}
