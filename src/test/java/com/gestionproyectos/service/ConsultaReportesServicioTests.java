package com.gestionproyectos.service;

import com.gestionproyectos.dto.ReporteConsultaRequest;
import com.gestionproyectos.exception.RecursoNoEncontradoException;
import com.gestionproyectos.exception.SolicitudInvalidaException;
import com.gestionproyectos.model.*;
import com.gestionproyectos.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ConsultaReportesServicioTests {
    @Autowired ConsultaReportesServicio servicio;
    @Autowired ProyectoRepository proyectos;
    @Autowired UsuarioRepository usuarios;
    @Autowired TareaRepository tareas;
    @Autowired ProyectoUsuarioRepository participaciones;
    @Autowired RecursoRepository recursos;

    @Test
    void filtraRegistrosPostgreSQLPorProyectoYFechasSegunTipo() {
        Usuario usuario = new Usuario();
        usuario.setNombre("Usuario reporte");
        usuario.setEmail(UUID.randomUUID() + "@ejemplo.test");
        usuario.setPassword("hash-de-prueba");
        usuario.setRol("USUARIO");
        usuario = usuarios.save(usuario);

        Proyecto primero = proyecto("Primero", usuario, LocalDate.of(2026, 11, 10), "En progreso", 60f);
        Proyecto segundo = proyecto("Segundo", usuario, LocalDate.of(2026, 10, 10), "Completado", 100f);
        tarea(primero, usuario, "Completada", LocalDate.of(2026, 11, 15));
        tarea(segundo, usuario, "Pendiente", LocalDate.of(2026, 10, 15));
        participacion(primero, usuario, LocalDateTime.of(2026, 11, 12, 9, 0));
        recurso(primero, "Equipo", 3);
        recurso(segundo, "Licencia", 7);

        var avance = servicio.consultar(filtro("avance", primero.getIdProyecto(), "2026-11-01", "2026-11-30"));
        assertEquals("Primero", avance.proyecto());
        assertEquals("1", resumen(avance).get("Proyectos"));
        assertEquals("60%", resumen(avance).get("Progreso promedio"));
        assertEquals("1", resumen(avance).get("En progreso"));

        var tareasResultado = servicio.consultar(filtro("tareas", primero.getIdProyecto(), "2026-11-01", "2026-11-30"));
        assertEquals("1", resumen(tareasResultado).get("Total de tareas"));
        assertEquals("1", resumen(tareasResultado).get("Completadas"));
        assertEquals("0", resumen(tareasResultado).get("Pendientes"));

        var equipo = servicio.consultar(filtro("equipo", primero.getIdProyecto(), "2026-11-12", "2026-11-12"));
        assertEquals("1", resumen(equipo).get("Participaciones"));
        assertEquals("1", resumen(equipo).get("Activas"));

        var recursosResultado = servicio.consultar(filtro("recursos", primero.getIdProyecto(), null, null));
        assertEquals("1", resumen(recursosResultado).get("Recursos registrados"));
        assertEquals("3", resumen(recursosResultado).get("Unidades registradas"));
        assertEquals("1", resumen(recursosResultado).get("Tipos de recurso"));

        assertEquals("0", resumen(servicio.consultar(filtro("avance", segundo.getIdProyecto(),
                "2026-11-01", "2026-11-30"))).get("Proyectos"));
        assertTrue(servicio.indicadores().proyectosActivos() >= 1);
        assertTrue(servicio.indicadores().tareasCompletadas() >= 1);
    }

    @Test
    void rechazaTiposProyectosYFechasInvalidas() {
        assertThrows(SolicitudInvalidaException.class,
                () -> servicio.consultar(filtro("falso", null, null, null)));
        assertThrows(SolicitudInvalidaException.class,
                () -> servicio.consultar(filtro("tareas", null, "2026-12-01", "2026-11-01")));
        assertThrows(SolicitudInvalidaException.class,
                () -> servicio.consultar(filtro("recursos", null, "2026-11-01", null)));
        assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.consultar(filtro("avance", Integer.MAX_VALUE, null, null)));
    }

    private ReporteConsultaRequest filtro(String tipo, Integer id, String desde, String hasta) {
        return new ReporteConsultaRequest(tipo, id, desde == null ? null : LocalDate.parse(desde),
                hasta == null ? null : LocalDate.parse(hasta));
    }

    private Map<String, String> resumen(com.gestionproyectos.dto.ReporteConsultaDto dto) {
        return dto.resumen().stream().collect(Collectors.toMap(
                com.gestionproyectos.dto.ReporteConsultaDto.IndicadorDto::nombre,
                com.gestionproyectos.dto.ReporteConsultaDto.IndicadorDto::valor));
    }

    private Proyecto proyecto(String nombre, Usuario usuario, LocalDate inicio, String estado, float progreso) {
        Proyecto p = new Proyecto();
        p.setNombre(nombre);
        p.setResponsable(usuario);
        p.setFechaInicio(inicio);
        p.setEstado(estado);
        p.setPorcentajeAvance(progreso);
        return proyectos.save(p);
    }

    private void tarea(Proyecto proyecto, Usuario usuario, String estado, LocalDate fin) {
        Tarea t = new Tarea();
        t.setNombre("Tarea reporte");
        t.setProyecto(proyecto);
        t.setResponsable(usuario);
        t.setEstado(estado);
        t.setFechaFin(fin);
        tareas.save(t);
    }

    private void participacion(Proyecto proyecto, Usuario usuario, LocalDateTime fecha) {
        ProyectoUsuarioId id = new ProyectoUsuarioId();
        id.setIdProyecto(proyecto.getIdProyecto());
        id.setIdUsuario(usuario.getIdUsuario());
        ProyectoUsuario pu = new ProyectoUsuario();
        pu.setId(id);
        pu.setProyecto(proyecto);
        pu.setUsuario(usuario);
        pu.setRol("Responsable");
        pu.setEstado("Activo");
        pu.setFechaAsignacion(fecha);
        participaciones.save(pu);
    }

    private void recurso(Proyecto proyecto, String tipo, int cantidad) {
        Recurso recurso = new Recurso();
        recurso.setNombreRecurso("Recurso reporte");
        recurso.setTipoRecurso(tipo);
        recurso.setCantidad(cantidad);
        recurso.setProyecto(proyecto);
        recursos.save(recurso);
    }
}
