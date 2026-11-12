package com.turnofacil.backend.services;

import com.turnofacil.backend.dtos.TurnoRegistroDTO;
import com.turnofacil.backend.dtos.TurnoReprogramacionDTO;
import com.turnofacil.backend.dtos.TurnoResponseDTO;
import com.turnofacil.backend.enums.EstadoTurno;
import com.turnofacil.backend.enums.Rol;
import com.turnofacil.backend.exceptions.ConflictoTurnoException;
import com.turnofacil.backend.exceptions.RecursoNoEncontradoException;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional
public class TurnoService {

    private static final int DURACION_POR_DEFECTO = 30;

    @Autowired
    private TurnoRepository turnoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProfesionalRepository profesionalRepository;

    @Autowired
    private HorarioRepository horarioRepository;

    // ------------------------------------------------------------------
    // Alta
    // ------------------------------------------------------------------

    /**
     * Registra un turno para el cliente autenticado (clienteId proviene del token, nunca del body).
     * Valida profesional activo, RN-01 (día y franja horaria), RN-02 (solapamiento por rango)
     * y RN-03 (los turnos cancelados no bloquean el horario).
     */
    public TurnoResponseDTO crearTurno(Long clienteId, TurnoRegistroDTO dto) {
        Usuario cliente = usuarioRepository.findById(clienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado con ID: " + clienteId));

        Profesional profesional = profesionalRepository.findById(dto.getProfesionalId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Profesional no encontrado con ID: " + dto.getProfesionalId()));

        int duracion = dto.getDuracionMinutos() != null ? dto.getDuracionMinutos() : DURACION_POR_DEFECTO;

        validarNuevoHorario(profesional, dto.getFecha(), dto.getHora(), duracion, null);

        Turno turno = new Turno();
        turno.setCliente(cliente);
        turno.setProfesional(profesional);
        turno.setFecha(dto.getFecha());
        turno.setHora(dto.getHora());
        turno.setDuracionMinutos(duracion);
        turno.setEstado(EstadoTurno.PENDIENTE);

        return guardar(turno);
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    /** Historial de un cliente. Solo el propio cliente o un ADMINISTRADOR pueden verlo. */
    @Transactional(readOnly = true)
    public List<TurnoResponseDTO> listarTurnosPorUsuario(Long usuarioId, Usuario actual) {
        if (!esAdmin(actual) && !Objects.equals(actual.getId(), usuarioId)) {
            throw new AccessDeniedException("No podés ver los turnos de otro usuario.");
        }
        return turnoRepository.findByClienteId(usuarioId).stream()
                .map(this::mapearAResponseDTO)
                .collect(Collectors.toList());
    }

    /** Agenda de un profesional (HU04). Solo el propio profesional o un ADMINISTRADOR. */
    @Transactional(readOnly = true)
    public List<TurnoResponseDTO> listarTurnosPorProfesional(Long profesionalId, Usuario actual) {
        Profesional profesional = profesionalRepository.findById(profesionalId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Profesional no encontrado con ID: " + profesionalId));

        if (!esAdmin(actual) && !esDuenoDelPerfil(profesional, actual)) {
            throw new AccessDeniedException("No podés ver la agenda de otro profesional.");
        }
        return turnoRepository.findByProfesionalId(profesionalId).stream()
                .map(this::mapearAResponseDTO)
                .collect(Collectors.toList());
    }

    /** Listado global para el módulo administrativo, opcionalmente filtrado por fecha. */
    @Transactional(readOnly = true)
    public List<TurnoResponseDTO> listarTodos(LocalDate fecha) {
        List<Turno> turnos = fecha != null
                ? turnoRepository.findByFechaOrderByHoraAsc(fecha)
                : turnoRepository.findAllByOrderByFechaDescHoraDesc();
        return turnos.stream().map(this::mapearAResponseDTO).collect(Collectors.toList());
    }

    // ------------------------------------------------------------------
    // Cambios de estado
    // ------------------------------------------------------------------

    /**
     * Cancelación lógica (RN-04): solo PENDIENTE o CONFIRMADO, con al menos 2 horas de anticipación.
     * Pueden cancelar el cliente dueño, el profesional del turno o un ADMINISTRADOR.
     */
    public TurnoResponseDTO cancelarTurno(Long id, Usuario actual) {
        Turno turno = obtener(id);
        validarAccesoCliente(turno, actual);

        validarEstadoModificable(turno, "cancelar");
        validarAnticipacion(turno, "cancelarse");

        turno.setEstado(EstadoTurno.CANCELADO);
        return mapearAResponseDTO(turnoRepository.save(turno));
    }

    /** Confirma un turno PENDIENTE. Solo el profesional del turno o un ADMINISTRADOR. */
    public TurnoResponseDTO confirmarTurno(Long id, Usuario actual) {
        Turno turno = obtener(id);
        validarAccesoProfesional(turno, actual);

        if (turno.getEstado() != EstadoTurno.PENDIENTE) {
            throw new ConflictoTurnoException("Solo se pueden confirmar turnos que se encuentren en estado PENDIENTE.");
        }

        turno.setEstado(EstadoTurno.CONFIRMADO);
        return mapearAResponseDTO(turnoRepository.save(turno));
    }

    /** Finaliza un turno CONFIRMADO. Solo el profesional del turno o un ADMINISTRADOR. */
    public TurnoResponseDTO finalizarTurno(Long id, Usuario actual) {
        Turno turno = obtener(id);
        validarAccesoProfesional(turno, actual);

        if (turno.getEstado() != EstadoTurno.CONFIRMADO) {
            throw new ConflictoTurnoException("Solo se pueden finalizar turnos que se encuentren en estado CONFIRMADO.");
        }

        turno.setEstado(EstadoTurno.FINALIZADO);
        return mapearAResponseDTO(turnoRepository.save(turno));
    }

    // ------------------------------------------------------------------
    // Reprogramación (RN-04)
    // ------------------------------------------------------------------

    /**
     * Reprograma un turno PENDIENTE o CONFIRMADO con al menos 2 horas de anticipación respecto del
     * horario ORIGINAL. El nuevo horario pasa por las mismas validaciones que una reserva nueva
     * (profesional activo, RN-01, RN-02), excluyendo al propio turno del chequeo de solapamiento.
     * Tras reprogramar, el turno vuelve a PENDIENTE para que el profesional lo confirme de nuevo.
     */
    public TurnoResponseDTO reprogramarTurno(Long id, TurnoReprogramacionDTO dto, Usuario actual) {
        Turno turno = obtener(id);
        validarAccesoCliente(turno, actual);

        validarEstadoModificable(turno, "reprogramar");
        validarAnticipacion(turno, "reprogramarse");

        int duracion = dto.duracionMinutos() != null ? dto.duracionMinutos() : turno.getDuracionMinutos();

        validarNuevoHorario(turno.getProfesional(), dto.fecha(), dto.hora(), duracion, turno.getId());

        turno.setFecha(dto.fecha());
        turno.setHora(dto.hora());
        turno.setDuracionMinutos(duracion);
        turno.setEstado(EstadoTurno.PENDIENTE);
        return guardar(turno);
    }

    // ------------------------------------------------------------------
    // Validaciones de negocio
    // ------------------------------------------------------------------

    /** Reglas comunes a reservar y reprogramar. */
    private void validarNuevoHorario(Profesional profesional, LocalDate fecha, LocalTime inicio,
                                     int duracion, Long turnoIdExcluido) {
        if (!Boolean.TRUE.equals(profesional.getActivo())) {
            throw new ConflictoTurnoException("El profesional no se encuentra activo y no admite nuevos turnos.");
        }

        if (LocalDateTime.of(fecha, inicio).isBefore(LocalDateTime.now())) {
            throw new ConflictoTurnoException("No se pueden reservar turnos en una fecha y hora pasadas.");
        }

        LocalTime fin = inicio.plusMinutes(duracion);
        validarDisponibilidadHoraria(profesional.getId(), fecha, inicio, fin);
        validarSolapamiento(profesional.getId(), fecha, inicio, fin, turnoIdExcluido);
    }

    /**
     * RN-01: el turno debe caer completo dentro de una franja del profesional
     * correspondiente al DÍA DE LA SEMANA de la fecha pedida (1 = lunes ... 7 = domingo).
     */
    private void validarDisponibilidadHoraria(Long profesionalId, LocalDate fecha, LocalTime inicio, LocalTime fin) {
        int diaSemana = fecha.getDayOfWeek().getValue();
        List<Horario> franjas = horarioRepository.findByProfesionalIdAndDiaSemana(profesionalId, diaSemana);

        // Si fin <= inicio el turno cruzaría la medianoche: nunca entra en una franja diaria
        boolean dentro = fin.isAfter(inicio) && franjas.stream().anyMatch(h ->
                !inicio.isBefore(h.getHoraInicio()) && !fin.isAfter(h.getHoraFin()));

        if (!dentro) {
            throw new ConflictoTurnoException(
                    "El turno seleccionado está fuera de la disponibilidad horaria del profesional para ese día (RN-01).");
        }
    }

    /**
     * RN-02 y RN-03: no puede haber dos turnos activos del mismo profesional cuyos rangos
     * [inicio, fin) se superpongan, ni siquiera parcialmente. Los CANCELADOS no cuentan.
     */
    private void validarSolapamiento(Long profesionalId, LocalDate fecha, LocalTime inicio, LocalTime fin,
                                     Long turnoIdExcluido) {
        List<Turno> activos = turnoRepository.findByProfesionalIdAndFechaAndEstadoNot(
                profesionalId, fecha, EstadoTurno.CANCELADO);

        boolean solapa = activos.stream()
                .filter(t -> turnoIdExcluido == null || !Objects.equals(t.getId(), turnoIdExcluido))
                .anyMatch(t -> {
                    LocalTime tInicio = t.getHora();
                    LocalTime tFin = tInicio.plusMinutes(t.getDuracionMinutos());
                    return inicio.isBefore(tFin) && fin.isAfter(tInicio);
                });

        if (solapa) {
            throw new ConflictoTurnoException(
                    "El horario se superpone con otro turno activo del profesional (RN-02).");
        }
    }

    private void validarEstadoModificable(Turno turno, String accion) {
        if (turno.getEstado() != EstadoTurno.PENDIENTE && turno.getEstado() != EstadoTurno.CONFIRMADO) {
            throw new ConflictoTurnoException(
                    "Solo se pueden " + accion + " turnos en estado PENDIENTE o CONFIRMADO (estado actual: "
                            + turno.getEstado() + ").");
        }
    }

    private void validarAnticipacion(Turno turno, String verbo) {
        LocalDateTime inicio = LocalDateTime.of(turno.getFecha(), turno.getHora());
        if (LocalDateTime.now().isAfter(inicio.minusHours(2))) {
            throw new ConflictoTurnoException(
                    "Los turnos solo pueden " + verbo + " con al menos 2 horas de anticipación (RN-04).");
        }
    }

    // ------------------------------------------------------------------
    // Autorización sobre el recurso (propiedad del turno)
    // ------------------------------------------------------------------

    /** Cliente dueño, profesional del turno o administrador. */
    private void validarAccesoCliente(Turno turno, Usuario actual) {
        boolean esCliente = turno.getCliente() != null && Objects.equals(turno.getCliente().getId(), actual.getId());
        if (!(esAdmin(actual) || esCliente || esProfesionalDelTurno(turno, actual))) {
            throw new AccessDeniedException("El turno no pertenece al usuario autenticado.");
        }
    }

    /** Profesional del turno o administrador. */
    private void validarAccesoProfesional(Turno turno, Usuario actual) {
        if (!(esAdmin(actual) || esProfesionalDelTurno(turno, actual))) {
            throw new AccessDeniedException("El turno no pertenece a la agenda del usuario autenticado.");
        }
    }

    private boolean esAdmin(Usuario u) {
        return u != null && u.getRol() == Rol.ADMINISTRADOR;
    }

    private boolean esProfesionalDelTurno(Turno turno, Usuario actual) {
        return esDuenoDelPerfil(turno.getProfesional(), actual);
    }

    private boolean esDuenoDelPerfil(Profesional profesional, Usuario actual) {
        return profesional != null && profesional.getUsuario() != null
                && Objects.equals(profesional.getUsuario().getId(), actual.getId());
    }

    // ------------------------------------------------------------------
    // Auxiliares
    // ------------------------------------------------------------------

    private Turno obtener(Long id) {
        return turnoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("El turno con ID " + id + " no fue encontrado."));
    }

    // La restricción de exclusión de la base es la última barrera ante reservas concurrentes
    private TurnoResponseDTO guardar(Turno turno) {
        try {
            return mapearAResponseDTO(turnoRepository.save(turno));
        } catch (DataIntegrityViolationException e) {
            throw new ConflictoTurnoException("El horario se superpone con otro turno activo del profesional (RN-02).");
        }
    }

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
