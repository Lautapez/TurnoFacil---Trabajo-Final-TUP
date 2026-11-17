# Prueba de la restricción de no solapamiento (concurrencia)

**Qué se demuestra:** aunque el servicio valide RN-02 antes de guardar, dos clientes pueden reservar el mismo horario *al mismo tiempo* y ambos pasar esa validación (condición de carrera). La última barrera es la restricción `EXCLUDE` de `database/schema.sql`:

```sql
CONSTRAINT no_solapamiento_turnos EXCLUDE USING gist (
    profesional_id WITH =,
    tsrange((fecha + hora)::timestamp, (fecha + hora + (duracion_minutos * interval '1 minute'))::timestamp, '[)') WITH &&
) WHERE (estado != 'CANCELADO')
```

La base rechaza cualquier turno activo del mismo profesional cuyo rango `[inicio, fin)` se superponga con otro, aunque sea parcialmente. Los turnos `CANCELADO` no cuentan (RN-03) y dos turnos consecutivos (uno termina cuando empieza el otro) no se superponen.

Hay dos formas de demostrarlo: el test automático (A) y la demostración manual con dos sesiones (B).

---

## A) Test automático contra PostgreSQL real

El test `RestriccionExclusionDbTest` es **opcional**: si no se define `TEST_DB_URL` se omite, y `./mvnw test` sigue funcionando en un entorno limpio.

1. Crear una base de prueba y cargarle el esquema:
   ```bash
   createdb -U postgres turnofacil_test
   psql -U postgres -d turnofacil_test -f database/schema.sql
   ```
2. Ejecutarlo:
   ```bash
   cd backend
   # Linux / Mac
   TEST_DB_URL=jdbc:postgresql://localhost:5432/turnofacil_test TEST_DB_USERNAME=postgres TEST_DB_PASSWORD=tu_clave \
     ./mvnw test -Dtest=RestriccionExclusionDbTest

   # PowerShell
   $env:TEST_DB_URL="jdbc:postgresql://localhost:5432/turnofacil_test"; $env:TEST_DB_USERNAME="postgres"; $env:TEST_DB_PASSWORD="tu_clave"
   .\mvnw.cmd test -Dtest=RestriccionExclusionDbTest
   ```

Qué comprueba (todos los datos que crea los borra al terminar):

| Caso | Resultado esperado |
|---|---|
| 10:00-10:30 y luego 10:15-10:45 (mismo profesional) | La base lanza `exclusion_violation` (SQLSTATE `23P01`) |
| 10:00-10:30 y 10:30-11:00 | Se permiten (turnos consecutivos) |
| Un turno `CANCELADO` a las 10:00 y otro `PENDIENTE` a las 10:00 | Se permiten (RN-03) |
| Mismo horario en dos profesionales distintos | Se permite |
| **Dos transacciones simultáneas** insertando el mismo horario | Exactamente una confirma y la otra falla con `23P01` |

> Si la base de prueba no tiene cargado `schema.sql` (o no tiene la extensión `btree_gist`), los tests fallan al preparar los datos: no es un error de la aplicación.

---

## B) Demostración manual con dos sesiones (para el video / la defensa)

Abrir **dos terminales** con `psql -U postgres -d turnofacil_test`.

**Preparación (una sola vez, en cualquier terminal):**
```sql
INSERT INTO usuarios (nombre, apellido, email, password, rol) VALUES
  ('Demo', 'Cliente', 'demo-cliente@example.com', 'x', 'CLIENTE'),
  ('Demo', 'Profesional', 'demo-pro@example.com', 'x', 'PROFESIONAL');
INSERT INTO especialidades (nombre) VALUES ('Demo');
INSERT INTO profesionales (usuario_id, especialidad_id, telefono)
  SELECT (SELECT id FROM usuarios WHERE email = 'demo-pro@example.com'),
         (SELECT id FROM especialidades WHERE nombre = 'Demo'), '000';
```

**Paso 1: Sesión A** (abre una transacción y NO confirma todavía):
```sql
BEGIN;
INSERT INTO turnos (usuario_id, profesional_id, fecha, hora, duracion_minutos)
  SELECT c.id, p.id, '2030-01-07', '10:00', 30
  FROM usuarios c, profesionales p
  WHERE c.email = 'demo-cliente@example.com'
    AND p.usuario_id = (SELECT id FROM usuarios WHERE email = 'demo-pro@example.com');
-- INSERT 0 1   (queda pendiente de COMMIT)
```

**Paso 2: Sesión B** (intenta reservar un horario que se pisa con el de A):
```sql
BEGIN;
INSERT INTO turnos (usuario_id, profesional_id, fecha, hora, duracion_minutos)
  SELECT c.id, p.id, '2030-01-07', '10:15', 30
  FROM usuarios c, profesionales p
  WHERE c.email = 'demo-cliente@example.com'
    AND p.usuario_id = (SELECT id FROM usuarios WHERE email = 'demo-pro@example.com');
-- La sesión B QUEDA ESPERANDO: la base no sabe todavía si A va a confirmar.
```

**Paso 3: Sesión A:**
```sql
COMMIT;
```

**Resultado en la sesión B:** el `INSERT` falla con un error como
```
ERROR:  conflicting key value violates exclusion constraint "no_solapamiento_turnos"
```
Entonces `ROLLBACK;` en B. Si en cambio A hiciera `ROLLBACK`, B terminaría insertando sin problema.

**Limpieza:**
```sql
DELETE FROM turnos WHERE usuario_id = (SELECT id FROM usuarios WHERE email = 'demo-cliente@example.com');
DELETE FROM profesionales WHERE usuario_id = (SELECT id FROM usuarios WHERE email = 'demo-pro@example.com');
DELETE FROM usuarios WHERE email IN ('demo-cliente@example.com', 'demo-pro@example.com');
DELETE FROM especialidades WHERE nombre = 'Demo';
```

---

## Cómo explicarlo en el informe y en la defensa

- El servicio valida RN-01 y RN-02 para devolver un mensaje claro al usuario (validación "amable").
- Pero entre la validación y el guardado hay una ventana: dos solicitudes simultáneas pueden pasar ambas la validación.
- Por eso la regla se **garantiza en la base de datos**: es la única forma de que sea correcta bajo concurrencia, sin depender de bloqueos en la aplicación.
- Cuando la base rechaza, `TurnoService` captura `DataIntegrityViolationException` y devuelve el mismo mensaje de solapamiento (409), no un error genérico.
