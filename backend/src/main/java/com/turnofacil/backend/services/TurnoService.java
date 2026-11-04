package com.turnofacil.backend.services;

import com.turnofacil.backend.dtos.TurnoRegistroDTO;
import com.turnofacil.backend.dtos.TurnoResponseDTO;
import com.turnofacil.backend.enums.EstadoTurno;
import com.turnofacil.backend.models.Horario;
import com.turnofacil.backend.models.Profesional;
import com.turnofacil.backend.models.Turno;
import com.turnofacil.backend.models.Usuario;
import com.turnofacil.backend.repositories.HorarioRepository;
import com.turnofacil.backend.repositories.ProfesionalRepository;
import com.turnofacil.backend.repositories.TurnoRepository;
import com.turnofacil.backend.repositories.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TurnoService {

    @Autowired
    private TurnoRepository turnoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProfesionalRepository profesionalRepository;

    @Autowired
    private HorarioRepository horarioRepository; // Inyectamos para validar RN-01

    /**
     * Registra un nuevo turno validando la disponibilidad horaria (RN-01)
     * y el no solapamiento excluyendo turnos cancelados (RN-02 y RN-03).
     */
    public TurnoResponseDTO crearTurno(TurnoRegistroDTO dto) {
        Usuario cliente = usuarioRepository.findById(dto.getClienteId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado con ID: " + dto.getClienteId()));

        Profesional profesional = profesionalRepository.findById(dto.getProfesionalId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profesional no encontrado con ID: " + dto.getProfesionalId()));

        // --- RN-01: Validar que el turno caiga dentro de los horarios del profesional ---
        List<Horario> horariosProfesional = horarioRepository.findByProfesionalId(dto.getProfesionalId());

        int duracion = dto.getDuracionMinutos() != null ? dto.getDuracionMinutos() : 30;
        LocalTime horaInicioTurno = dto.getHora();
        LocalTime horaFinTurno = horaInicioTurno.plusMinutes(duracion);

        boolean estaDentroDeHorario = horariosProfesional.stream().anyMatch(h ->
                !horaInicioTurno.isBefore(h.getHoraInicio()) && !horaFinTurno.isAfter(h.getHoraFin())
        );

        if (!estaDentroDeHorario) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El turno seleccionado está fuera de la disponibilidad horaria del profesional (RN-01).");
        }

        // --- RN-02 y RN-03: Validar solapamiento excluyendo turnos CANCELADOS ---
        boolean turnoOcupado = turnoRepository.existsByProfesionalIdAndFechaAndHoraAndEstadoNot(
                dto.getProfesionalId(), dto.getFecha(), dto.getHora(), EstadoTurno.CANCELADO
        );

        if (turnoOcupado) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El profesional ya cuenta con un turno activo asignado en ese horario.");
        }

        Turno turno = new Turno();
        turno.setCliente(cliente);
        turno.setProfesional(profesional);
        turno.setFecha(dto.getFecha());
        turno.setHora(dto.getHora());
        turno.setDuracionMinutos(duracion);
        turno.setEstado(EstadoTurno.PENDIENTE);

        try {
            Turno turnoGuardado = turnoRepository.save(turno);
            return mapearAResponseDTO(turnoGuardado);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error de integridad en la base de datos al guardar el turno.");
        }
    }

    /**
     * Obtiene el historial de turnos de un cliente específico mapeado a DTOs.
     */
    public List<TurnoResponseDTO> listarTurnosPorUsuario(Long usuarioId) {
        List<Turno> turnos = turnoRepository.findByClienteId(usuarioId);
        return turnos.stream()
                .map(this::mapearAResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene los turnos asignados a un profesional específico mapeado a DTOs.
     */
    public List<TurnoResponseDTO> listarTurnosPorProfesional(Long profesionalId) {
        List<Turno> turnos = turnoRepository.findByProfesionalId(profesionalId);
        return turnos.stream()
                .map(this::mapearAResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Realiza la cancelación lógica de un turno validando la regla RN-04
     * (debe cancelarse con al menos 2 horas de anticipación).
     */
    public TurnoResponseDTO cancelarTurno(Long id) {
        Turno turno = turnoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El turno con ID " + id + " no fue encontrado."));

        if (turno.getEstado() == EstadoTurno.CANCELADO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El turno ya se encuentra cancelado.");
        }

        // --- RN-04: Validar 2 horas de anticipación ---
        LocalDateTime fechaHoraTurno = LocalDateTime.of(turno.getFecha(), turno.getHora());
        LocalDateTime ahora = LocalDateTime.now();

        if (ahora.isAfter(fechaHoraTurno.minusHours(2))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Los turnos solo pueden cancelarse con al menos 2 horas de anticipación (RN-04).");
        }

        turno.setEstado(EstadoTurno.CANCELADO);
        Turno turnoActualizado = turnoRepository.save(turno);
        return mapearAResponseDTO(turnoActualizado);
    }

    // Método auxiliar para mapear Entidad -> TurnoResponseDTO
    private TurnoResponseDTO mapearAResponseDTO(Turno turno) {
        return new TurnoResponseDTO(
                turno.getId(),
                turno.getCliente().getId(),
                turno.getCliente().getNombre() + " " + turno.getCliente().getApellido(),
                turno.getProfesional().getId(),
                turno.getFecha(),
                turno.getHora(),
                turno.getDuracionMinutos(),
                turno.getEstado(),
                null
        );
    }
}