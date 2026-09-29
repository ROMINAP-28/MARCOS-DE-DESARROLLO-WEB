package com.gestionproyectos.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "proyecto")
@Data
public class Proyecto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String descripcion;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private String estado;

    @OneToMany(mappedBy = "proyecto", cascade = CascadeType.ALL)
    private List proyectoUsuarios;

    @OneToMany(mappedBy = "proyecto", cascade = CascadeType.ALL)
    private List tareas;
}