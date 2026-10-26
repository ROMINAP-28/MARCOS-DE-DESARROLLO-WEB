package com.gestionproyectos.repository;

import com.gestionproyectos.model.Especialidad;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EspecialidadRepository extends JpaRepository<Especialidad, Integer> {

    boolean existsByNombreEspecialidadIgnoreCase(String nombreEspecialidad);
}
