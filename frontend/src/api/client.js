const BASE = import.meta.env.VITE_API_URL || '/api';

export class ApiError extends Error {
  constructor(message, status) {
    super(message);
    this.status = status;
  }
}

// El backend devuelve el mensaje de error como texto plano. Cuando viene de
// ResponseStatusException llega como: 400 BAD_REQUEST "mensaje". Lo limpiamos.
function limpiarMensaje(data, status) {
  let msg = typeof data === 'string' ? data : data?.message || data?.error || '';
  const m = /^\d{3}\s+[A-Z_]+\s+"(.*)"$/s.exec(msg);
  if (m) msg = m[1];
  return msg || `Error inesperado (${status}).`;
}

export async function request(path, { method = 'GET', body } = {}) {
  let res;
  try {
    res = await fetch(`${BASE}${path}`, {
      method,
      headers: body ? { 'Content-Type': 'application/json' } : undefined,
      body: body ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new ApiError('No se pudo conectar con el servidor. Verificá que el backend esté corriendo.', 0);
  }
  const text = await res.text();
  let data = null;
  if (text) {
    try { data = JSON.parse(text); } catch { data = text; }
  }
  if (!res.ok) throw new ApiError(limpiarMensaje(data, res.status), res.status);
  return data;
}
