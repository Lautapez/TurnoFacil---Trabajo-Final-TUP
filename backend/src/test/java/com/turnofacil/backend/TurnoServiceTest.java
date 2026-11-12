package com.turnofacil.backend;

import com.turnofacil.backend.dtos.TurnoRegistroDTO;
import com.turnofacil.backend.dtos.TurnoReprogramacionDTO;
import com.turnofacil.backend.dtos.TurnoResponseDTO;
import com.turnofacil.backend.enums.EstadoTurno;
import com.turnofacil.backend.enums.Rol;
import com.turnofacil.backend.exceptions.ConflictoTurnoException;
import com.turnofacil.backend.models.Horario;
import com.turnofacil.backend.models.Profesional;
import com.turnofacil.backend.models.Turno;
import com.turnofacil.backend.models.Usuario;
import com.turnofacil.backend.repositories.HorarioRepository;
import com.turnofacil.backend.repositories.ProfesionalRepository;
import com.turnofacil.backend.repositories.TurnoRepository;
import com.turnofacil.backend.repositories.UsuarioRepository;
import com.turnofacil.backend.services.TurnoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TurnoServiceTest {

    @Mock
    private TurnoRepository turnoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ProfesionalRepository profesionalRepository;

    @Mock
    private HorarioRepository horarioRepository;

    @InjectMocks
    private TurnoService turnoService;

    // Próximo lunes (siempre futuro) y próximo domingo, para no depender del día en que corre el test
    private static final LocalDate LUNES = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    private static final LocalDate DOMINGO = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.SUNDAY));

    // ---------------------------------------------------------------- helpers

    private Usuario usuario(Long id, Rol rol) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNombre("Nombre" + id);
        u.setApellido("Apellido" + id);
        u.setRol(rol);
        return u;
    }

    private Profesional profesional(Long id, Usuario usuario) {
        Profesional p = new Profesional();
        p.setId(id);
        p.setUsuario(usuario);
        return p;
    }

    private Horario franja(Profesional p, int dia, int desde, int hasta) {
        Horario h = new Horario();
        h.setProfesional(p);
        h.setDiaSemana(dia);
        h.setHoraInicio(LocalTime.of(desde, 0));
        h.setHoraFin(LocalTime.of(hasta, 0));
        return h;
    }

    private Turno turno(Long id, Usuario cliente, Profesional p, LocalDate fecha, LocalTime hora, int dur, EstadoTurno estado) {
        Turno t = new Turno();
        t.setId(id);
        t.setCliente(cliente);
        t.setProfesional(p);
        t.setFecha(fecha);
        t.setHora(hora);
        t.setDuracionMinutos(dur);
        t.setEstado(estado);
        return t;
    }

    private TurnoRegistroDTO dto(LocalDate fecha, LocalTime hora, int dur) {
        TurnoRegistroDTO dto = new TurnoRegistroDTO();
        dto.setProfesionalId(1L);
        dto.setFecha(fecha);
        dto.setHora(hora);
        dto.setDuracionMinutos(dur);
        return dto;
    }

    /** Prepara cliente 10, profesional 1 (usuario 20) con franja de lunes de 9 a 13. */
    private Profesional prepararReserva(Usuario cliente, Usuario usuarioProfesional) {
        Profesional pro = profesional(1L, usuarioProfesional);
        when(usuarioRepository.findById(cliente.getId())).thenReturn(Optional.of(cliente));
        when(profesionalRepository.findById(1L)).thenReturn(Optional.of(pro));
        return pro;
    }

    // ---------------------------------------------------------------- RN-01

    @Test
    @DisplayName("RN-01: Falla si el turno se encuentra fuera de la franja horaria del día")
    void rn01_fueraDeFranjaHoraria() {
        Usuario cliente = usuario(10L, Rol.CLIENTE);
        Profesional pro = prepararReserva(cliente, usuario(20L, Rol.PROFESIONAL));
        when(horarioRepository.findByProfesionalIdAndDiaSemana(1L, 1)).thenReturn(List.of(franja(pro, 1, 8, 12)));

        ConflictoTurnoException ex = assertThrows(ConflictoTurnoException.class,
                () -> turnoService.crearTurno(10L, dto(LUNES, LocalTime.of(15, 0), 30)));

        assertTrue(ex.getMessage().contains("fuera de la disponibilidad horaria"));
        verify(turnoRepository, never()).save(any());
    }

    @Test
    @DisplayName("RN-01: Falla si el profesional no atiende ese día de la semana (lunes 9-13, turno un domingo)")
    void rn01_diaSinFranja() {
        Usuario cliente = usuario(10L, Rol.CLIENTE);
        prepararReserva(cliente, usuario(20L, Rol.PROFESIONAL));
        // El domingo es el día 7: el profesional solo tiene franja el lunes (día 1), así que no hay nada para el 7
        when(horarioRepository.findByProfesionalIdAndDiaSemana(1L, 7)).thenReturn(List.of());

        ConflictoTurnoException ex = assertThrows(ConflictoTurnoException.class,
                () -> turnoService.crearTurno(10L, dto(DOMINGO, LocalTime.of(10, 0), 30)));

        assertTrue(ex.getMessage().contains("fuera de la disponibilidad horaria"));
        verify(horarioRepository).findByProfesionalIdAndDiaSemana(1L, 7);
        verify(turnoRepository, never()).save(any());
    }

    @Test
    @DisplayName("RN-01: Falla si el turno empieza dentro de la franja pero termina después de que cierra")
    void rn01_terminaDespuesDelCierre() {
        Usuario cliente = usuario(10L, Rol.CLIENTE);
        Profesional pro = prepararReserva(cliente, usuario(20L, Rol.PROFESIONAL));
        when(horarioRepository.findByProfesionalIdAndDiaSemana(1L, 1)).thenReturn(List.of(franja(pro, 1, 9, 13)));

        assertThrows(ConflictoTurnoException.class,
                () -> turnoService.crearTurno(10L, dto(LUNES, LocalTime.of(12, 45), 30)));
    }

    @Test
    @DisplayName("RN-01: Reserva válida dentro de la franja del día queda PENDIENTE")
    void rn01_reservaValida() {
        Usuario cliente = usuario(10L, Rol.CLIENTE);
        Profesional pro = prepararReserva(cliente, usuario(20L, Rol.PROFESIONAL));
        when(horarioRepository.findByProfesionalIdAndDiaSemana(1L, 1)).thenReturn(List.of(franja(pro, 1, 9, 13)));
        when(turnoRepository.findByProfesionalIdAndFechaAndEstadoNot(1L, LUNES, EstadoTurno.CANCELADO)).thenReturn(List.of());
        when(turnoRepository.save(any(Turno.class))).thenAnswer(i -> i.getArgument(0));

        TurnoResponseDTO res = turnoService.crearTurno(10L, dto(LUNES, LocalTime.of(10, 0), 30));

        assertEquals(EstadoTurno.PENDIENTE, res.getEstado());
        assertEquals(10L, res.getClienteId());
    }

    // ---------------------------------------------------------------- RN-02

    @Test
    @DisplayName("RN-02: Falla ante solapamiento PARCIAL (existente 10:00-10:30, nuevo 10:15-10:45)")
    void rn02_solapamientoParcial() {
        Usuario cliente = usuario(10L, Rol.CLIENTE);
        Profesional pro = prepararReserva(cliente, usuario(20L, Rol.PROFESIONAL));
        when(horarioRepository.findByProfesionalIdAndDiaSemana(1L, 1)).thenReturn(List.of(franja(pro, 1, 9, 13)));
        Turno existente = turno(1L, usuario(11L, Rol.CLIENTE), pro, LUNES, LocalTime.of(10, 0), 30, EstadoTurno.CONFIRMADO);
        when(turnoRepository.findByProfesionalIdAndFechaAndEstadoNot(1L, LUNES, EstadoTurno.CANCELADO)).thenReturn(List.of(existente));

        ConflictoTurnoException ex = assertThrows(ConflictoTurnoException.class,
                () -> turnoService.crearTurno(10L, dto(LUNES, LocalTime.of(10, 15), 30)));

        assertTrue(ex.getMessage().contains("se superpone"));
        verify(turnoRepository, never()).save(any());
    }

    @Test
    @DisplayName("RN-02: Falla si el nuevo turno largo cubre por completo a uno existente")
    void rn02_nuevoEnvuelveAlExistente() {
        Usuario cliente = usuario(10L, Rol.CLIENTE);
        Profesional pro = prepararReserva(cliente, usuario(20L, Rol.PROFESIONAL));
        when(horarioRepository.findByProfesionalIdAndDiaSemana(1L, 1)).thenReturn(List.of(franja(pro, 1, 9, 13)));
        Turno existente = turno(1L, usuario(11L, Rol.CLIENTE), pro, LUNES, LocalTime.of(10, 30), 30, EstadoTurno.PENDIENTE);
        when(turnoRepository.findByProfesionalIdAndFechaAndEstadoNot(1L, LUNES, EstadoTurno.CANCELADO)).thenReturn(List.of(existente));

        assertThrows(ConflictoTurnoException.class,
                () -> turnoService.crearTurno(10L, dto(LUNES, LocalTime.of(10, 0), 90)));
    }

    @Test
    @DisplayName("RN-02: Turnos consecutivos (uno termina cuando empieza el otro) NO se solapan")
    void rn02_turnosContiguosNoSolapan() {
        Usuario cliente = usuario(10L, Rol.CLIENTE);
        Profesional pro = prepararReserva(cliente, usuario(20L, Rol.PROFESIONAL));
        when(horarioRepository.findByProfesionalIdAndDiaSemana(1L, 1)).thenReturn(List.of(franja(pro, 1, 9, 13)));
        Turno existente = turno(1L, usuario(11L, Rol.CLIENTE), pro, LUNES, LocalTime.of(10, 0), 30, EstadoTurno.CONFIRMADO);
        when(turnoRepository.findByProfesionalIdAndFechaAndEstadoNot(1L, LUNES, EstadoTurno.CANCELADO)).thenReturn(List.of(existente));
        when(turnoRepository.save(any(Turno.class))).thenAnswer(i -> i.getArgument(0));

        TurnoResponseDTO res = turnoService.crearTurno(10L, dto(LUNES, LocalTime.of(10, 30), 30));

        assertEquals(LocalTime.of(10, 30), res.getHora());
    }

    // ---------------------------------------------------------------- RN-03

    @Test
    @DisplayName("RN-03: Un turno CANCELADO no bloquea el horario (la consulta de solapamiento excluye CANCELADO)")
    void rn03_cancelado_noBloqueaElHorario() {
        Usuario cliente = usuario(10L, Rol.CLIENTE);
        Profesional pro = prepararReserva(cliente, usuario(20L, Rol.PROFESIONAL));
        when(horarioRepository.findByProfesionalIdAndDiaSemana(1L, 1)).thenReturn(List.of(franja(pro, 1, 9, 13)));
        // Existe un turno cancelado a las 10:00, por eso la consulta (que excluye CANCELADO) devuelve vacío
        when(turnoRepository.findByProfesionalIdAndFechaAndEstadoNot(1L, LUNES, EstadoTurno.CANCELADO)).thenReturn(List.of());
        when(turnoRepository.save(any(Turno.class))).thenAnswer(i -> i.getArgument(0));

        TurnoResponseDTO res = turnoService.crearTurno(10L, dto(LUNES, LocalTime.of(10, 0), 30));

        assertEquals(EstadoTurno.PENDIENTE, res.getEstado());
        verify(turnoRepository).findByProfesionalIdAndFechaAndEstadoNot(1L, LUNES, EstadoTurno.CANCELADO);
    }

    @Test
    @DisplayName("RN-03: Cancelar es lógico: el turno pasa a CANCELADO y NO se borra")
    void rn03_cancelacionEsLogica() {
        Usuario cliente = usuario(10L, Rol.CLIENTE);
        Profesional pro = profesional(1L, usuario(20L, Rol.PROFESIONAL));
        Turno t = turno(5L, cliente, pro, LUNES.plusWeeks(1), LocalTime.of(10, 0), 30, EstadoTurno.PENDIENTE);
        when(turnoRepository.findById(5L)).thenReturn(Optional.of(t));
        when(turnoRepository.save(any(Turno.class))).thenAnswer(i -> i.getArgument(0));

        TurnoResponseDTO res = turnoService.cancelarTurno(5L, cliente);

        assertEquals(EstadoTurno.CANCELADO, res.getEstado());
        verify(turnoRepository, never()).delete(any());
        verify(turnoRepository, never()).deleteById(any());
    }

    // ---------------------------------------------------------------- profesional activo

    @Test
    @DisplayName("Falla al reservar con un profesional inactivo")
    void reservaConProfesionalInactivo() {
        Usuario cliente = usuario(10L, Rol.CLIENTE);
        Profesional pro = prepararReserva(cliente, usuario(20L, Rol.PROFESIONAL));
        pro.setActivo(false);

        ConflictoTurnoException ex = assertThrows(ConflictoTurnoException.class,
                () -> turnoService.crearTurno(10L, dto(LUNES, LocalTime.of(10, 0), 30)));

        assertTrue(ex.getMessage().contains("no se encuentra activo"));
        verify(turnoRepository, never()).save(any());
    }

    // ---------------------------------------------------------------- RN-04 cancelar

    @Test
    @DisplayName("RN-04: Falla si se intenta cancelar un turno con menos de 2 horas")
    void rn04_cancelarConMenosDeDosHoras() {
        Usuario cliente = usuario(10L, Rol.CLIENTE);
        Profesional pro = profesional(1L, usuario(20L, Rol.PROFESIONAL));
        Turno t = turno(10L, cliente, pro, LocalDate.now(), LocalTime.now().plusMinutes(30), 30, EstadoTurno.PENDIENTE);
        when(turnoRepository.findById(10L)).thenReturn(Optional.of(t));

        ConflictoTurnoException ex = assertThrows(ConflictoTurnoException.class,
                () -> turnoService.cancelarTurno(10L, cliente));

        assertTrue(ex.getMessage().contains("al menos 2 horas de anticipación"));
        verify(turnoRepository, never()).save(any());
    }

    @Test
    @DisplayName("RN-04: No se puede cancelar un turno FINALIZADO")
    void rn04_noSePuedeCancelarFinalizado() {
        Usuario cliente = usuario(10L, Rol.CLIENTE);
        Profesional pro = profesional(1L, usuario(20L, Rol.PROFESIONAL));
        Turno t = turno(10L, cliente, pro, LUNES.plusWeeks(1), LocalTime.of(10, 0), 30, EstadoTurno.FINALIZADO);
        when(turnoRepository.findById(10L)).thenReturn(Optional.of(t));

        assertThrows(ConflictoTurnoException.class, () -> turnoService.cancelarTurno(10L, cliente));
    }

    // ---------------------------------------------------------------- RN-04 reprogramar

    @Test
    @DisplayName("RN-04: Reprogramar mueve el turno, lo deja PENDIENTE y no se choca consigo mismo")
    void rn04_reprogramarOk() {
        Usuario cliente = usuario(10L, Rol.CLIENTE);
        Profesional pro = profesional(1L, usuario(20L, Rol.PROFESIONAL));
        LocalDate fecha = LUNES.plusWeeks(1);
        Turno t = turno(5L, cliente, pro, fecha, LocalTime.of(10, 0), 30, EstadoTurno.CONFIRMADO);
        when(turnoRepository.findById(5L)).thenReturn(Optional.of(t));
        when(horarioRepository.findByProfesionalIdAndDiaSemana(1L, 1)).thenReturn(List.of(franja(pro, 1, 9, 13)));
        // El propio turno (10:00-10:30) solaparía con 10:15, pero debe excluirse del chequeo
        when(turnoRepository.findByProfesionalIdAndFechaAndEstadoNot(1L, fecha, EstadoTurno.CANCELADO)).thenReturn(List.of(t));
        when(turnoRepository.save(any(Turno.class))).thenAnswer(i -> i.getArgument(0));

        TurnoResponseDTO res = turnoService.reprogramarTurno(5L, new TurnoReprogramacionDTO(fecha, LocalTime.of(10, 15), null), cliente);

        assertEquals(LocalTime.of(10, 15), res.getHora());
        assertEquals(30, res.getDuracionMinutos());
        assertEquals(EstadoTurno.PENDIENTE, res.getEstado());
    }

    @Test
    @DisplayName("RN-04: Falla al reprogramar con menos de 2 horas de anticipación")
    void rn04_reprogramarConMenosDeDosHoras() {
        Usuario cliente = usuario(10L, Rol.CLIENTE);
        Profesional pro = profesional(1L, usuario(20L, Rol.PROFESIONAL));
        Turno t = turno(5L, cliente, pro, LocalDate.now(), LocalTime.now().plusMinutes(30), 30, EstadoTurno.PENDIENTE);
        when(turnoRepository.findById(5L)).thenReturn(Optional.of(t));

        ConflictoTurnoException ex = assertThrows(ConflictoTurnoException.class,
                () -> turnoService.reprogramarTurno(5L, new TurnoReprogramacionDTO(LUNES, LocalTime.of(10, 0), null), cliente));

        assertTrue(ex.getMessage().contains("al menos 2 horas de anticipación"));
    }

    @Test
    @DisplayName("Reprogramar aplica RN-02: falla si el nuevo horario choca con otro turno")
    void reprogramarChocaConOtroTurno() {
        Usuario cliente = usuario(10L, Rol.CLIENTE);
        Profesional pro = profesional(1L, usuario(20L, Rol.PROFESIONAL));
        LocalDate fecha = LUNES.plusWeeks(1);
        Turno propio = turno(5L, cliente, pro, fecha, LocalTime.of(9, 0), 30, EstadoTurno.PENDIENTE);
        Turno otro = turno(6L, usuario(11L, Rol.CLIENTE), pro, fecha, LocalTime.of(11, 0), 30, EstadoTurno.CONFIRMADO);
        when(turnoRepository.findById(5L)).thenReturn(Optional.of(propio));
        when(horarioRepository.findByProfesionalIdAndDiaSemana(1L, 1)).thenReturn(List.of(franja(pro, 1, 9, 13)));
        when(turnoRepository.findByProfesionalIdAndFechaAndEstadoNot(1L, fecha, EstadoTurno.CANCELADO)).thenReturn(List.of(propio, otro));

        assertThrows(ConflictoTurnoException.class,
                () -> turnoService.reprogramarTurno(5L, new TurnoReprogramacionDTO(fecha, LocalTime.of(11, 15), null), cliente));
    }

    // ---------------------------------------------------------------- estados

    @Test
    @DisplayName("Finalizar rechaza turnos PENDIENTES (solo CONFIRMADO)")
    void finalizarRechazaPendiente() {
        Usuario userPro = usuario(20L, Rol.PROFESIONAL);
        Profesional pro = profesional(1L, userPro);
        Turno t = turno(5L, usuario(10L, Rol.CLIENTE), pro, LUNES, LocalTime.of(10, 0), 30, EstadoTurno.PENDIENTE);
        when(turnoRepository.findById(5L)).thenReturn(Optional.of(t));

        ConflictoTurnoException ex = assertThrows(ConflictoTurnoException.class,
                () -> turnoService.finalizarTurno(5L, userPro));

        assertTrue(ex.getMessage().contains("CONFIRMADO"));
    }

    @Test
    @DisplayName("Finalizar un turno CONFIRMADO lo pasa a FINALIZADO")
    void finalizarConfirmado() {
        Usuario userPro = usuario(20L, Rol.PROFESIONAL);
        Profesional pro = profesional(1L, userPro);
        Turno t = turno(5L, usuario(10L, Rol.CLIENTE), pro, LUNES, LocalTime.of(10, 0), 30, EstadoTurno.CONFIRMADO);
        when(turnoRepository.findById(5L)).thenReturn(Optional.of(t));
        when(turnoRepository.save(any(Turno.class))).thenAnswer(i -> i.getArgument(0));

        assertEquals(EstadoTurno.FINALIZADO, turnoService.finalizarTurno(5L, userPro).getEstado());
    }

    // ---------------------------------------------------------------- propiedad del turno

    @Test
    @DisplayName("Un cliente no puede cancelar el turno de otro cliente")
    void clienteNoPuedeCancelarTurnoAjeno() {
        Usuario dueno = usuario(10L, Rol.CLIENTE);
        Usuario intruso = usuario(11L, Rol.CLIENTE);
        Profesional pro = profesional(1L, usuario(20L, Rol.PROFESIONAL));
        Turno t = turno(5L, dueno, pro, LUNES.plusWeeks(1), LocalTime.of(10, 0), 30, EstadoTurno.PENDIENTE);
        when(turnoRepository.findById(5L)).thenReturn(Optional.of(t));

        assertThrows(AccessDeniedException.class, () -> turnoService.cancelarTurno(5L, intruso));
        verify(turnoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Un profesional no puede confirmar turnos de la agenda de otro profesional")
    void profesionalNoPuedeConfirmarTurnoAjeno() {
        Usuario otroPro = usuario(21L, Rol.PROFESIONAL);
        Profesional pro = profesional(1L, usuario(20L, Rol.PROFESIONAL));
        Turno t = turno(5L, usuario(10L, Rol.CLIENTE), pro, LUNES, LocalTime.of(10, 0), 30, EstadoTurno.PENDIENTE);
        when(turnoRepository.findById(5L)).thenReturn(Optional.of(t));

        assertThrows(AccessDeniedException.class, () -> turnoService.confirmarTurno(5L, otroPro));
        verify(turnoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Un ADMINISTRADOR puede confirmar cualquier turno PENDIENTE")
    void adminPuedeConfirmar() {
        Usuario admin = usuario(1L, Rol.ADMINISTRADOR);
        Profesional pro = profesional(1L, usuario(20L, Rol.PROFESIONAL));
        Turno t = turno(5L, usuario(10L, Rol.CLIENTE), pro, LUNES, LocalTime.of(10, 0), 30, EstadoTurno.PENDIENTE);
        when(turnoRepository.findById(5L)).thenReturn(Optional.of(t));
        when(turnoRepository.save(any(Turno.class))).thenAnswer(i -> i.getArgument(0));

        assertEquals(EstadoTurno.CONFIRMADO, turnoService.confirmarTurno(5L, admin).getEstado());
    }

    @Test
    @DisplayName("Un cliente no puede ver el historial de otro usuario")
    void clienteNoVeHistorialAjeno() {
        assertThrows(AccessDeniedException.class,
                () -> turnoService.listarTurnosPorUsuario(10L, usuario(11L, Rol.CLIENTE)));
        verifyNoInteractions(turnoRepository);
    }

    @Test
    @DisplayName("Un profesional no puede ver la agenda de otro profesional")
    void profesionalNoVeAgendaAjena() {
        Profesional pro = profesional(1L, usuario(20L, Rol.PROFESIONAL));
        when(profesionalRepository.findById(1L)).thenReturn(Optional.of(pro));

        assertThrows(AccessDeniedException.class,
                () -> turnoService.listarTurnosPorProfesional(1L, usuario(21L, Rol.PROFESIONAL)));
        verify(turnoRepository, never()).findByProfesionalId(any());
    }
}
