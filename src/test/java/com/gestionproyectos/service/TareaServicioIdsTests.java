package com.gestionproyectos.service;

import com.gestionproyectos.dto.TareaRequest;
import com.gestionproyectos.exception.SolicitudInvalidaException;
import com.gestionproyectos.model.Proyecto;
import com.gestionproyectos.model.Tarea;
import com.gestionproyectos.model.Usuario;
import com.gestionproyectos.repository.ProyectoRepository;
import com.gestionproyectos.repository.TareaRepository;
import com.gestionproyectos.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TareaServicioIdsTests {
    @Test
    void asignacionDeEquipoUsaIdsAunqueExistanNombresRepetidos() {
        var tareas = mock(TareaRepository.class);
        var proyectos = mock(ProyectoRepository.class);
        var usuarios = mock(UsuarioRepository.class);
        var servicio = new TareaServicio(tareas, proyectos, usuarios);

        Proyecto proyecto = new Proyecto();
        proyecto.setIdProyecto(10);
        proyecto.setNombre("Proyecto real");
        Usuario responsable = new Usuario();
        responsable.setIdUsuario(20);
        responsable.setNombre("Ana");
        when(proyectos.findById(10)).thenReturn(Optional.of(proyecto));
        when(usuarios.findById(20)).thenReturn(Optional.of(responsable));
        when(tareas.save(any(Tarea.class))).thenAnswer(invocacion -> {
            Tarea tarea = invocacion.getArgument(0);
            tarea.setIdTarea(30);
            return tarea;
        });

        var resultado = servicio.guardarTarea(new TareaRequest("Verificar", "", "Proyecto equivocado",
                "Alta", "Pendiente", LocalDate.of(2026, 12, 1), "Otra persona", 10, 20, null));

        assertEquals(30, resultado.id());
        assertEquals("Ana", resultado.responsable());
        assertEquals("Proyecto real", resultado.proyecto());
        verify(proyectos).findById(10);
        verifyNoMoreInteractions(proyectos);
        verify(usuarios, never()).findFirstByNombreIgnoreCase(any());
    }

    @Test
    void aceptaPrioridadesYEstadosPermitidosEnTareas() {
        var tareas = mock(TareaRepository.class);
        var proyectos = mock(ProyectoRepository.class);
        var usuarios = mock(UsuarioRepository.class);
        var servicio = new TareaServicio(tareas, proyectos, usuarios);
        Proyecto proyecto = new Proyecto();
        proyecto.setIdProyecto(10);
        proyecto.setNombre("Proyecto");
        Usuario responsable = new Usuario();
        responsable.setIdUsuario(20);
        responsable.setNombre("Ana");
        when(proyectos.findById(10)).thenReturn(Optional.of(proyecto));
        when(usuarios.findById(20)).thenReturn(Optional.of(responsable));
        when(tareas.save(any(Tarea.class))).thenAnswer(invocacion -> {
            Tarea guardada = invocacion.getArgument(0);
            guardada.setIdTarea(30);
            return guardada;
        });

        String[] prioridades = {"Baja", "Media", "Alta", "Urgente"};
        String[] estados = {"Pendiente", "En progreso", "Completada", "Pendiente"};
        for (int i = 0; i < prioridades.length; i++) {
            var guardada = servicio.guardarTarea(new TareaRequest("Trabajo", "", null,
                    prioridades[i], estados[i], LocalDate.of(2026, 12, 1), null, 10, 20, null));
            assertEquals(prioridades[i], guardada.prioridad());
            assertEquals(estados[i], guardada.estado());
        }
    }

    @Test
    void rechazaPeriodoInvalidoInclusoSinValidacionRestAntesDeMutarLaTarea() {
        var tareas = mock(TareaRepository.class);
        var proyectos = mock(ProyectoRepository.class);
        var usuarios = mock(UsuarioRepository.class);
        var servicio = new TareaServicio(tareas, proyectos, usuarios);
        var request = new TareaRequest("Trabajo", "Descripción", null, "Alta", "Pendiente",
                LocalDate.of(2026, 12, 1), null, 10, 20, LocalDate.of(2026, 12, 2));

        assertThrows(SolicitudInvalidaException.class, () -> servicio.guardarTarea(request));
        Tarea original = new Tarea();
        original.setNombre("Original");
        original.setFechaInicio(LocalDate.of(2026, 11, 1));
        original.setFechaFin(LocalDate.of(2026, 12, 1));
        when(tareas.findById(30)).thenReturn(Optional.of(original));
        assertThrows(SolicitudInvalidaException.class, () -> servicio.actualizarTarea(30, request));
        assertEquals("Original", original.getNombre());
        assertEquals(LocalDate.of(2026, 11, 1), original.getFechaInicio());
        assertEquals(LocalDate.of(2026, 12, 1), original.getFechaFin());
        verify(tareas, never()).save(any());
        verifyNoInteractions(proyectos, usuarios);
    }

    @Test
    void rechazaNombresSinIdsSinConsultarPorNombre() {
        var tareas = mock(TareaRepository.class);
        var proyectos = mock(ProyectoRepository.class);
        var usuarios = mock(UsuarioRepository.class);
        var servicio = new TareaServicio(tareas, proyectos, usuarios);

        assertThrows(SolicitudInvalidaException.class, () -> servicio.guardarTarea(
                new TareaRequest("Verificar", "", "Proyecto real", "Alta", "Pendiente",
                        LocalDate.of(2026, 12, 1), "Ana", null, null, null)));
        verifyNoInteractions(proyectos, usuarios, tareas);
    }
}
