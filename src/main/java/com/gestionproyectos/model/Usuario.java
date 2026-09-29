package com.gestionproyectos.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Entity
@Table(name = "usuario")
@Data
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String email;
    private String password;

    @ManyToOne
    @JoinColumn(name = "especialidad_id")
    private Especialidad especialidad;

    @OneToMany(mappedBy = "usuario")
    private List proyectoUsuarios;

    @OneToMany(mappedBy = "usuario")
    private List tareas;

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL)
    private List notificaciones;
}