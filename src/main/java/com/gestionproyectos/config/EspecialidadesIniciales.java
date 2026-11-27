package com.gestionproyectos.config;

import com.gestionproyectos.model.Especialidad;
import com.gestionproyectos.repository.EspecialidadRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
@Configuration
public class EspecialidadesIniciales {

    @Bean
    CommandLineRunner cargarEspecialidades(EspecialidadRepository especialidades) {
        return args -> {
            if (especialidades.count() > 0) return;

            List.of(
                    new String[]{"Jefe de Proyecto", "Planifica, supervisa y responde por el avance de los proyectos."},
                    new String[]{"Coordinador de Proyecto", "Coordina equipos, actividades y recursos."},
                    new String[]{"Desarrollador Backend", "Servicios, base de datos y lógica del servidor."},
                    new String[]{"Desarrollador Frontend", "Interfaces web y experiencia de uso."},
                    new String[]{"Analista de Sistemas", "Levantamiento y análisis de requerimientos."},
                    new String[]{"Diseñador UX/UI", "Diseño de interfaces y experiencia de usuario."},
                    new String[]{"Soporte Técnico", "Atención y resolución de incidencias."},
                    new String[]{"Administración de Sistemas", "Infraestructura, accesos y mantenimiento."},
                    new String[]{"Recursos Humanos", "Gestión del personal y de los equipos."},
                    new String[]{"Finanzas", "Control de presupuestos y costos."}
            ).forEach(dato -> {
                Especialidad e = new Especialidad();
                e.setNombreEspecialidad(dato[0]);
                e.setDescripcion(dato[1]);
                especialidades.save(e);
            });
        };
    }
}
