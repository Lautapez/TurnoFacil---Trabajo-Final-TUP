package com.turnofacil.backend.repositories;

import com.turnofacil.backend.enums.EstadoTurno;
import com.turnofacil.backend.models.Turno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TurnoRepository extends JpaRepository<Turno, Long> {

    // RN-02 / RN-03: turnos de un profesional en una fecha, sin contar los del estado indicado (CANCELADO)
    List<Turno> findByProfesionalIdAndFechaAndEstadoNot(Long profesionalId, LocalDate fecha, EstadoTurno estado);

    List<Turno> findByClienteId(Long clienteId);

    List<Turno> findByProfesionalId(Long profesionalId);

    // Módulo administrativo
    List<Turno> findByFechaOrderByHoraAsc(LocalDate fecha);

    List<Turno> findAllByOrderByFechaDescHoraDesc();

    long countByEstado(EstadoTurno estado);

    long countByFecha(LocalDate fecha);

    long countByFechaAndEstado(LocalDate fecha, EstadoTurno estado);
}
