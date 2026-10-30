
package com.gestionproyectos.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "notificacion")
@Getter
@Setter
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notificacion")
    private Integer idNotificacion;

    @Size(max = 255)
    @Column(name = "tipo")
    private String tipo;

    @NotBlank
    @Size(max = 255)
    @Column(name = "mensaje", nullable = false)
    private String mensaje;

    // Detalle opcional, separado del mensaje breve usado en las notificaciones actuales.
    @Column(name = "contenido", length = 255)
    private String contenido;

    @Column(name = "leido")
    private Boolean leido = false;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @PrePersist
    void iniciarFechaCreacion() {
        if (fechaCreacion == null) fechaCreacion = LocalDateTime.now();
    }

    @NotNull
    @ManyToOne
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "id_tarea", nullable = false)
    private Tarea tarea;
}
