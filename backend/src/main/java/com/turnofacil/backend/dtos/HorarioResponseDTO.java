package com.turnofacil.backend.dtos;

import com.turnofacil.backend.models.Horario;

import java.time.LocalTime;

public record HorarioResponseDTO(Long id, Long profesionalId, Integer diaSemana, LocalTime horaInicio, LocalTime horaFin) {

    public static HorarioResponseDTO from(Horario h) {
        return new HorarioResponseDTO(h.getId(), h.getProfesional().getId(), h.getDiaSemana(), h.getHoraInicio(), h.getHoraFin());
    }
}
