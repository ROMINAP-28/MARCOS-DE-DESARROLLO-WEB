package com.gestionproyectos.repository;

import com.gestionproyectos.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository
        extends JpaRepository<Usuario, Integer> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdUsuarioNot(String email, Integer idUsuario);

    Optional<Usuario> findFirstByNombreIgnoreCase(String nombre);

    List<Usuario> findAllByOrderByNombreAsc();
}
