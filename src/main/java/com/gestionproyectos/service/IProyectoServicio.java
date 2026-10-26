package com.gestionproyectos.service;

import com.gestionproyectos.dto.ProyectoDto;
import com.gestionproyectos.dto.ProyectoRequest;

import java.util.List;

public interface IProyectoServicio {

    List<ProyectoDto> listarProyectos(String busqueda);

    ProyectoDto buscarProyectoPorId(Integer idProyecto);

    ProyectoDto guardarProyecto(ProyectoRequest request);

    ProyectoDto actualizarProyecto(Integer idProyecto, ProyectoRequest request);

    void eliminarProyecto(Integer idProyecto);
}
