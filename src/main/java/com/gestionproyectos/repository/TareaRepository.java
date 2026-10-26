package com.gestionproyectos.repository;

import com.gestionproyectos.model.Tarea;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TareaRepository extends JpaRepository<Tarea, Integer> {

    List<Tarea> findAllByOrderByIdTareaDesc();
}
