package com.turnofacil.backend;

import com.turnofacil.backend.dtos.TurnoRegistroDTO;
import com.turnofacil.backend.enums.EstadoTurno;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @Test
    @DisplayName("RN-01: Falla si el turno se encuentra fuera de la disponibilidad horaria")
    void testCrearTurnoFueraDeHorarioThrowsException() {
        TurnoRegistroDTO dto = new TurnoRegistroDTO();
        dto.setClienteId(1L);
        dto.setProfesionalId(1L);
        dto.setFecha(LocalDate.now().plusDays(1));
        dto.setHora(LocalTime.of(15, 0));
        dto.setDuracionMinutos(30);

        Usuario cliente = new Usuario();
        Profesional profesional = new Profesional();
        Horario horario = new Horario();
        horario.setHoraInicio(LocalTime.of(8, 0));
        horario.setHoraFin(LocalTime.of(12, 0));

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(profesionalRepository.findById(1L)).thenReturn(Optional.of(profesional));
        when(horarioRepository.findByProfesionalId(1L)).thenReturn(List.of(horario));

        ConflictoTurnoException exception1 = assertThrows(ConflictoTurnoException.class, () -> {
            turnoService.crearTurno(dto);
        });

        assertTrue(exception1.getMessage().contains("fuera de la disponibilidad horaria"));
    }

    @Test
    @DisplayName("RN-04: Falla si se intenta cancelar un turno con menos de 2 horas")
    void testCancelarTurnoConMenosDeDosHorasThrowsException() {
        Long turnoId = 10L;
        Turno turno = new Turno();
        turno.setFecha(LocalDate.now());
        turno.setHora(LocalTime.now().plusMinutes(30));
        turno.setEstado(EstadoTurno.PENDIENTE);

        when(turnoRepository.findById(turnoId)).thenReturn(Optional.of(turno));

        ConflictoTurnoException exception2 = assertThrows(ConflictoTurnoException.class, () -> {
            turnoService.cancelarTurno(turnoId);
        });

        assertTrue(exception2.getMessage().contains("al menos 2 horas de anticipación"));
    }
}