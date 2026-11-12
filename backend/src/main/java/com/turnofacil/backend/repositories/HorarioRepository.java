package com.turnofacil.backend.repositories;

import com.turnofacil.backend.models.Horario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HorarioRepository extends JpaRepository<Horario, Long> {

    // Todas las franjas de un profesional
    List<Horario> findByProfesionalId(Long profesionalId);

    // Franjas de un día concreto (1 = lunes ... 7 = domingo). Usado por RN-01
    List<Horario> findByProfesionalIdAndDiaSemana(Long profesionalId, Integer diaSemana);
}
