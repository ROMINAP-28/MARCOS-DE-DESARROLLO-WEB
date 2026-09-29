
package com.gestionproyectos.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;
import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class ProyectoUsuarioId implements Serializable {

    @Column(name = "id_proyecto")
    private Integer idProyecto;

    @Column(name = "id_usuario")
    private Integer idUsuario;
}