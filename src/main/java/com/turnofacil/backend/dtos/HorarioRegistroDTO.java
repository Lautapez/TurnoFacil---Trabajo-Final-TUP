package com.turnofacil.backend.dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public class HorarioRegistroDTO {

    @NotNull(message = "El ID del profesional es obligatorio")
    private Long profesionalId;

    @NotNull(message = "El día de la semana es obligatorio")
    @Min(value = 1, message = "El día de la semana debe ser entre 1 (Lunes) y 7 (Domingo)")
    @Max(value = 7, message = "El día de la semana debe ser entre 1 (Lunes) y 7 (Domingo)")
    private Integer diaSemana;

    @NotNull(message = "La hora de inicio es obligatoria")
    private LocalTime horaInicio;

    @NotNull(message = "La hora de fin es obligatoria")
    private LocalTime horaFin;

    // Getters y Setters
    public Long getProfesionalId() { return profesionalId; }
    public void setProfesionalId(Long profesionalId) { this.profesionalId = profesionalId; }

    public Integer getDiaSemana() { return diaSemana; }
    public void setDiaSemana(Integer diaSemana) { this.diaSemana = diaSemana; }

    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }

    public LocalTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalTime horaFin) { this.horaFin = horaFin; }
}