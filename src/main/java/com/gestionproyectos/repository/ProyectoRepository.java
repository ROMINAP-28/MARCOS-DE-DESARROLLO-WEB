package com.gestionproyectos.repository;

import com.gestionproyectos.model.Proyecto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProyectoRepository extends JpaRepository<Proyecto, Integer> {

    List<Proyecto> findAllByOrderByIdProyectoDesc();

    Optional<Proyecto> findFirstByNombreIgnoreCase(String nombre);

    @Query("""
            select p from Proyecto p
            where lower(p.nombre) like lower(concat('%', :q, '%'))
               or lower(coalesce(p.descripcion, '')) like lower(concat('%', :q, '%'))
            order by p.idProyecto desc
            """)
    List<Proyecto> buscar(@Param("q") String q);
}
