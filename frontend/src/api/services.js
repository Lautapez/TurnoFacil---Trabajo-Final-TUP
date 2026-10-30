import { request } from './client.js';

// POST /api/auth/register | /api/auth/login
export const authApi = {
  registrar: (dto) => request('/auth/register', { method: 'POST', body: dto }),
  login: (email, password) => request('/auth/login', { method: 'POST', body: { email, password } }),
};

// /api/professionals
export const profesionalesApi = {
  listar: (especialidadId) =>
    request(`/professionals${especialidadId ? `?especialidadId=${especialidadId}` : ''}`),
  crear: (dto) => request('/professionals', { method: 'POST', body: dto }),
  horarios: (id) => request(`/professionals/${id}/availability`),
  guardarHorario: (dto) => request('/professionals/availability', { method: 'POST', body: dto }),
};

// /api/appointments
export const turnosApi = {
  crear: (dto) => request('/appointments', { method: 'POST', body: dto }),
  porUsuario: (id) => request(`/appointments/user/${id}`),
  cancelar: (id) => request(`/appointments/${id}/cancel`, { method: 'PUT' }),
  // El servicio ya tiene listarTurnosPorProfesional, pero el controlador aún no lo expone.
  porProfesional: (id) => request(`/appointments/professional/${id}`),
};
