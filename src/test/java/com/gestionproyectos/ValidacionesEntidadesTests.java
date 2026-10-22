package com.gestionproyectos;

import com.gestionproyectos.model.Comentario;
import com.gestionproyectos.model.Especialidad;
import com.gestionproyectos.model.Notificacion;
import com.gestionproyectos.model.Proyecto;
import com.gestionproyectos.model.ProyectoUsuario;
import com.gestionproyectos.model.Tarea;
import com.gestionproyectos.model.Usuario;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidacionesEntidadesTests {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void iniciarValidador() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void cerrarValidador() {
        factory.close();
    }

    @Test
    void usuarioRechazaCorreoInvalidoYCadenasVacias() {
        Usuario usuario = new Usuario();
        usuario.setNombre(" ");
        usuario.setEmail("correo-invalido");
        usuario.setPassword(" ");
        usuario.setRol("USUARIO");

        assertTrue(validator.validate(usuario).stream()
                .anyMatch(error -> error.getPropertyPath().toString().equals("email")));
        assertTrue(validator.validate(usuario).stream()
                .anyMatch(error -> error.getPropertyPath().toString().equals("nombre")));
        assertTrue(validator.validate(usuario).stream()
                .anyMatch(error -> error.getPropertyPath().toString().equals("password")));
    }

    @Test
    void proyectoRechazaAvanceYFechasInvalidas() {
        Proyecto proyecto = new Proyecto();
        proyecto.setNombre("Proyecto");
        proyecto.setFechaInicio(LocalDate.of(2026, 10, 10));
        proyecto.setFechaFin(LocalDate.of(2026, 10, 9));
        proyecto.setPorcentajeAvance(101f);

        assertTrue(validator.validate(proyecto).stream()
                .anyMatch(error -> error.getPropertyPath().toString().equals("periodoValido")));
        assertTrue(validator.validate(proyecto).stream()
                .anyMatch(error -> error.getPropertyPath().toString().equals("porcentajeAvance")));
    }

    @Test
    void tareaExigeProyectoYResponsable() {
        Tarea tarea = new Tarea();
        tarea.setNombre("Diseñar interfaz");

        assertTrue(validator.validate(tarea).stream()
                .anyMatch(error -> error.getPropertyPath().toString().equals("proyecto")));
        assertTrue(validator.validate(tarea).stream()
                .anyMatch(error -> error.getPropertyPath().toString().equals("responsable")));
    }

    @Test
    void relacionesYCamposObligatoriosSeValidan() {
        assertFalse(validator.validate(new Comentario()).isEmpty());
        assertFalse(validator.validate(new Notificacion()).isEmpty());
        assertFalse(validator.validate(new Especialidad()).isEmpty());
        assertFalse(validator.validate(new ProyectoUsuario()).isEmpty());
    }
}
