# Guía de despliegue (Supabase + Render + Vercel)

Arquitectura definida en el README: **PostgreSQL en Supabase**, **backend en Render** y **frontend en Vercel**.

> Esta guía se escribió sin poder desplegar desde el entorno de desarrollo: los pasos y archivos (`backend/Dockerfile`, `frontend/vercel.json`) **no fueron probados en las plataformas**. Verificar cada paso contra los paneles actuales y anotar acá cualquier ajuste.

Orden recomendado: **1) base de datos → 2) backend → 3) frontend → 4) conectar CORS**.

## 1. Base de datos (Supabase)

1. Crear un proyecto en Supabase y anotar la contraseña de la base.
2. En *SQL Editor*, pegar y ejecutar **todo** `database/schema.sql` (incluye `CREATE EXTENSION btree_gist`; si Supabase pide habilitarla, hacerlo en *Database → Extensions*).
3. Copiar los datos de conexión (*Project Settings → Database*) y armar la URL JDBC:
   ```
   jdbc:postgresql://<host>:5432/postgres?sslmode=require
   ```
   - Usuario y contraseña: los del proyecto.
   - Si Render no logra conectarse a la conexión directa, usar la cadena del **pooler** (*Session mode*) que muestra el mismo panel; su usuario suele tener la forma `postgres.<ref-del-proyecto>`. Verificarlo en el panel.

## 2. Backend (Render)

1. *New → Web Service*, conectar el repositorio de GitHub.
2. **Root Directory:** `backend`. **Runtime:** `Docker` (usa `backend/Dockerfile`).
3. Variables de entorno (*Environment*):

   | Variable | Valor |
   |---|---|
   | `DB_URL` | La URL JDBC del paso 1 |
   | `DB_USERNAME` / `DB_PASSWORD` | Los de Supabase |
   | `JWT_SECRET` | Texto aleatorio de 32+ caracteres (`openssl rand -base64 48`) |
   | `ADMIN_EMAIL` / `ADMIN_PASSWORD` | Credenciales del primer administrador |
   | `CORS_ALLOWED_ORIGINS` | La URL del frontend (se completa en el paso 4) |

   Render define `PORT` solo; la aplicación ya lo respeta.
4. Desplegar y anotar la URL pública, por ejemplo `https://turnofacil-backend.onrender.com`.
5. Probar que responde (debe dar 401, no error de conexión):
   ```bash
   curl -i https://<tu-backend>.onrender.com/api/professionals
   ```

El plan gratuito de Render "duerme" el servicio por inactividad: la primera petición puede tardar ~1 minuto. Despertarlo **antes** de grabar el video o de la defensa.

## 3. Frontend (Vercel)

1. *Add New → Project*, importar el repositorio.
2. **Root Directory:** `frontend`. Framework: Vite (se detecta solo).
3. Variable de entorno:
   ```
   VITE_API_URL=https://<tu-backend>.onrender.com/api
   ```
   (debe terminar en `/api`).
4. Desplegar. `frontend/vercel.json` hace que las rutas de React Router (`/login`, `/admin`, etc.) funcionen al recargar la página.

## 4. Conectar CORS

Volver a Render y completar `CORS_ALLOWED_ORIGINS` con la URL exacta del frontend (sin barra final), por ejemplo `https://turnofacil.vercel.app`. Re-desplegar el backend.

## 5. Verificación final

1. Abrir la URL de Vercel e iniciar sesión con `ADMIN_EMAIL` / `ADMIN_PASSWORD`.
2. Seguir el flujo de primer uso del README (especialidad → profesional → franjas → reserva).
3. Pegar la URL del sistema en el README (sección *Entrega Final*).

## Problemas frecuentes

| Síntoma | Causa probable |
|---|---|
| El backend no arranca en Render y menciona `jwt.secret` | Falta `JWT_SECRET` |
| `Schema-validation: missing table` | No se ejecutó `schema.sql` en Supabase |
| Error de conexión / timeout a la base | URL o credenciales mal, falta `?sslmode=require`, o usar el pooler |
| En el navegador: error de CORS | `CORS_ALLOWED_ORIGINS` no coincide exactamente con la URL del frontend |
| Todas las llamadas del frontend dan 404 | `VITE_API_URL` sin `/api` al final |
| Recargar `/login` da 404 en Vercel | Falta `frontend/vercel.json` |
