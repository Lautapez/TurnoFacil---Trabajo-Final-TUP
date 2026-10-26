package com.turnofacil.backend.repositories;

import com.turnofacil.backend.models.Turno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface TurnoRepository extends JpaRepository<Turno, Long> {

    // Consulta los turnos asociados a un cliente específico utilizando el atributo 'cliente'
    List<Turno> findByClienteId(Long clienteId);

    // Permite consultar los turnos asignados a un profesional específico
    List<Turno> findByProfesionalId(Long profesionalId);

    // Método para verificar si ya existe un turno asignado al profesional en la misma fecha y hora (evita solapamientos)
    boolean existsByProfesionalIdAndFechaAndHora(Long profesionalId, LocalDate fecha, LocalTime hora);
}