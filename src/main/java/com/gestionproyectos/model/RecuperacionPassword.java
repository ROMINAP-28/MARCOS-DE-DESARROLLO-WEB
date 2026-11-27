package com.gestionproyectos.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "recuperacion_password", indexes = @Index(name = "idx_recuperacion_usuario", columnList = "id_usuario"))
public class RecuperacionPassword {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "correo_solicitud", nullable = false, length = 150)
    private String correoSolicitud;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "expira", nullable = false)
    private LocalDateTime expira;

    @Column(name = "consumido")
    private LocalDateTime consumido;

    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public String getCorreoSolicitud() { return correoSolicitud; }
    public void setCorreoSolicitud(String correoSolicitud) { this.correoSolicitud = correoSolicitud; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public LocalDateTime getExpira() { return expira; }
    public void setExpira(LocalDateTime expira) { this.expira = expira; }
    public LocalDateTime getConsumido() { return consumido; }
    public void setConsumido(LocalDateTime consumido) { this.consumido = consumido; }
}
