package com.gestionproyectos.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "reporte")
@Getter
@Setter
public class Reporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reporte")
    private Integer idReporte;

    @NotBlank
    @Size(max = 100)
    @Column(name = "tipo_reporte", nullable = false, length = 100)
    private String tipoReporte;

    @Column(name = "fecha_generacion", nullable = false, updatable = false)
    private LocalDateTime fechaGeneracion;

    @NotBlank
    @Size(max = 30)
    @Column(name = "estado", nullable = false, length = 30)
    private String estado;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_proyecto", nullable = false)
    private Proyecto proyecto;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_generador", nullable = false)
    private Usuario usuarioGenerador;

    @PrePersist
    void iniciarFechaGeneracion() {
        if (fechaGeneracion == null) fechaGeneracion = LocalDateTime.now();
    }
}
