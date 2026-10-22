
package com.gestionproyectos.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Entity
@Table(name = "especialidad")
@Getter
@Setter
public class Especialidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_especialidad")
    private Integer idEspecialidad;

    @NotBlank
    @Size(max = 100)
    @Column(name = "nombre_especialidad",
            nullable = false,
            unique = true,
            length = 100)
    private String nombreEspecialidad;

    @Size(max = 255)
    @Column(name = "descripcion", length = 255)
    private String descripcion;

    @OneToMany(mappedBy = "especialidad")
    private List<Usuario> usuarios;
}
