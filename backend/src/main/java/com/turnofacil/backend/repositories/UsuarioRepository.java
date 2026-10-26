package com.turnofacil.backend.repositories;

import com.turnofacil.backend.models.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Método para verificar si ya existe un usuario con ese email
    boolean existsByEmail(String email);

    // Método para buscar un usuario por su email (utilizado en el login)
    Optional<Usuario> findByEmail(String email);
}