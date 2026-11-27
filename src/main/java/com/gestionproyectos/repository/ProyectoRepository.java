package com.gestionproyectos.repository;

import com.gestionproyectos.model.Proyecto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProyectoRepository extends JpaRepository<Proyecto, Integer> {

    @Query("""
            select p.idProyecto from Proyecto p
            where p.responsable.idUsuario = :idUsuario
               or exists (select 1 from ProyectoUsuario pu
                          where pu.proyecto = p and pu.usuario.idUsuario = :idUsuario
                            and pu.estado = 'Activo')
            """)
    List<Integer> idsVisibles(@Param("idUsuario") Integer idUsuario);

    List<Proyecto> findAllByOrderByIdProyectoDesc();

    @Query("""
            select p from Proyecto p
            where lower(p.nombre) like lower(concat('%', :q, '%'))
               or lower(coalesce(p.descripcion, '')) like lower(concat('%', :q, '%'))
            order by p.idProyecto desc
            """)
    List<Proyecto> buscar(@Param("q") String q);
}
