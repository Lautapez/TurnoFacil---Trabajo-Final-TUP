# TurnoFácil · Frontend

React + Vite + Tailwind CSS (según el stack definido en el README principal). Consume la API REST del directorio `../backend`.

## Ejecutar

```bash
# 1. Backend (puerto 8080, PostgreSQL con la base "turnofacil")
cd backend && ./mvnw spring-boot:run

# 2. Frontend (puerto 5173)
cd frontend && npm install && npm run dev
```

En desarrollo, Vite redirige `/api` al backend (`vite.config.js`), por lo que no hace falta CORS.
En producción (Vercel), definí `VITE_API_URL=https://<tu-backend>.onrender.com/api` y agregá CORS en el backend.

## Estructura

```
src/
  api/        client.js (fetch + manejo de errores) · services.js (un método por endpoint)
  context/    AuthContext (sesión en localStorage)
  lib/        fechas.js (bloques horarios, RN-04, día 1=lunes … 7=domingo)
  components/ Layout · ui
  pages/      Login · Registro · Profesionales · Reservar · MisTurnos · Panel
```

## Pantallas y endpoints

| Pantalla | Rol | Endpoints |
|---|---|---|
| Login / Registro | todos | `POST /auth/login`, `POST /auth/register` |
| Profesionales | CLIENTE | `GET /professionals` |
| Reservar | CLIENTE | `GET /professionals/{id}/availability`, `POST /appointments` |
| Mis turnos | CLIENTE | `GET /appointments/user/{id}`, `PUT /appointments/{id}/cancel` |
| Panel (agenda) | PROFESIONAL / ADMINISTRADOR | `POST /professionals`, `GET`/`POST /professionals/availability` |
| Panel (hoy) | PROFESIONAL / ADMINISTRADOR | `GET /appointments/professional/{id}` *(pendiente en el backend)* |

## Reglas de negocio reflejadas en la UI

- **RN-01**: sólo se ofrecen días y bloques dentro de las franjas configuradas por el profesional.
- **RN-02**: si el backend rechaza el horario por solapamiento, el bloque se marca como ocupado.
- **RN-04**: cancelar/reprogramar sólo con estado PENDIENTE o CONFIRMADO y hasta 2 h antes (validado en la UI).
- **RN-05**: el CLIENTE no accede al panel; el profesional no usa las pantallas de cliente.

## Pendientes del backend detectados

1. **CORS**: no está configurado (resuelto en desarrollo con el proxy).
2. **JWT**: el login devuelve el usuario sin token y `SecurityConfig` permite todo; la sesión del frontend es sólo local.
3. **Turnos por profesional**: falta exponer `GET /api/appointments/professional/{id}` (el servicio ya lo tiene).
4. **Disponibilidad por fecha**: la API devuelve la agenda semanal, no los huecos libres; la UI genera bloques de 30 min y detecta ocupados al reservar.
5. **RN-04** no se valida en `cancelarTurno` (sólo en la UI); tampoco hay endpoints para confirmar/finalizar turnos, listar especialidades ni reprogramar.
6. `GET /professionals` serializa la entidad `Profesional` completa (incluye el hash de `usuario.password`); conviene devolver un DTO.
