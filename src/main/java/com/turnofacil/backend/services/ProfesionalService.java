package com.turnofacil.backend.services;

import com.turnofacil.backend.models.Horario;
import com.turnofacil.backend.models.Profesional;
import com.turnofacil.backend.repositories.HorarioRepository;
import com.turnofacil.backend.repositories.ProfesionalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProfesionalService {

    @Autowired
    private ProfesionalRepository profesionalRepository;

    @Autowired
    private HorarioRepository horarioRepository;

    /**
     * Obtiene el listado general de profesionales activos (GET /api/professionals).
     */
    public List<Profesional> listarProfesionalesActivos() {
        return profesionalRepository.findByActivoTrue();
    }

    /**
     * Filtra profesionales por ID de especialidad.
     */
    public List<Profesional> listarPorEspecialidad(Long especialidadId) {
        return profesionalRepository.findByEspecialidadId(especialidadId);
    }

    /**
     * Permite guardar/dar de alta un nuevo profesional (POST /api/professionals).
     */
    public Profesional guardarProfesional(Profesional profesional) {
        return profesionalRepository.save(profesional);
    }

    /**
     * Consulta las franjas horarias configuradas para un profesional (GET /api/professionals/{id}/availability).
     */
    public List<Horario> obtenerHorariosPorProfesional(Long profesionalId) {
        return horarioRepository.findByProfesionalId(profesionalId);
    }

    /**
     * Permite configurar y guardar una nueva franja horaria para el profesional (POST /api/professionals/availability).
     */
    public Horario guardarHorario(Horario horario) {
        return horarioRepository.save(horario);
    }
}