package com.gestionproyectos.repository;

import com.gestionproyectos.model.RecuperacionPassword;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface RecuperacionPasswordRepository extends JpaRepository<RecuperacionPassword, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RecuperacionPassword> findByTokenHash(String hash);

    void deleteByUsuario_IdUsuario(Integer idUsuario);
}
