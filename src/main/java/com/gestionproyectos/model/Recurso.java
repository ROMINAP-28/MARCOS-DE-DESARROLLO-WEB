package com.gestionproyectos.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "recurso")
@Getter
@Setter
public class Recurso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_recurso")
    private Integer idRecurso;

    @NotBlank
    @Size(max = 150)
    @Column(name = "nombre_recurso", nullable = false, length = 150)
    private String nombreRecurso;

    @NotBlank
    @Size(max = 100)
    @Column(name = "tipo_recurso", nullable = false, length = 100)
    private String tipoRecurso;

    @NotNull
    @Min(0)
    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_proyecto", nullable = false)
    private Proyecto proyecto;
}
