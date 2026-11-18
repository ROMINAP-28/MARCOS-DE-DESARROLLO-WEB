package com.gestionproyectos.repository;

import com.gestionproyectos.model.ProyectoUsuario;
import com.gestionproyectos.model.ProyectoUsuarioId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProyectoUsuarioRepository
        extends JpaRepository<ProyectoUsuario, ProyectoUsuarioId> {

    @Query("select pu from ProyectoUsuario pu where pu.id.idProyecto = :idProyecto "
            + "order by pu.fechaAsignacion, pu.id.idUsuario")
    List<ProyectoUsuario> buscarPorProyecto(@Param("idProyecto") Integer idProyecto);

    @Query("select pu from ProyectoUsuario pu join fetch pu.proyecto "
            + "join fetch pu.usuario u left join fetch u.especialidad "
            + "order by pu.id.idProyecto, pu.id.idUsuario")
    List<ProyectoUsuario> listarParticipaciones();

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from ProyectoUsuario pu where pu.id.idProyecto = :idProyecto")
    void eliminarPorProyecto(@Param("idProyecto") Integer idProyecto);
}
