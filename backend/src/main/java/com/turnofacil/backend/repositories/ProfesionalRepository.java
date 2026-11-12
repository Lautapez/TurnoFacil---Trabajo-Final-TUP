package com.turnofacil.backend.repositories;

import com.turnofacil.backend.models.Profesional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfesionalRepository extends JpaRepository<Profesional, Long> {

    // Profesionales activos filtrados por especialidad (requerimiento del MVP)
    List<Profesional> findByEspecialidadIdAndActivoTrue(Long especialidadId);

    // Permite buscar profesionales activos
    List<Profesional> findByActivoTrue();

    // Perfil profesional asociado a un usuario (el profesional se identifica por su token)
    Optional<Profesional> findByUsuarioId(Long usuarioId);

    boolean existsByUsuarioId(Long usuarioId);

    long countByActivoTrue();
}
