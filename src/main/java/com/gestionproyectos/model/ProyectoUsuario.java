
package com.gestionproyectos.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "proyecto_usuario")
@Getter
@Setter
public class ProyectoUsuario {

    @EmbeddedId
    private ProyectoUsuarioId id;

    @ManyToOne
    @MapsId("idProyecto")
    @JoinColumn(name = "id_proyecto", nullable = false)
    private Proyecto proyecto;

    @ManyToOne
    @MapsId("idUsuario")
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "rol", length = 50)
    private String rol;

    @Column(name = "estado", length = 30)
    private String estado;

    @Column(name = "fecha_asignacion")
    private LocalDateTime fechaAsignacion;
}