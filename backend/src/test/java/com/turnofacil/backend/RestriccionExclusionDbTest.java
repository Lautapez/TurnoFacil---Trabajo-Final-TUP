package com.turnofacil.backend;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Prueba la restricción EXCLUDE (no_solapamiento_turnos) de schema.sql contra una base PostgreSQL REAL.
 *
 * Es opcional: si no está definida la variable de entorno TEST_DB_URL el test se OMITE, de modo que
 * `./mvnw test` funciona en un entorno limpio sin configurar nada. Para ejecutarlo, ver
 * docs/PRUEBA_CONCURRENCIA.md. La base indicada debe tener ya cargado database/schema.sql.
 *
 * Todos los datos que crea los borra al terminar.
 */
@EnabledIfEnvironmentVariable(named = "TEST_DB_URL", matches = ".+")
class RestriccionExclusionDbTest {

    private static final String EXCLUSION_VIOLATION = "23P01"; // exclusion_violation: la restricción rechazó la fila
    // Con dos transacciones simultáneas que se pisan, PostgreSQL puede rechazar a la perdedora de
    // cualquiera de estas dos formas; en ambas la base impidió la doble reserva.
    private static final String DEADLOCK_DETECTED = "40P01";
    private static final LocalDate FECHA = LocalDate.of(2030, 1, 7);

    private static Connection admin;
    private static long clienteId;
    private static long profesionalId;
    private static long otroProfesionalId;
    private static final List<Long> usuarios = new ArrayList<>();
    private static long especialidadId;

    private static String env(String nombre, String porDefecto) {
        String v = System.getenv(nombre);
        return (v == null || v.isBlank()) ? porDefecto : v;
    }

    private static Connection abrir() throws SQLException {
        return DriverManager.getConnection(
                System.getenv("TEST_DB_URL"),
                env("TEST_DB_USERNAME", "postgres"),
                env("TEST_DB_PASSWORD", ""));
    }

