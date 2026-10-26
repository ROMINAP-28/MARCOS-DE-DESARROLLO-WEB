package com.gestionproyectos.service;

import com.gestionproyectos.dto.TareaDto;
import com.gestionproyectos.dto.TareaRequest;

import java.util.List;

public interface ITareaServicio {

    List<TareaDto> listarTareas();

    TareaDto buscarTareaPorId(Integer idTarea);

    TareaDto guardarTarea(TareaRequest request);

    TareaDto actualizarTarea(Integer idTarea, TareaRequest request);

    TareaDto cambiarEstado(Integer idTarea, String estado);

    void eliminarTarea(Integer idTarea);
}
