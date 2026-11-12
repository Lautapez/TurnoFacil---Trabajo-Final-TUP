import { request } from './client.js';

// POST /api/auth/register | /api/auth/login (públicos)
export const authApi = {
  registrar: (dto) => request('/auth/register', { method: 'POST', body: dto }),
  login: (email, password) => request('/auth/login', { method: 'POST', body: { email, password } }),
};

// /api/specialties
export const especialidadesApi = {
  listar: () => request('/specialties'),
  crear: (nombre) => request('/specialties', { method: 'POST', body: { nombre } }),
};

// /api/professionals
export const profesionalesApi = {
  listar: (especialidadId) =>
    request(`/professionals${especialidadId ? `?especialidadId=${especialidadId}` : ''}`),
  miPerfil: () => request('/professionals/me'),
  crear: (dto) => request('/professionals', { method: 'POST', body: dto }), // solo ADMINISTRADOR
  desactivar: (id) => request(`/professionals/${id}/deactivate`, { method: 'PUT' }),
  activar: (id) => request(`/professionals/${id}/activate`, { method: 'PUT' }),
  horarios: (id) => request(`/professionals/${id}/availability`),
  // El profesional se identifica por el token: el body no lleva profesionalId
  guardarHorario: (dto) => request('/professionals/availability', { method: 'POST', body: dto }),
};

// /api/appointments
export const turnosApi = {
  // El cliente se identifica por el token: el body no lleva clienteId
  crear: (dto) => request('/appointments', { method: 'POST', body: dto }),
  porUsuario: (id) => request(`/appointments/user/${id}`),
  porProfesional: (id) => request(`/appointments/professional/${id}`),
  cancelar: (id) => request(`/appointments/${id}/cancel`, { method: 'PUT' }),
  reprogramar: (id, dto) => request(`/appointments/${id}/reschedule`, { method: 'PUT', body: dto }),
  confirmar: (id) => request(`/appointments/${id}/confirm`, { method: 'PUT' }),
  finalizar: (id) => request(`/appointments/${id}/finish`, { method: 'PUT' }),
};

// /api/admin (solo ADMINISTRADOR)
export const adminApi = {
  dashboard: () => request('/admin/dashboard'),
  usuarios: () => request('/admin/users'),
  cambiarRol: (id, rol) => request(`/admin/users/${id}/role`, { method: 'PUT', body: { rol } }),
  cambiarEstado: (id, activo) => request(`/admin/users/${id}/status`, { method: 'PUT', body: { activo } }),
  profesionales: () => request('/admin/professionals'),
  turnos: (fecha) => request(`/admin/appointments${fecha ? `?fecha=${fecha}` : ''}`),
};
