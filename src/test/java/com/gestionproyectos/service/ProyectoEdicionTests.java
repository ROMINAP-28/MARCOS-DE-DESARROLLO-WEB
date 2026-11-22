package com.gestionproyectos.service;

import com.gestionproyectos.dto.ProyectoRequest;
import com.gestionproyectos.exception.SolicitudInvalidaException;
import com.gestionproyectos.model.Tarea;
import com.gestionproyectos.model.Usuario;
import com.gestionproyectos.repository.ProyectoRepository;
import com.gestionproyectos.repository.ProyectoUsuarioRepository;
import com.gestionproyectos.repository.TareaRepository;
import com.gestionproyectos.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ProyectoEdicionTests {
    @Autowired ProyectoServicio servicio;
    @Autowired ProyectoRepository proyectos;
    @Autowired ProyectoUsuarioRepository participaciones;
    @Autowired TareaRepository tareas;
    @Autowired UsuarioRepository usuarios;

    @Test
    void editarConservaIdFechaYEstadoDeMiembrosExistentes() {
        Usuario responsable = usuario("Ana");
        Usuario miembro = usuario("Bruno");
        Usuario nuevo = usuario("Carla");
        int id = servicio.guardarProyecto(solicitud("Original", responsable, List.of(miembro.getIdUsuario())))
                .id();
        var originales = participaciones.buscarPorProyecto(id);
        var fecha = originales.stream().filter(p -> p.getUsuario().getIdUsuario().equals(miembro.getIdUsuario()))
                .findFirst().orElseThrow().getFechaAsignacion();
        originales.stream().filter(p -> p.getUsuario().getIdUsuario().equals(miembro.getIdUsuario()))
                .findFirst().orElseThrow().setEstado("Inactivo");

        long antes = proyectos.count();
        var respuesta = servicio.actualizarProyecto(id, solicitud("Editado", responsable,
                List.of(miembro.getIdUsuario(), nuevo.getIdUsuario())));
        participaciones.flush();

        assertEquals(id, respuesta.id());
        assertEquals(antes, proyectos.count());
        assertEquals("Editado", proyectos.findById(id).orElseThrow().getNombre());
        var miembroPersistido = participaciones.buscarPorProyecto(id).stream()
                .filter(p -> p.getUsuario().getIdUsuario().equals(miembro.getIdUsuario()))
                .findFirst().orElseThrow();
        assertEquals("Inactivo", miembroPersistido.getEstado());
        assertEquals(fecha, miembroPersistido.getFechaAsignacion());
        assertEquals(3, participaciones.buscarPorProyecto(id).size());

        servicio.actualizarProyecto(id, solicitud("Con nuevo responsable", nuevo,
                List.of(miembro.getIdUsuario(), responsable.getIdUsuario())));
        assertEquals("Responsable", participaciones.buscarPorProyecto(id).stream()
                .filter(p -> p.getUsuario().getIdUsuario().equals(nuevo.getIdUsuario()))
                .findFirst().orElseThrow().getRol());
        assertEquals("Integrante", participaciones.buscarPorProyecto(id).stream()
                .filter(p -> p.getUsuario().getIdUsuario().equals(responsable.getIdUsuario()))
                .findFirst().orElseThrow().getRol());
    }

    @Test
    void impideRetirarIntegranteConTareasYEliminaProyectoSoloSiSeConfirmaEnCliente() {
        Usuario responsable = usuario("Ana");
        Usuario miembro = usuario("Bruno");
        int id = servicio.guardarProyecto(solicitud("Temporal", responsable, List.of(miembro.getIdUsuario())))
                .id();
        Tarea tarea = new Tarea();
        tarea.setNombre("Asignada");
        tarea.setProyecto(proyectos.findById(id).orElseThrow());
        tarea.setResponsable(miembro);
        tareas.saveAndFlush(tarea);

        assertThrows(SolicitudInvalidaException.class, () -> servicio.actualizarProyecto(id,
                solicitud("No debe cambiar", responsable, List.of())));
        assertEquals("Temporal", proyectos.findById(id).orElseThrow().getNombre());
        assertEquals(2, participaciones.buscarPorProyecto(id).size());

        servicio.eliminarProyecto(id);
        proyectos.flush();
        assertFalse(proyectos.existsById(id));
    }

    @Test
    void seleccionaUsuariosPorIdAunqueCoincidanSusNombres() {
        Usuario primero = usuario("Nombre repetido");
        Usuario segundo = usuario("Nombre repetido");
        int id = servicio.guardarProyecto(new ProyectoRequest("Con nombres iguales", "Descripción",
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 31), "Alta",
                "Nombre que no existe", List.of("Integrante inexistente"), "Planificado", 0,
                segundo.getIdUsuario(), List.of(primero.getIdUsuario()))).id();
        assertEquals(segundo.getIdUsuario(), proyectos.findById(id).orElseThrow()
                .getResponsable().getIdUsuario());
        assertEquals(2, participaciones.buscarPorProyecto(id).size());
        assertTrue(participaciones.buscarPorProyecto(id).stream().anyMatch(p ->
                p.getUsuario().getIdUsuario().equals(primero.getIdUsuario())));
    }

    @Test
    void aceptaPrioridadesYEstadosPermitidosEnProyectos() {
        Usuario responsable = usuario("Responsable valores");
        String[] prioridades = {"Alta", "Media", "Baja", "Alta"};
        String[] estados = {"Planificado", "En progreso", "Completado", "Atrasado"};
        for (int i = 0; i < estados.length; i++) {
            var guardado = servicio.guardarProyecto(new ProyectoRequest("Valores " + i, "Descripción",
                    LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 31), prioridades[i],
                    null, null, estados[i], null, responsable.getIdUsuario(), List.of()));
            assertEquals(prioridades[i], guardado.prioridad());
            assertEquals(estados[i], guardado.estado());
        }
    }

    @Test
    void rechazaIntegrantesPorNombreAunqueElResponsableTengaId() {
        Usuario responsable = usuario("Responsable");
        assertThrows(SolicitudInvalidaException.class, () -> servicio.guardarProyecto(
                new ProyectoRequest("Sin IDs", "Descripción", LocalDate.of(2026, 11, 1),
                        LocalDate.of(2026, 12, 31), "Alta", responsable.getNombre(),
                        List.of("Integrante"), "Planificado", 0, responsable.getIdUsuario(), null)));
    }

    private ProyectoRequest solicitud(String nombre, Usuario responsable, List<Integer> integrantes) {
        return new ProyectoRequest(nombre, "Descripción", LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 12, 31), "Alta", responsable.getNombre(), List.of(),
                "Planificado", 0, responsable.getIdUsuario(), integrantes);
    }

    private Usuario usuario(String nombre) {
        Usuario usuario = new Usuario();
        usuario.setNombre(nombre);
        usuario.setEmail(UUID.randomUUID() + "@ejemplo.test");
        usuario.setPassword("hash-de-prueba");
        usuario.setRol("USUARIO");
        return usuarios.save(usuario);
    }
}
