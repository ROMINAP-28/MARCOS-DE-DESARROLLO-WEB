package com.gestionproyectos.service;

import com.gestionproyectos.dto.EspecialidadDto;

import java.util.List;

public interface IEspecialidadServicio {

    List<EspecialidadDto> listarEspecialidades();

    EspecialidadDto buscarEspecialidadPorId(Integer idEspecialidad);

    EspecialidadDto guardarEspecialidad(EspecialidadDto especialidad);

    EspecialidadDto actualizarEspecialidad(Integer idEspecialidad, EspecialidadDto especialidad);

    void eliminarEspecialidad(Integer idEspecialidad);
}
