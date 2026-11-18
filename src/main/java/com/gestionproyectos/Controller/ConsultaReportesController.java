package com.gestionproyectos.Controller;

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

    public ConsultaReportesController(ConsultaReportesServicio servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/indicadores")
    public IndicadoresReportesDto indicadores() {
        return servicio.indicadores();
    }

    @PostMapping("/consulta")
    public ReporteConsultaDto consultar(@Valid @RequestBody ReporteConsultaRequest request) {
        return servicio.consultar(request);
    }
}
