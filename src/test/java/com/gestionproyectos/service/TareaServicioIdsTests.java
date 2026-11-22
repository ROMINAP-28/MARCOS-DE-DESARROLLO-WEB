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
                "Alta", "Pendiente", LocalDate.of(2026, 12, 1), "Otra persona", 10, 20));

        assertEquals(30, resultado.id());
        assertEquals("Ana", resultado.responsable());
        assertEquals("Proyecto real", resultado.proyecto());
        verify(proyectos).findById(10);
        verifyNoMoreInteractions(proyectos);
        verify(usuarios, never()).findFirstByNombreIgnoreCase(any());
    }

    @Test
    void rechazaNombresSinIdsSinConsultarPorNombre() {
        var tareas = mock(TareaRepository.class);
        var proyectos = mock(ProyectoRepository.class);
        var usuarios = mock(UsuarioRepository.class);
        var servicio = new TareaServicio(tareas, proyectos, usuarios);

        assertThrows(SolicitudInvalidaException.class, () -> servicio.guardarTarea(
                new TareaRequest("Verificar", "", "Proyecto real", "Alta", "Pendiente",
                        LocalDate.of(2026, 12, 1), "Ana", null, null)));
        verifyNoInteractions(proyectos, usuarios, tareas);
    }
}
