const BASE = import.meta.env.VITE_API_URL || '/api';

const TOKEN_KEY = 'turnofacil.token';

// Token JWT de la sesión. Se guarda al iniciar sesión y se envía en cada request.
export const getToken = () => localStorage.getItem(TOKEN_KEY);
export const setToken = (t) => (t ? localStorage.setItem(TOKEN_KEY, t) : localStorage.removeItem(TOKEN_KEY));

export class ApiError extends Error {
  constructor(message, status) {
    super(message);
    this.status = status;
  }
}

// El backend responde los errores como JSON { status, error, message }.
// Si llegara texto plano o formato ResponseStatusException, también se limpia.
function limpiarMensaje(data, status) {
  let msg = typeof data === 'string' ? data : data?.message || data?.error || '';
  const m = /^\d{3}\s+[A-Z_]+\s+"(.*)"$/s.exec(msg);
  if (m) msg = m[1];
  if (msg) return msg;
  if (status === 401) return 'Tu sesión expiró. Iniciá sesión nuevamente.';
  if (status === 403) return 'No tenés permisos para realizar esta acción.';
  return `Error inesperado (${status}).`;
}

// Sesión vencida o token inválido: se limpia y se vuelve al login.
function cerrarSesionExpirada() {
  setToken(null);
  localStorage.removeItem('turnofacil.usuario');
  if (window.location.pathname !== '/login') window.location.assign('/login');
}

export async function request(path, { method = 'GET', body } = {}) {
  const token = getToken();
  const headers = {};
  if (body) headers['Content-Type'] = 'application/json';
  if (token) headers['Authorization'] = `Bearer ${token}`;

  let res;
  try {
    res = await fetch(`${BASE}${path}`, {
      method,
      headers,
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
  // 401 con token enviado (fuera de /auth): la sesión ya no es válida
  if (res.status === 401 && token && !path.startsWith('/auth/')) cerrarSesionExpirada();
  if (!res.ok) throw new ApiError(limpiarMensaje(data, res.status), res.status);
  return data;
}
