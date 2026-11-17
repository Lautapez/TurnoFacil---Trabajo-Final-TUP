package com.turnofacil.backend;

import com.turnofacil.backend.controllers.AdminController;
import com.turnofacil.backend.controllers.EspecialidadController;
import com.turnofacil.backend.controllers.ProfesionalController;
import com.turnofacil.backend.controllers.TurnoController;
import com.turnofacil.backend.dtos.EspecialidadRequestDTO;
import com.turnofacil.backend.dtos.HorarioRegistroDTO;
import com.turnofacil.backend.dtos.ProfesionalRegistroDTO;
import com.turnofacil.backend.dtos.TurnoRegistroDTO;
import com.turnofacil.backend.enums.Rol;
import com.turnofacil.backend.models.Usuario;
import com.turnofacil.backend.services.AdminService;
import com.turnofacil.backend.services.EspecialidadService;
import com.turnofacil.backend.services.ProfesionalService;
import com.turnofacil.backend.services.TurnoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifica las reglas @PreAuthorize de los controladores con seguridad de métodos real
 * (@EnableMethodSecurity) y servicios simulados, sin necesidad de base de datos.
 */
@SpringJUnitConfig(SeguridadPorRolTest.Config.class)
class SeguridadPorRolTest {

    // Solo se registran los controladores reales; sus servicios son mocks (@MockitoBean más abajo).
    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean TurnoController turnoController() { return new TurnoController(); }
        @Bean ProfesionalController profesionalController() { return new ProfesionalController(); }
        @Bean EspecialidadController especialidadController() { return new EspecialidadController(); }
        @Bean AdminController adminController() { return new AdminController(); }
    }

    // @MockitoBean crea los mocks SIN inyectarles dependencias. Registrarlos con @Bean + Mockito.mock()
    // hacía que Spring intentara resolver sus campos @Autowired (por ejemplo TurnoRepository) y fallara.
    @MockitoBean private TurnoService turnoService;
    @MockitoBean private ProfesionalService profesionalService;
    @MockitoBean private EspecialidadService especialidadService;
    @MockitoBean private AdminService adminService;

    @Autowired private TurnoController turnoController;
    @Autowired private ProfesionalController profesionalController;
    @Autowired private EspecialidadController especialidadController;
    @Autowired private AdminController adminController;

    private Usuario usuario(Rol rol) {
        Usuario u = new Usuario();
        u.setId(1L);
        u.setRol(rol);
        return u;
    }

    // ------------------------------------------------------------ CLIENTE

    @Test
    @WithMockUser(roles = "CLIENTE")
    @DisplayName("CLIENTE no puede dar de baja profesionales")
    void clienteNoPuedeDarDeBaja() {
        assertThrows(AccessDeniedException.class, () -> profesionalController.darDeBajaProfesional(1L));
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    @DisplayName("CLIENTE no puede confirmar ni finalizar turnos")
    void clienteNoPuedeConfirmarNiFinalizar() {
        Usuario u = usuario(Rol.CLIENTE);
        assertThrows(AccessDeniedException.class, () -> turnoController.confirmarTurno(1L, u));
        assertThrows(AccessDeniedException.class, () -> turnoController.finalizarTurno(1L, u));
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    @DisplayName("CLIENTE no puede cargar horarios ni crear perfiles profesionales")
    void clienteNoPuedeCargarHorariosNiCrearPerfiles() {
        Usuario u = usuario(Rol.CLIENTE);
        assertThrows(AccessDeniedException.class, () -> profesionalController.guardarHorario(new HorarioRegistroDTO(), u));
        assertThrows(AccessDeniedException.class, () -> profesionalController.eliminarHorario(1L, u));
        assertThrows(AccessDeniedException.class, () -> profesionalController.crearProfesional(new ProfesionalRegistroDTO()));
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    @DisplayName("CLIENTE no puede ver la agenda de profesionales ni usar el módulo administrativo")
    void clienteNoAccedeAAgendaNiAdmin() {
        Usuario u = usuario(Rol.CLIENTE);
        assertThrows(AccessDeniedException.class, () -> turnoController.listarTurnosPorProfesional(1L, u));
        assertThrows(AccessDeniedException.class, () -> adminController.dashboard());
        assertThrows(AccessDeniedException.class, () -> adminController.listarUsuarios());
        assertThrows(AccessDeniedException.class, () -> especialidadController.crear(new EspecialidadRequestDTO("X")));
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    @DisplayName("CLIENTE sí puede reservar, cancelar y reprogramar sus turnos")
    void clientePuedeReservarYCancelar() {
        Usuario u = usuario(Rol.CLIENTE);
        assertDoesNotThrow(() -> turnoController.crearTurno(new TurnoRegistroDTO(), u));
        assertDoesNotThrow(() -> turnoController.cancelarTurno(1L, u));
        assertDoesNotThrow(() -> turnoController.reprogramarTurno(1L, null, u));
    }

    // ------------------------------------------------------------ PROFESIONAL

    @Test
    @WithMockUser(roles = "PROFESIONAL")
    @DisplayName("PROFESIONAL no puede reservar turnos, dar de baja profesionales ni entrar al módulo admin")
    void profesionalNoPuedeReservarNiAdministrar() {
        Usuario u = usuario(Rol.PROFESIONAL);
        assertThrows(AccessDeniedException.class, () -> turnoController.crearTurno(new TurnoRegistroDTO(), u));
        assertThrows(AccessDeniedException.class, () -> profesionalController.darDeBajaProfesional(1L));
        assertThrows(AccessDeniedException.class, () -> adminController.dashboard());
    }

    @Test
    @WithMockUser(roles = "PROFESIONAL")
    @DisplayName("PROFESIONAL sí puede confirmar, finalizar, ver su agenda y cargar horarios")
    void profesionalPuedeGestionarSuAgenda() {
        Usuario u = usuario(Rol.PROFESIONAL);
        assertDoesNotThrow(() -> turnoController.confirmarTurno(1L, u));
        assertDoesNotThrow(() -> turnoController.finalizarTurno(1L, u));
        assertDoesNotThrow(() -> turnoController.listarTurnosPorProfesional(1L, u));
        assertDoesNotThrow(() -> profesionalController.guardarHorario(new HorarioRegistroDTO(), u));
        assertDoesNotThrow(() -> profesionalController.miPerfil(u));
        assertDoesNotThrow(() -> profesionalController.eliminarHorario(1L, u));
    }

    // ------------------------------------------------------------ ADMINISTRADOR

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("ADMINISTRADOR puede dar de baja profesionales, crear perfiles y usar el módulo admin")
    void adminPuedeAdministrar() {
        assertDoesNotThrow(() -> profesionalController.darDeBajaProfesional(1L));
        assertDoesNotThrow(() -> profesionalController.crearProfesional(new ProfesionalRegistroDTO()));
        assertDoesNotThrow(() -> adminController.dashboard());
        assertDoesNotThrow(() -> adminController.listarUsuarios());
        assertDoesNotThrow(() -> adminController.listarTurnos(null));
        assertDoesNotThrow(() -> especialidadController.crear(new EspecialidadRequestDTO("Cardiología")));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("ADMINISTRADOR no reserva turnos ni carga horarios propios (no es CLIENTE ni PROFESIONAL)")
    void adminNoReservaNiCargaHorarios() {
        Usuario u = usuario(Rol.ADMINISTRADOR);
        assertThrows(AccessDeniedException.class, () -> turnoController.crearTurno(new TurnoRegistroDTO(), u));
        assertThrows(AccessDeniedException.class, () -> profesionalController.guardarHorario(new HorarioRegistroDTO(), u));
    }
}
