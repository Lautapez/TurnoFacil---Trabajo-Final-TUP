package com.turnofacil.backend.services;

import com.turnofacil.backend.dtos.HorarioRegistroDTO;
import com.turnofacil.backend.dtos.ProfesionalRegistroDTO;
import com.turnofacil.backend.enums.Rol;
import com.turnofacil.backend.models.Especialidad;
import com.turnofacil.backend.models.Horario;
import com.turnofacil.backend.models.Profesional;
import com.turnofacil.backend.models.Usuario;
import com.turnofacil.backend.repositories.EspecialidadRepository;
import com.turnofacil.backend.repositories.HorarioRepository;
import com.turnofacil.backend.repositories.ProfesionalRepository;
import com.turnofacil.backend.repositories.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProfesionalService {

    @Autowired
    private ProfesionalRepository profesionalRepository;

    @Autowired
    private HorarioRepository horarioRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EspecialidadRepository especialidadRepository;

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
     * Permite guardar/dar de alta un nuevo profesional mediante DTO (POST /api/professionals).
     */
    public Profesional guardarProfesionalDTO(ProfesionalRegistroDTO dto) {
        Usuario usuario = usuarioRepository.findById(dto.getUsuarioId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + dto.getUsuarioId()));

        // Blindaje RN-05: Un usuario con rol CLIENTE no puede registrarse como profesional
        if (usuario.getRol() == Rol.CLIENTE) {
            throw new RuntimeException("Acceso denegado: Un usuario con rol CLIENTE no puede realizar esta acción.");
        }

        Especialidad especialidad = especialidadRepository.findById(dto.getEspecialidadId())
                .orElseThrow(() -> new RuntimeException("Especialidad no encontrada con ID: " + dto.getEspecialidadId()));

        Profesional profesional = new Profesional();
        profesional.setUsuario(usuario);
        profesional.setEspecialidad(especialidad);
        profesional.setTelefono(dto.getTelefono());
        profesional.setActivo(true);

        return profesionalRepository.save(profesional);
    }

    /**
     * Método clásico compatible por si se usa internamente en otra capa.
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
     * Permite configurar y guardar una nueva franja horaria mediante DTO (POST /api/professionals/availability).
     */
    public Horario guardarHorarioDTO(HorarioRegistroDTO dto) {
        Profesional profesional = profesionalRepository.findById(dto.getProfesionalId())
                .orElseThrow(() -> new RuntimeException("Profesional no encontrado con ID: " + dto.getProfesionalId()));

        // Blindaje RN-05: Verificar que el usuario asociado no sea un CLIENTE intentando configurar agendas
        if (profesional.getUsuario() != null && profesional.getUsuario().getRol() == Rol.CLIENTE) {
            throw new RuntimeException("Acceso denegado: Un usuario con rol CLIENTE no puede configurar ni modificar agendas profesionales.");
        }

        // Validación de lógica: la hora de fin debe ser posterior a la hora de inicio
        if (!dto.getHoraFin().isAfter(dto.getHoraInicio())) {
            throw new RuntimeException("La hora de fin debe ser posterior a la hora de inicio.");
        }

        Horario horario = new Horario();
        horario.setProfesional(profesional);
        horario.setDiaSemana(dto.getDiaSemana());
        horario.setHoraInicio(dto.getHoraInicio());
        horario.setHoraFin(dto.getHoraFin());

        return horarioRepository.save(horario);
    }

    /**
     * Método clásico compatible.
     */
    public Horario guardarHorario(Horario horario) {
        return horarioRepository.save(horario);
    }
}