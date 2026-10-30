
package com.gestionproyectos.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "proyecto")
@Getter
@Setter
public class Proyecto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_proyecto")
    private Integer idProyecto;

    @NotBlank
    @Size(max = 150)
    @Column(name = "nombre_proyecto", nullable = false, length = 150)
    private String nombre;

    @Size(max = 255)
    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Size(max = 255)
    @Column(name = "prioridad")
    private String prioridad;

    @Size(max = 255)
    @Column(name = "estado")
    private String estado;

    @DecimalMin("0.0")
    @DecimalMax("100.0")
    @Column(name = "porcentaje_avance")
    private Float porcentajeAvance;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_responsable", nullable = false)
    private Usuario responsable;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    void iniciarFechaCreacion() {
        if (fechaCreacion == null) fechaCreacion = LocalDateTime.now();
    }

    @AssertTrue(message = "La fecha de fin no puede ser anterior a la fecha de inicio")
    public boolean isPeriodoValido() {
        return fechaInicio == null || fechaFin == null || !fechaFin.isBefore(fechaInicio);
    }

    @OneToMany(mappedBy = "proyecto", cascade = CascadeType.ALL)
    private List<ProyectoUsuario> proyectoUsuarios;

    @OneToMany(mappedBy = "proyecto", cascade = CascadeType.ALL)
    private List<Tarea> tareas;

    @OneToMany(mappedBy = "proyecto", cascade = CascadeType.ALL)
    private List<Recurso> recursos;

    @OneToMany(mappedBy = "proyecto", cascade = CascadeType.ALL)
    private List<Reporte> reportes;
}
