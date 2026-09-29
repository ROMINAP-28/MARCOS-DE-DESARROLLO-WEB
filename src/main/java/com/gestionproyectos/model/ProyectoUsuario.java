package com.gestionproyectos.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "proyecto_usuario")
@Data
public class ProyectoUsuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String rolEnProyecto;

    @ManyToOne
    @JoinColumn(name = "proyecto_id")
    private Proyecto proyecto;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;
}