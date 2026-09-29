package com.gestionproyectos.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "notificacion")
@Data
public class Notificacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String mensaje;
    private LocalDateTime fechaEnvio;
    private Boolean leido = false;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;
}