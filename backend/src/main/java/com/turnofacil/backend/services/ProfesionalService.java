package com.turnofacil.backend.services;

import com.turnofacil.backend.dtos.HorarioRegistroDTO;
import com.turnofacil.backend.dtos.HorarioResponseDTO;
import com.turnofacil.backend.dtos.ProfesionalRegistroDTO;
import com.turnofacil.backend.dtos.ProfesionalResponseDTO;
import com.turnofacil.backend.enums.Rol;
import com.turnofacil.backend.exceptions.ConflictoTurnoException;
import com.turnofacil.backend.exceptions.RecursoNoEncontradoException;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProfesionalService {

    @Autowired
    private ProfesionalRepository profesionalRepository;

    @Autowired
    private HorarioRepository horarioRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EspecialidadRepository especialidadRepository;

    /** Profesionales activos (GET /api/professionals). */
    @Transactional(readOnly = true)
    public List<ProfesionalResponseDTO> listarProfesionalesActivos() {
        return profesionalRepository.findByActivoTrue().stream()
                .map(ProfesionalResponseDTO::from).collect(Collectors.toList());
    }

    /** Profesionales activos de una especialidad. */
    @Transactional(readOnly = true)
    public List<ProfesionalResponseDTO> listarPorEspecialidad(Long especialidadId) {
        return profesionalRepository.findByEspecialidadIdAndActivoTrue(especialidadId).stream()
                .map(ProfesionalResponseDTO::from).collect(Collectors.toList());
    }

    /** Todos los profesionales, activos e inactivos (módulo administrativo). */
    @Transactional(readOnly = true)
    public List<ProfesionalResponseDTO> listarTodos() {
        return profesionalRepository.findAll().stream()
                .map(ProfesionalResponseDTO::from).collect(Collectors.toList());
    }

    /** Perfil profesional del usuario autenticado (incluso si está dado de baja). */
    @Transactional(readOnly = true)
    public ProfesionalResponseDTO obtenerPerfilDe(Usuario actual) {
        return profesionalRepository.findByUsuarioId(actual.getId())
                .map(ProfesionalResponseDTO::from)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Todavía no tenés un perfil profesional. Un administrador debe crearlo."));
    }

    /**
     * Alta del perfil profesional (solo ADMINISTRADOR). El usuario debe tener rol PROFESIONAL,
     * asignado previamente por un administrador (RN-05: un CLIENTE no puede ser profesional).
     */
    public ProfesionalResponseDTO guardarProfesionalDTO(ProfesionalRegistroDTO dto) {
        Usuario usuario = usuarioRepository.findById(dto.getUsuarioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con ID: " + dto.getUsuarioId()));

        if (usuario.getRol() != Rol.PROFESIONAL) {
            throw new ConflictoTurnoException(
                    "El usuario debe tener rol PROFESIONAL para crear su perfil profesional (RN-05). "
                            + "Un administrador debe asignarle ese rol primero.");
        }

        if (profesionalRepository.existsByUsuarioId(usuario.getId())) {
            throw new ConflictoTurnoException("El usuario ya tiene un perfil profesional.");
        }

        Especialidad especialidad = especialidadRepository.findById(dto.getEspecialidadId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Especialidad no encontrada con ID: " + dto.getEspecialidadId()));

        Profesional profesional = new Profesional();
        profesional.setUsuario(usuario);
        profesional.setEspecialidad(especialidad);
        profesional.setTelefono(dto.getTelefono());
        profesional.setActivo(true);

        return ProfesionalResponseDTO.from(profesionalRepository.save(profesional));
    }

    /** Baja lógica de un profesional (solo ADMINISTRADOR). */
    public void darDeBajaProfesional(Long id) {
        Profesional profesional = obtener(id);

        if (!profesional.getActivo()) {
            throw new ConflictoTurnoException("El profesional ya se encuentra inactivo.");
        }

        profesional.setActivo(false);
        profesionalRepository.save(profesional);
    }

    /** Reactiva un profesional dado de baja (solo ADMINISTRADOR). */
    public void reactivarProfesional(Long id) {
        Profesional profesional = obtener(id);

        if (profesional.getActivo()) {
            throw new ConflictoTurnoException("El profesional ya se encuentra activo.");
        }

        profesional.setActivo(true);
        profesionalRepository.save(profesional);
    }

    /** Franjas horarias de un profesional (GET /api/professionals/{id}/availability). */
    @Transactional(readOnly = true)
    public List<HorarioResponseDTO> obtenerHorariosPorProfesional(Long profesionalId) {
        return horarioRepository.findByProfesionalId(profesionalId).stream()
                .map(HorarioResponseDTO::from).collect(Collectors.toList());
    }

    /**
     * Carga una franja horaria en la agenda del profesional AUTENTICADO. El profesional
     * se obtiene del token: nadie puede cargar horarios en la agenda de otra persona.
     */
    public HorarioResponseDTO guardarHorarioDTO(HorarioRegistroDTO dto, Usuario actual) {
        Profesional profesional = profesionalRepository.findByUsuarioId(actual.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Todavía no tenés un perfil profesional. Un administrador debe crearlo."));

        if (!Boolean.TRUE.equals(profesional.getActivo())) {
            throw new ConflictoTurnoException("Tu perfil profesional está inactivo y no puede modificar la agenda.");
        }

        // La hora de fin debe ser posterior a la hora de inicio
        if (!dto.getHoraFin().isAfter(dto.getHoraInicio())) {
            throw new ConflictoTurnoException("La hora de fin debe ser posterior a la hora de inicio.");
        }

        Horario horario = new Horario();
        horario.setProfesional(profesional);
        horario.setDiaSemana(dto.getDiaSemana());
        horario.setHoraInicio(dto.getHoraInicio());
        horario.setHoraFin(dto.getHoraFin());

        return HorarioResponseDTO.from(horarioRepository.save(horario));
    }

    private Profesional obtener(Long id) {
        return profesionalRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Profesional no encontrado con ID: " + id));
    }
}
