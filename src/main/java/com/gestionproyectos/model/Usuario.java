
package com.gestionproyectos.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "usuario")
@Getter
@Setter
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer idUsuario;

    @NotBlank
    @Size(max = 100)
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @NotBlank
    @Email
    @Size(max = 150)
    @Column(name = "correo", nullable = false, unique = true, length = 150)
    private String email;

    @NotBlank
    @Size(max = 255)
    @Column(name = "contrasena_hash", nullable = false, length = 255)
    private String password;

    @Size(max = 20)
    @Column(name = "telefono", length = 20)
    private String telefono;

    @NotBlank
    @Size(max = 50)
    @Column(name = "rol", nullable = false, length = 50)
    private String rol;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_especialidad")
    private Especialidad especialidad;

    @Column(name = "notif_tareas")
    private Boolean notifTareas = true;

    @Column(name = "recordatorios_fechas")
    private Boolean recordatoriosFechas = true;

    @Column(name = "menciones_comentarios")
    private Boolean mencionesComentarios = true;

    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro;

    @PrePersist
    void iniciarFechaRegistro() {
        if (fechaRegistro == null) fechaRegistro = LocalDateTime.now();
    }

    @OneToMany(mappedBy = "usuario")
    private List<ProyectoUsuario> proyectoUsuarios;

    @OneToMany(mappedBy = "responsable")
    private List<Proyecto> proyectosResponsable;

    @OneToMany(mappedBy = "responsable")
    private List<Tarea> tareas;

    @OneToMany(mappedBy = "usuario")
    private List<Notificacion> notificaciones;

    @OneToMany(mappedBy = "usuario")
    private List<Comentario> comentarios;

    @OneToMany(mappedBy = "usuarioGenerador")
    private List<Reporte> reportesGenerados;
}
