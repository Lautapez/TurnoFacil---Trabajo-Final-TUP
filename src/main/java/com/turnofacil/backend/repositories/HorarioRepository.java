package com.turnofacil.backend.repositories;

import com.turnofacil.backend.models.Horario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HorarioRepository extends JpaRepository<Horario, Long> {

    // Permite consultar los horarios disponibles asignados a un profesional específico
    List<Horario> findByProfesionalId(Long profesionalId);
}