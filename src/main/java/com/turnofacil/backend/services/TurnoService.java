package com.turnofacil.backend.services;

import com.turnofacil.backend.dtos.TurnoRegistroDTO;
import com.turnofacil.backend.dtos.TurnoResponseDTO;
import com.turnofacil.backend.enums.EstadoTurno;
import com.turnofacil.backend.models.Profesional;
import com.turnofacil.backend.models.Turno;
import com.turnofacil.backend.models.Usuario;
import com.turnofacil.backend.repositories.ProfesionalRepository;
import com.turnofacil.backend.repositories.TurnoRepository;
import com.turnofacil.backend.repositories.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TurnoService {

    @Autowired
    private TurnoRepository turnoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProfesionalRepository profesionalRepository;

    /**
     * Registra un nuevo turno validando la regla de negocio de no solapamiento mediante DTO.
     */
    public TurnoResponseDTO crearTurno(TurnoRegistroDTO dto) {
        Usuario cliente = usuarioRepository.findById(dto.getClienteId())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + dto.getClienteId()));

        Profesional profesional = profesionalRepository.findById(dto.getProfesionalId())
                .orElseThrow(() -> new RuntimeException("Profesional no encontrado con ID: " + dto.getProfesionalId()));

        // Validación explícita de solapamiento de turno para la misma fecha y hora
        boolean turnoOcupado = turnoRepository.existsByProfesionalIdAndFechaAndHora(
                dto.getProfesionalId(), dto.getFecha(), dto.getHora()
        );

        if (turnoOcupado) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El profesional ya cuenta con un turno asignado en ese horario.");
        }

        Turno turno = new Turno();
        turno.setCliente(cliente);
        turno.setProfesional(profesional);
        turno.setFecha(dto.getFecha());
        turno.setHora(dto.getHora());
        turno.setDuracionMinutos(dto.getDuracionMinutos() != null ? dto.getDuracionMinutos() : 30);
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
     * Realiza la cancelación lógica de un turno (RN-03 y RN-04),
     * utilizando el Enum EstadoTurno.CANCELADO.
     */
    public TurnoResponseDTO cancelarTurno(Long id) {
        Optional<Turno> turnoOpt = turnoRepository.findById(id);
        if (turnoOpt.isPresent()) {
            Turno turno = turnoOpt.get();
            turno.setEstado(EstadoTurno.CANCELADO);
            Turno turnoActualizado = turnoRepository.save(turno);
            return mapearAResponseDTO(turnoActualizado);
        } else {
            throw new RuntimeException("El turno con ID " + id + " no fue encontrado.");
        }
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
                null // created_at si lo agregas luego en la entidad/dto
        );
    }
}