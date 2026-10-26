
package com.turnofacil.backend.repositories;

import com.turnofacil.backend.models.Profesional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProfesionalRepository extends JpaRepository<Profesional, Long> {

    // Permite listar profesionales filtrados por especialidad, tal como se especifica en los requerimientos del MVP
    List<Profesional> findByEspecialidadId(Long especialidadId);

    // Permite buscar profesionales activos
    List<Profesional> findByActivoTrue();
}