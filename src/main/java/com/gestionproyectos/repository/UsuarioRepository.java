
package com.gestionproyectos.repository;

import com.gestionproyectos.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository
        extends JpaRepository<Usuario, Integer> {

    boolean existsByEmail(String email);
}