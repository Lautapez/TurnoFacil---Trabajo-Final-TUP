package com.turnofacil.backend.repositories;

import com.turnofacil.backend.enums.EstadoTurno;
import com.turnofacil.backend.models.Turno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface TurnoRepository extends JpaRepository<Turno, Long> {

    boolean existsByProfesionalIdAndFechaAndHora(Long profesionalId, LocalDate fecha, LocalTime hora);

    boolean existsByProfesionalIdAndFechaAndHoraAndEstadoNot(Long profesionalId, LocalDate fecha, LocalTime hora, EstadoTurno estado);

    List<Turno> findByClienteId(Long clienteId);

    List<Turno> findByProfesionalId(Long profesionalId);
}