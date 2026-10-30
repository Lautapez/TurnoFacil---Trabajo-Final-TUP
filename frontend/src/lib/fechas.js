export const DIAS = ['', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo'];
export const DIAS_CORTOS = ['', 'lun', 'mar', 'mié', 'jue', 'vie', 'sáb', 'dom'];

export const hhmm = (t) => (t ? String(t).slice(0, 5) : '');
export const aMinutos = (t) => { const [h, m] = hhmm(t).split(':').map(Number); return h * 60 + m; };
export const deMinutos = (n) => `${String(Math.floor(n / 60)).padStart(2, '0')}:${String(n % 60).padStart(2, '0')}`;

// Fecha local en formato yyyy-MM-dd (evita el desfase de toISOString con UTC)
export const isoLocal = (d) =>
  `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;

// Backend: 1 = lunes ... 7 = domingo. JS: 0 = domingo.
export const diaBackend = (d) => (d.getDay() === 0 ? 7 : d.getDay());

export const parseFecha = (iso) => { const [y, m, d] = iso.split('-').map(Number); return new Date(y, m - 1, d); };

export const fechaLarga = (iso) =>
  parseFecha(iso).toLocaleDateString('es-AR', { weekday: 'long', day: 'numeric', month: 'long' });

export const proximosDias = (n = 14) =>
  Array.from({ length: n }, (_, i) => { const d = new Date(); d.setHours(0, 0, 0, 0); d.setDate(d.getDate() + i); return d; });

// Genera los bloques de inicio posibles para una fecha según los horarios semanales.
export function bloquesDelDia(horarios, fecha, duracion = 30) {
  const dia = diaBackend(fecha);
  const ahora = new Date();
  const esHoy = isoLocal(fecha) === isoLocal(ahora);
  const minAhora = ahora.getHours() * 60 + ahora.getMinutes();
  const out = new Set();
  horarios.filter((h) => h.diaSemana === dia).forEach((h) => {
    for (let t = aMinutos(h.horaInicio); t + duracion <= aMinutos(h.horaFin); t += 30) {
      if (!esHoy || t > minAhora) out.add(t);
    }
  });
  return [...out].sort((a, b) => a - b).map(deMinutos);
}

export const inicioTurno = (t) => new Date(`${t.fecha}T${hhmm(t.hora)}:00`);

// RN-04: sólo PENDIENTE/CONFIRMADO y hasta 2 horas antes del inicio.
export function puedeModificar(t) {
  if (t.estado !== 'PENDIENTE' && t.estado !== 'CONFIRMADO') return false;
  return inicioTurno(t).getTime() - Date.now() >= 2 * 60 * 60 * 1000;
}

export const nombreProfesional = (p) =>
  p?.usuario ? `${p.usuario.nombre} ${p.usuario.apellido}` : `Profesional #${p?.id}`;
