package com.gestionproyectos.service;

import com.gestionproyectos.dto.EspecialidadDto;
import com.gestionproyectos.exception.RecursoNoEncontradoException;
import com.gestionproyectos.exception.SolicitudInvalidaException;
import com.gestionproyectos.model.Especialidad;
import com.gestionproyectos.repository.EspecialidadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class EspecialidadServicio implements IEspecialidadServicio {

    private final EspecialidadRepository especialidadRepositorio;

    public EspecialidadServicio(EspecialidadRepository especialidadRepositorio) {
        this.especialidadRepositorio = especialidadRepositorio;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EspecialidadDto> listarEspecialidades() {
        return especialidadRepositorio.findAll().stream().map(this::aDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EspecialidadDto buscarEspecialidadPorId(Integer idEspecialidad) {
        return aDto(obtener(idEspecialidad));
    }

    @Override
    public EspecialidadDto guardarEspecialidad(EspecialidadDto dto) {
        if (especialidadRepositorio.existsByNombreEspecialidadIgnoreCase(dto.nombre().trim())) {
            throw new SolicitudInvalidaException("Ya existe una especialidad con ese nombre.");
        }
        Especialidad e = new Especialidad();
        e.setNombreEspecialidad(dto.nombre().trim());
        e.setDescripcion(dto.descripcion());
        return aDto(especialidadRepositorio.save(e));
    }

    @Override
    public EspecialidadDto actualizarEspecialidad(Integer idEspecialidad, EspecialidadDto dto) {
        Especialidad e = obtener(idEspecialidad);
        e.setNombreEspecialidad(dto.nombre().trim());
        e.setDescripcion(dto.descripcion());
        return aDto(especialidadRepositorio.save(e));
    }

    @Override
    public void eliminarEspecialidad(Integer idEspecialidad) {
        especialidadRepositorio.delete(obtener(idEspecialidad));
    }

    private Especialidad obtener(Integer id) {
        return especialidadRepositorio.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Especialidad no encontrada"));
    }

    private EspecialidadDto aDto(Especialidad e) {
        return new EspecialidadDto(e.getIdEspecialidad(), e.getNombreEspecialidad(), e.getDescripcion());
    }
}
