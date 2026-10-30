package com.gestionproyectos.config;

import com.gestionproyectos.model.*;
import com.gestionproyectos.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Carga datos de ejemplo SOLO si no hay proyectos (los usuarios se crean si faltan), para que las pantallas
 * de Proyectos y Tareas tengan responsables/proyectos que seleccionar.
 * Los usuarios de ejemplo reciben una contraseña aleatoria: no se puede iniciar sesión con ellos.
 * Para no usarlo, borra esta clase.
 */
@Configuration
public class DatosIniciales {

    private static final Logger logger = LoggerFactory.getLogger(DatosIniciales.class);

    @Bean
    CommandLineRunner cargarDatosEjemplo(UsuarioRepository usuarios,
                                         ProyectoRepository proyectos,
                                         ProyectoUsuarioRepository equipos,
                                         TareaRepository tareas,
                                         PasswordEncoder encoder) {
        return args -> {
            if (proyectos.count() > 0) return;

            Map<String, Usuario> u = new HashMap<>();
            for (String nombre : List.of("María López", "Carlos Torres", "Ana García", "José Ramírez",
                    "Brayan Quispe", "Ana Torres", "Carlos Ruiz", "Juan Pérez")) {
                Usuario usuario = usuarios.findFirstByNombreIgnoreCase(nombre).orElseGet(() -> {
                    Usuario nuevo = new Usuario();
                    nuevo.setNombre(nombre);
                    nuevo.setEmail(correo(nombre));
                    nuevo.setPassword(encoder.encode(UUID.randomUUID().toString()));
                    nuevo.setRol("USUARIO");
                    nuevo.setFechaRegistro(LocalDateTime.now());
                    return usuarios.save(nuevo);
                });
                u.put(nombre, usuario);
            }

            Proyecto p1 = proyecto(proyectos, equipos, u, "Plataforma web colaborativa",
                    "Sistema para organizar equipos, tareas y proyectos.", "2026-09-10", "2026-10-30",
                    "Alta", "En progreso", 65f, "María López", "Carlos Torres", "Ana García");
            Proyecto p2 = proyecto(proyectos, equipos, u, "Sistema de inventario",
                    "Aplicación para administrar productos y existencias.", "2026-09-15", "2026-11-15",
                    "Media", "Planificado", 0f, "Carlos Torres", "José Ramírez");
            Proyecto p3 = proyecto(proyectos, equipos, u, "Portal de reportes",
                    "Panel de consulta y generación de informes.", "2026-08-01", "2026-09-20",
                    "Baja", "Completado", 100f, "Ana García", "Brayan Quispe");

            tarea(tareas, p1, u.get("Ana Torres"), "Diseñar la interfaz",
                    "Preparar las vistas principales del sistema.", "Alta", "Pendiente", "2026-09-30");
            tarea(tareas, p1, u.get("Carlos Ruiz"), "Conectar base de datos",
                    "Configurar la conexión con PostgreSQL.", "Urgente", "En progreso", "2026-10-02");
            tarea(tareas, p2, u.get("María López"), "Revisar requisitos",
                    "Validar los requisitos funcionales.", "Media", "Pendiente", "2026-10-05");
            tarea(tareas, p1, u.get("Juan Pérez"), "Crear estructura HTML",
                    "Implementar la estructura inicial de las páginas.", "Baja", "Completada", "2026-09-25");
            tarea(tareas, p3, u.get("Ana Torres"), "Implementar autenticación",
                    "Preparar el inicio de sesión de usuarios.", "Alta", "En progreso", "2026-10-08");
            logger.info("Datos de ejemplo cargados.");
        };
    }

    private String correo(String nombre) {
        String base = java.text.Normalizer.normalize(nombre, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase().replace(' ', '.');
        return base + "@ejemplo.local";
    }

    private Proyecto proyecto(ProyectoRepository proyectos, ProyectoUsuarioRepository equipos,
                              Map<String, Usuario> u, String nombre, String descripcion,
                              String inicio, String fin, String prioridad, String estado,
                              float avance, String responsable, String... integrantes) {
        Proyecto p = new Proyecto();
        p.setNombre(nombre);
        p.setDescripcion(descripcion);
        p.setFechaInicio(LocalDate.parse(inicio));
        p.setFechaFin(LocalDate.parse(fin));
        p.setPrioridad(prioridad);
        p.setEstado(estado);
        p.setPorcentajeAvance(avance);
        p.setResponsable(u.get(responsable));
        p = proyectos.save(p);

        asignar(equipos, p, u.get(responsable), "Responsable");
        for (String nombreIntegrante : integrantes) {
            if (!nombreIntegrante.equals(responsable)) {
                asignar(equipos, p, u.get(nombreIntegrante), "Integrante");
            }
        }
        return p;
    }

    private void asignar(ProyectoUsuarioRepository equipos, Proyecto p, Usuario usuario, String rol) {
        ProyectoUsuarioId id = new ProyectoUsuarioId();
        id.setIdProyecto(p.getIdProyecto());
        id.setIdUsuario(usuario.getIdUsuario());
        ProyectoUsuario pu = new ProyectoUsuario();
        pu.setId(id);
        pu.setProyecto(p);
        pu.setUsuario(usuario);
        pu.setRol(rol);
        pu.setEstado("Activo");
        pu.setFechaAsignacion(LocalDateTime.now());
        equipos.save(pu);
    }

    private void tarea(TareaRepository tareas, Proyecto p, Usuario responsable, String nombre,
                       String descripcion, String prioridad, String estado, String fechaFin) {
        Tarea t = new Tarea();
        t.setNombre(nombre);
        t.setDescripcion(descripcion);
        t.setPrioridad(prioridad);
        t.setEstado(estado);
        t.setFechaFin(LocalDate.parse(fechaFin));
        t.setFechaCreacion(LocalDateTime.now());
        t.setProyecto(p);
        t.setResponsable(responsable);
        tareas.save(t);
    }
}
