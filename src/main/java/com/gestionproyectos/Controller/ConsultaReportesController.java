package com.gestionproyectos.Controller;

import com.gestionproyectos.config.AlcanceLectura;
import com.gestionproyectos.dto.IndicadoresReportesDto;
import com.gestionproyectos.dto.ReporteConsultaDto;
import com.gestionproyectos.dto.ReporteConsultaRequest;
import com.gestionproyectos.service.ConsultaReportesServicio;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/miembro3/reportes")
public class ConsultaReportesController {
    private final ConsultaReportesServicio servicio;
    private final AlcanceLectura alcance;

    public ConsultaReportesController(ConsultaReportesServicio servicio, AlcanceLectura alcance) {
        this.servicio = servicio;
        this.alcance = alcance;
    }

    @GetMapping("/indicadores")
    public IndicadoresReportesDto indicadores() {
        return servicio.indicadores(alcance.idsProyectosVisibles());
    }

    @PostMapping("/consulta")
    public ReporteConsultaDto consultar(@Valid @RequestBody ReporteConsultaRequest request) {
        return servicio.consultar(request, alcance.idsProyectosVisibles());
    }
}
