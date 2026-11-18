package com.gestionproyectos.service;

import com.gestionproyectos.dto.EstadoParticipacionRequest;
import com.gestionproyectos.dto.ParticipacionRequest;
import com.gestionproyectos.exception.RecursoDuplicadoException;
import com.gestionproyectos.exception.SolicitudInvalidaException;
import com.gestionproyectos.model.Proyecto;
import com.gestionproyectos.model.ProyectoUsuario;
import com.gestionproyectos.model.ProyectoUsuarioId;
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

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ParticipacionServicioTests {
    @Autowired ParticipacionServicio servicio;
    @Autowired ProyectoRepository proyectos;
    @Autowired UsuarioRepository usuarios;
    @Autowired ProyectoUsuarioRepository participaciones;
    @Autowired TareaRepository tareas;

    @Test
    void agregaActualizaYRetiraParticipacionEnPostgreSQL() {
        Usuario responsable = usuario("Responsable");
        Usuario miembro = usuario("Integrante");
        Proyecto proyecto = proyecto(responsable);

        var guardada = servicio.agregar(new ParticipacionRequest(proyecto.getIdProyecto(), miembro.getIdUsuario(), "Activo"));
        ProyectoUsuarioId id = clave(proyecto, miembro);
        assertEquals("Integrante", guardada.rol());
        assertEquals("Activo", participaciones.findById(id).orElseThrow().getEstado());
        assertNotNull(guardada.fechaAsignacion());
        assertThrows(RecursoDuplicadoException.class,
                () -> servicio.agregar(new ParticipacionRequest(proyecto.getIdProyecto(), miembro.getIdUsuario(), "Activo")));
        assertThrows(SolicitudInvalidaException.class,
                () -> servicio.cambiarEstado(proyecto.getIdProyecto(), miembro.getIdUsuario(),
                        new EstadoParticipacionRequest(null)));

        servicio.cambiarEstado(proyecto.getIdProyecto(), miembro.getIdUsuario(),
                new EstadoParticipacionRequest("Inactivo"));
        assertEquals("Inactivo", participaciones.findById(id).orElseThrow().getEstado());
        assertTrue(servicio.listar().stream().anyMatch(p -> p.idProyecto().equals(proyecto.getIdProyecto())
                && p.idUsuario().equals(miembro.getIdUsuario())));

        servicio.retirar(proyecto.getIdProyecto(), miembro.getIdUsuario());
        participaciones.flush();
        assertFalse(participaciones.existsById(id));
    }

    @Test
    void noRetiraResponsableNiIntegranteConTareas() {
        Usuario responsable = usuario("Responsable");
        Usuario miembro = usuario("Integrante");
        Proyecto proyecto = proyecto(responsable);
        ProyectoUsuario vinculoResponsable = new ProyectoUsuario();
        vinculoResponsable.setId(clave(proyecto, responsable));
        vinculoResponsable.setProyecto(proyecto);
        vinculoResponsable.setUsuario(responsable);
        vinculoResponsable.setRol("Responsable");
        vinculoResponsable.setEstado("Activo");
        vinculoResponsable.setFechaAsignacion(LocalDateTime.now());
        participaciones.save(vinculoResponsable);
        assertThrows(SolicitudInvalidaException.class,
                () -> servicio.retirar(proyecto.getIdProyecto(), responsable.getIdUsuario()));

        servicio.agregar(new ParticipacionRequest(proyecto.getIdProyecto(), miembro.getIdUsuario(), "Activo"));
        Tarea tarea = new Tarea();
        tarea.setNombre("Tarea asignada");
        tarea.setProyecto(proyecto);
        tarea.setResponsable(miembro);
        tareas.saveAndFlush(tarea);
        assertEquals(1, servicio.listar().stream()
                .filter(p -> p.idProyecto().equals(proyecto.getIdProyecto())
                        && p.idUsuario().equals(miembro.getIdUsuario()))
                .findFirst().orElseThrow().tareasAsignadas());
        assertThrows(SolicitudInvalidaException.class,
                () -> servicio.retirar(proyecto.getIdProyecto(), miembro.getIdUsuario()));
    }

    private Usuario usuario(String nombre) {
        Usuario usuario = new Usuario();
        usuario.setNombre(nombre);
        usuario.setEmail(UUID.randomUUID() + "@ejemplo.test");
        usuario.setPassword("hash-de-prueba");
        usuario.setRol("USUARIO");
        return usuarios.save(usuario);
    }

    private Proyecto proyecto(Usuario responsable) {
        Proyecto proyecto = new Proyecto();
        proyecto.setNombre("Proyecto temporal");
        proyecto.setResponsable(responsable);
        return proyectos.save(proyecto);
    }

    private ProyectoUsuarioId clave(Proyecto proyecto, Usuario usuario) {
        ProyectoUsuarioId id = new ProyectoUsuarioId();
        id.setIdProyecto(proyecto.getIdProyecto());
        id.setIdUsuario(usuario.getIdUsuario());
        return id;
    }
}