    private static long insertar(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private static long crearUsuario(String rol) throws SQLException {
        long id = insertar(admin,
                "INSERT INTO usuarios (nombre, apellido, email, password, rol) VALUES (?,?,?,?,?) RETURNING id",
                "Test", "Exclusion", "test-" + UUID.randomUUID() + "@example.com", "no-es-un-hash-real", rol);
        usuarios.add(id);
        return id;
    }

    private static long crearProfesional() throws SQLException {
        long usuario = crearUsuario("PROFESIONAL");
        return insertar(admin,
                "INSERT INTO profesionales (usuario_id, especialidad_id, telefono) VALUES (?,?,?) RETURNING id",
                (int) usuario, (int) especialidadId, "000");
    }

    private static void insertarTurno(Connection c, long profesional, LocalTime hora, int duracion, String estado) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO turnos (usuario_id, profesional_id, fecha, hora, duracion_minutos, estado) VALUES (?,?,?,?,?,?)")) {
            ps.setInt(1, (int) clienteId);
            ps.setInt(2, (int) profesional);
            ps.setObject(3, FECHA);
            ps.setObject(4, hora);
            ps.setInt(5, duracion);
            ps.setString(6, estado);
            ps.executeUpdate();
        }
    }

    @BeforeAll
    static void prepararDatos() throws SQLException {
        admin = abrir();
        especialidadId = insertar(admin, "INSERT INTO especialidades (nombre) VALUES (?) RETURNING id",
                "Test-" + UUID.randomUUID());
        clienteId = crearUsuario("CLIENTE");
        profesionalId = crearProfesional();
        otroProfesionalId = crearProfesional();
    }

    @AfterEach
    void limpiarTurnos() throws SQLException {
        try (PreparedStatement ps = admin.prepareStatement("DELETE FROM turnos WHERE usuario_id = ?")) {
            ps.setInt(1, (int) clienteId);
            ps.executeUpdate();
        }
    }

    @AfterAll
    static void borrarDatos() throws SQLException {
        try (PreparedStatement ps = admin.prepareStatement("DELETE FROM turnos WHERE usuario_id = ?")) {
            ps.setInt(1, (int) clienteId);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = admin.prepareStatement(
                "DELETE FROM profesionales WHERE id IN (?, ?)")) {
            ps.setInt(1, (int) profesionalId);
            ps.setInt(2, (int) otroProfesionalId);
            ps.executeUpdate();
        }
        for (long u : usuarios) {
            try (PreparedStatement ps = admin.prepareStatement("DELETE FROM usuarios WHERE id = ?")) {
                ps.setInt(1, (int) u);
                ps.executeUpdate();
            }
        }
        try (PreparedStatement ps = admin.prepareStatement("DELETE FROM especialidades WHERE id = ?")) {
            ps.setInt(1, (int) especialidadId);
            ps.executeUpdate();
        }
        admin.close();
    }

    @Test
    @DisplayName("La base rechaza un solapamiento parcial (10:00-10:30 vs 10:15-10:45)")
    void rechazaSolapamientoParcial() throws SQLException {
        insertarTurno(admin, profesionalId, LocalTime.of(10, 0), 30, "PENDIENTE");

        SQLException ex = assertThrows(SQLException.class,
                () -> insertarTurno(admin, profesionalId, LocalTime.of(10, 15), 30, "PENDIENTE"));

        assertEquals(EXCLUSION_VIOLATION, ex.getSQLState());
    }

    @Test
    @DisplayName("La base permite turnos consecutivos (uno termina cuando empieza el otro)")
    void permiteTurnosContiguos() throws SQLException {
        insertarTurno(admin, profesionalId, LocalTime.of(10, 0), 30, "PENDIENTE");
        insertarTurno(admin, profesionalId, LocalTime.of(10, 30), 30, "CONFIRMADO"); // no debe lanzar
    }

    @Test
    @DisplayName("RN-03: un turno CANCELADO no bloquea el horario")
    void canceladoNoBloquea() throws SQLException {
        insertarTurno(admin, profesionalId, LocalTime.of(10, 0), 30, "CANCELADO");
        insertarTurno(admin, profesionalId, LocalTime.of(10, 0), 30, "PENDIENTE"); // no debe lanzar
    }

    @Test
    @DisplayName("El mismo horario en otro profesional no es conflicto")
    void otroProfesionalNoEsConflicto() throws SQLException {
        insertarTurno(admin, profesionalId, LocalTime.of(10, 0), 30, "PENDIENTE");
        insertarTurno(admin, otroProfesionalId, LocalTime.of(10, 0), 30, "PENDIENTE"); // no debe lanzar
    }

    @Test
    @DisplayName("Concurrencia: dos reservas simultáneas del mismo horario, solo una prospera (la otra: 23P01 o 40P01)")
    void dosReservasSimultaneasSoloUnaGana() throws Exception {
        CyclicBarrier salida = new CyclicBarrier(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);

        Callable<String> reserva = () -> {
            try (Connection c = abrir()) {
                c.setAutoCommit(false);
                salida.await(); // ambas transacciones insertan al mismo tiempo
                try {
                    insertarTurno(c, profesionalId, LocalTime.of(11, 0), 30, "PENDIENTE");
                    c.commit();
                    return "OK";
                } catch (SQLException e) {
                    c.rollback();
                    return e.getSQLState();
                }
            }
        };

        try {
            Future<String> a = pool.submit(reserva);
            Future<String> b = pool.submit(reserva);
            List<String> resultados = List.of(a.get(), b.get());

            assertEquals(1, resultados.stream().filter("OK"::equals).count(), "Debe ganar exactamente una: " + resultados);
            assertEquals(1, resultados.stream()
                            .filter(r -> EXCLUSION_VIOLATION.equals(r) || DEADLOCK_DETECTED.equals(r)).count(),
                    "La otra debe ser rechazada por la base (23P01 o 40P01): " + resultados);
        } finally {
            pool.shutdownNow();
        }
    }
}
