package com.turnofacil.backend;

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
import com.turnofacil.backend.services.ProfesionalService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.RecordComponent;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProfesionalServiceTest {

    @Mock
    private ProfesionalRepository profesionalRepository;
    @Mock
    private HorarioRepository horarioRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private EspecialidadRepository especialidadRepository;

    @InjectMocks
    private ProfesionalService profesionalService;

    private Usuario usuario(Long id, Rol rol) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNombre("Ana");
        u.setApellido("Gómez");
        u.setPassword("$2a$10$hashquenodebesalir");
        u.setRol(rol);
        return u;
    }

    private ProfesionalRegistroDTO altaDto(Long usuarioId) {
        ProfesionalRegistroDTO dto = new ProfesionalRegistroDTO();
        dto.setUsuarioId(usuarioId);
        dto.setEspecialidadId(3L);
        dto.setTelefono("2234567890");
        return dto;
    }

    @Test
    @DisplayName("RN-05: No se puede crear un perfil profesional para un usuario CLIENTE")
    void rn05_clienteNoPuedeSerProfesional() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario(1L, Rol.CLIENTE)));

        ConflictoTurnoException ex = assertThrows(ConflictoTurnoException.class,
                () -> profesionalService.guardarProfesionalDTO(altaDto(1L)));

        assertTrue(ex.getMessage().contains("RN-05"));
        verify(profesionalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Un usuario con rol PROFESIONAL (asignado por el admin) puede tener su perfil")
    void usuarioProfesionalPuedeCrearPerfil() {
        Especialidad esp = new Especialidad();
        esp.setId(3L);
        esp.setNombre("Odontología");
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario(2L, Rol.PROFESIONAL)));
        when(profesionalRepository.existsByUsuarioId(2L)).thenReturn(false);
        when(especialidadRepository.findById(3L)).thenReturn(Optional.of(esp));
        when(profesionalRepository.save(any(Profesional.class))).thenAnswer(i -> i.getArgument(0));

        ProfesionalResponseDTO res = profesionalService.guardarProfesionalDTO(altaDto(2L));

        assertEquals(2L, res.usuarioId());
        assertEquals("Odontología", res.especialidad());
        assertTrue(res.activo());
    }

    @Test
    @DisplayName("No se puede crear un segundo perfil para el mismo usuario")
    void noDuplicaPerfil() {
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario(2L, Rol.PROFESIONAL)));
        when(profesionalRepository.existsByUsuarioId(2L)).thenReturn(true);

        assertThrows(ConflictoTurnoException.class, () -> profesionalService.guardarProfesionalDTO(altaDto(2L)));
    }

    @Test
    @DisplayName("La respuesta de profesionales no expone la entidad Usuario ni el password")
    void dtoNoExponeUsuarioNiPassword() {
        Especialidad esp = new Especialidad();
        esp.setId(3L);
        esp.setNombre("Odontología");
        Profesional p = new Profesional();
        p.setId(1L);
        p.setUsuario(usuario(2L, Rol.PROFESIONAL));
        p.setEspecialidad(esp);
        p.setTelefono("123");

        ProfesionalResponseDTO dto = ProfesionalResponseDTO.from(p);

        String campos = Arrays.stream(ProfesionalResponseDTO.class.getRecordComponents())
                .map(RecordComponent::getName).reduce("", (a, b) -> a + "," + b).toLowerCase();
        assertFalse(campos.contains("password"));
        assertFalse(campos.contains("authorities"));
        assertFalse(dto.toString().contains("hashquenodebesalir"));
    }

    @Test
    @DisplayName("Los horarios se cargan en el perfil del usuario del token, no en el de otra persona")
    void horarioSeCargaEnElPerfilDelToken() {
        Usuario actual = usuario(2L, Rol.PROFESIONAL);
        Profesional propio = new Profesional();
        propio.setId(7L);
        propio.setUsuario(actual);
        when(profesionalRepository.findByUsuarioId(2L)).thenReturn(Optional.of(propio));
        when(horarioRepository.save(any(Horario.class))).thenAnswer(i -> i.getArgument(0));

        HorarioRegistroDTO dto = new HorarioRegistroDTO();
        dto.setDiaSemana(1);
        dto.setHoraInicio(LocalTime.of(9, 0));
        dto.setHoraFin(LocalTime.of(13, 0));

        HorarioResponseDTO res = profesionalService.guardarHorarioDTO(dto, actual);

        assertEquals(7L, res.profesionalId());
    }

    @Test
    @DisplayName("Cargar horarios sin perfil profesional devuelve 'no encontrado'")
    void horarioSinPerfil() {
        Usuario actual = usuario(2L, Rol.PROFESIONAL);
        when(profesionalRepository.findByUsuarioId(2L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> profesionalService.guardarHorarioDTO(new HorarioRegistroDTO(), actual));
    }

    @Test
    @DisplayName("La hora de fin debe ser posterior a la de inicio")
    void horarioFinAnteriorAInicio() {
        Usuario actual = usuario(2L, Rol.PROFESIONAL);
        Profesional propio = new Profesional();
        propio.setId(7L);
        propio.setUsuario(actual);
        when(profesionalRepository.findByUsuarioId(2L)).thenReturn(Optional.of(propio));

        HorarioRegistroDTO dto = new HorarioRegistroDTO();
        dto.setDiaSemana(1);
        dto.setHoraInicio(LocalTime.of(13, 0));
        dto.setHoraFin(LocalTime.of(9, 0));

        assertThrows(ConflictoTurnoException.class, () -> profesionalService.guardarHorarioDTO(dto, actual));
        verify(horarioRepository, never()).save(any());
    }
}
