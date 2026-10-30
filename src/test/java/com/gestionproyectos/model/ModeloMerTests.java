package com.gestionproyectos.model;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ModeloMerTests {

    @Test
    void entidadesYClavesForaneasDelMer() throws NoSuchFieldException {
        for (Class<?> tipo : new Class<?>[]{Especialidad.class, Usuario.class, Proyecto.class,
                ProyectoUsuario.class, Tarea.class, Recurso.class, Reporte.class,
                Notificacion.class, Comentario.class}) {
            assertNotNull(tipo.getAnnotation(Entity.class), tipo.getSimpleName());
        }
        assertEquals("id_responsable", Proyecto.class.getDeclaredField("responsable")
                .getAnnotation(JoinColumn.class).name());
        assertNotNull(Proyecto.class.getDeclaredField("responsable").getAnnotation(ManyToOne.class));
        assertEquals("id_proyecto", Recurso.class.getDeclaredField("proyecto")
                .getAnnotation(JoinColumn.class).name());
        assertEquals("id_usuario_generador", Reporte.class.getDeclaredField("usuarioGenerador")
                .getAnnotation(JoinColumn.class).name());
    }

    @Test
    void inicializaFechasSinSobrescribirValoresExistentes() {
        LocalDateTime anterior = LocalDateTime.of(2025, 1, 1, 12, 0);
        Proyecto proyecto = new Proyecto();
        proyecto.setFechaCreacion(anterior);
        proyecto.iniciarFechaCreacion();
        assertEquals(anterior, proyecto.getFechaCreacion());

        Tarea tarea = new Tarea();
        tarea.setFechaCreacion(anterior);
        tarea.iniciarFechas();
        assertEquals(anterior, tarea.getFechaAsignacion());

        Reporte reporte = new Reporte();
        reporte.iniciarFechaGeneracion();
        assertNotNull(reporte.getFechaGeneracion());

        Usuario usuario = new Usuario();
        usuario.iniciarFechaRegistro();
        assertNotNull(usuario.getFechaRegistro());
        Comentario comentario = new Comentario();
        comentario.iniciarFechaCreacion();
        assertNotNull(comentario.getFechaCreacion());
        Notificacion notificacion = new Notificacion();
        notificacion.iniciarFechaCreacion();
        assertNotNull(notificacion.getFechaCreacion());
    }

    @Test
    void recursoExigeDatosYUnaCantidadNoNegativa() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            Recurso recurso = new Recurso();
            recurso.setNombreRecurso("Servidor");
            recurso.setTipoRecurso("Equipo");
            recurso.setCantidad(-1);
            recurso.setProyecto(new Proyecto());
            assertTrue(validator.validate(recurso).stream()
                    .anyMatch(error -> error.getPropertyPath().toString().equals("cantidad")));
        }
    }
}
