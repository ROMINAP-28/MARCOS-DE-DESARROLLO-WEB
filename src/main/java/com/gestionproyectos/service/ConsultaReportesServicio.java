package com.gestionproyectos.service;

import com.gestionproyectos.dto.IndicadoresReportesDto;
import com.gestionproyectos.dto.ReporteConsultaDto;
import com.gestionproyectos.dto.ReporteConsultaDto.IndicadorDto;
import com.gestionproyectos.dto.ReporteConsultaRequest;
import com.gestionproyectos.exception.RecursoNoEncontradoException;
import com.gestionproyectos.exception.SolicitudInvalidaException;
import com.gestionproyectos.model.Proyecto;
import com.gestionproyectos.model.Recurso;
import com.gestionproyectos.model.Tarea;
import com.gestionproyectos.repository.ProyectoRepository;
import com.gestionproyectos.repository.ProyectoUsuarioRepository;
import com.gestionproyectos.repository.RecursoRepository;
import com.gestionproyectos.repository.TareaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class ConsultaReportesServicio {
    private final ProyectoRepository proyectos;
    private final TareaRepository tareas;
    private final ProyectoUsuarioRepository participaciones;
    private final RecursoRepository recursos;

    public ConsultaReportesServicio(ProyectoRepository proyectos, TareaRepository tareas,
                                   ProyectoUsuarioRepository participaciones, RecursoRepository recursos) {
        this.proyectos = proyectos;
        this.tareas = tareas;
        this.participaciones = participaciones;
        this.recursos = recursos;
    }

    public IndicadoresReportesDto indicadores() {
        long activos = proyectos.findAll().stream()
                .filter(p -> "En progreso".equals(p.getEstado())).count();
        long completadas = tareas.findAll().stream()
                .filter(t -> "Completada".equals(t.getEstado())).count();
        return new IndicadoresReportesDto(activos, completadas, proyectos.count(), tareas.count());
    }

    public ReporteConsultaDto consultar(ReporteConsultaRequest request) {
        if (request.fechaDesde() != null && request.fechaHasta() != null
                && request.fechaDesde().isAfter(request.fechaHasta())) {
            throw new SolicitudInvalidaException("La fecha inicial no puede ser posterior a la final.");
        }
        if (request.tipo() == null
                || !List.of("avance", "tareas", "equipo", "recursos").contains(request.tipo())) {
            throw new SolicitudInvalidaException("Tipo de reporte no válido.");
        }
        if ("recursos".equals(request.tipo())
                && (request.fechaDesde() != null || request.fechaHasta() != null)) {
            throw new SolicitudInvalidaException("Los recursos no tienen fecha; elimina el filtro de fechas.");
        }
        String proyecto = request.idProyecto() == null ? "Todos los proyectos" : proyectos
                .findById(request.idProyecto())
                .map(Proyecto::getNombre)
                .orElseThrow(() -> new RecursoNoEncontradoException("Proyecto no encontrado"));

        List<IndicadorDto> resumen = switch (request.tipo()) {
            case "avance" -> avance(request);
            case "tareas" -> tareas(request);
            case "equipo" -> equipo(request);
            case "recursos" -> recursos(request);
            default -> throw new IllegalStateException("Tipo no validado");
        };
        return new ReporteConsultaDto(request.tipo(), proyecto, LocalDateTime.now(),
                request.fechaDesde(), request.fechaHasta(), resumen);
    }

    private List<IndicadorDto> avance(ReporteConsultaRequest request) {
        var datos = proyectos.findAll().stream()
                .filter(p -> coincide(request.idProyecto(), p.getIdProyecto()))
                .filter(p -> enPeriodo(p.getFechaInicio(), request)).toList();
        double progreso = datos.stream().mapToDouble(p -> p.getPorcentajeAvance() == null
                ? 0 : p.getPorcentajeAvance()).average().orElse(0);
        return List.of(indicador("Proyectos", datos.size()),
                indicador("Progreso promedio", Math.round(progreso) + "%"),
                indicador("En progreso", datos.stream().filter(p -> "En progreso".equals(p.getEstado())).count()),
                indicador("Completados", datos.stream().filter(p -> "Completado".equals(p.getEstado())).count()));
    }

    private List<IndicadorDto> tareas(ReporteConsultaRequest request) {
        List<Tarea> datos = tareas.findAll().stream()
                .filter(t -> coincide(request.idProyecto(), t.getProyecto().getIdProyecto()))
                .filter(t -> enPeriodo(t.getFechaFin(), request)).toList();
        return List.of(indicador("Total de tareas", datos.size()),
                indicador("Pendientes", datos.stream().filter(t -> "Pendiente".equals(t.getEstado())).count()),
                indicador("En progreso", datos.stream().filter(t -> "En progreso".equals(t.getEstado())).count()),
                indicador("Completadas", datos.stream().filter(t -> "Completada".equals(t.getEstado())).count()));
    }

    private List<IndicadorDto> equipo(ReporteConsultaRequest request) {
        var datos = participaciones.listarParticipaciones().stream()
                .filter(p -> coincide(request.idProyecto(), p.getId().getIdProyecto()))
                .filter(p -> enPeriodo(p.getFechaAsignacion() == null
                        ? null : p.getFechaAsignacion().toLocalDate(), request)).toList();
        return List.of(indicador("Participaciones", datos.size()),
                indicador("Activas", datos.stream().filter(p -> "Activo".equals(p.getEstado())).count()),
                indicador("Inactivas", datos.stream().filter(p -> "Inactivo".equals(p.getEstado())).count()),
                indicador("Pendientes", datos.stream().filter(p -> "Pendiente".equals(p.getEstado())).count()));
    }

    private List<IndicadorDto> recursos(ReporteConsultaRequest request) {
        List<Recurso> datos = recursos.findAll().stream()
                .filter(r -> coincide(request.idProyecto(), r.getProyecto().getIdProyecto())).toList();
        return List.of(indicador("Recursos registrados", datos.size()),
                indicador("Unidades registradas", datos.stream().mapToLong(Recurso::getCantidad).sum()),
                indicador("Tipos de recurso", datos.stream().map(Recurso::getTipoRecurso).distinct().count()));
    }

    private boolean coincide(Integer solicitado, Integer real) {
        return solicitado == null || Objects.equals(solicitado, real);
    }

    // Los registros sin fecha no se incluyen cuando hay un filtro temporal.
    private boolean enPeriodo(LocalDate fecha, ReporteConsultaRequest request) {
        if (request.fechaDesde() == null && request.fechaHasta() == null) return true;
        return fecha != null && (request.fechaDesde() == null || !fecha.isBefore(request.fechaDesde()))
                && (request.fechaHasta() == null || !fecha.isAfter(request.fechaHasta()));
    }

    private IndicadorDto indicador(String nombre, long valor) {
        return indicador(nombre, Long.toString(valor));
    }

    private IndicadorDto indicador(String nombre, String valor) {
        return new IndicadorDto(nombre, valor);
    }
}
