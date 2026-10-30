import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { profesionalesApi } from '../api/services.js';
import { nombreProfesional } from '../lib/fechas.js';
import { Alert, Avatar, Cargando, Vacio } from '../components/ui.jsx';

export default function Profesionales() {
  const [lista, setLista] = useState(null);
  const [error, setError] = useState('');
  const [esp, setEsp] = useState('');
  const [q, setQ] = useState('');

  useEffect(() => { profesionalesApi.listar().then(setLista).catch((e) => setError(e.message)); }, []);

  // No hay endpoint de especialidades: se derivan del listado de profesionales.
  const especialidades = useMemo(() => {
    const m = new Map();
    (lista || []).forEach((p) => p.especialidad && m.set(p.especialidad.id, p.especialidad.nombre));
    return [...m].map(([id, nombre]) => ({ id, nombre }));
  }, [lista]);

  const visibles = (lista || []).filter((p) =>
    (!esp || String(p.especialidad?.id) === esp) &&
    nombreProfesional(p).toLowerCase().includes(q.toLowerCase()));

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Elegí un profesional</h1>
        <p className="text-sm text-muted">Filtrá por especialidad y reservá en el horario que te quede cómodo.</p>
      </div>
      <Alert>{error}</Alert>
      <div className="flex flex-col gap-3 sm:flex-row">
        <input className="input sm:max-w-xs" placeholder="Buscar por nombre" aria-label="Buscar por nombre" value={q} onChange={(e) => setQ(e.target.value)} />
        <select className="input sm:max-w-xs" aria-label="Especialidad" value={esp} onChange={(e) => setEsp(e.target.value)}>
          <option value="">Todas las especialidades</option>
          {especialidades.map((s) => <option key={s.id} value={s.id}>{s.nombre}</option>)}
        </select>
      </div>
      {!lista && !error && <Cargando />}
      {lista && visibles.length === 0 && <Vacio titulo="No encontramos profesionales">Probá con otra especialidad o quitá el filtro.</Vacio>}
      <ul className="grid gap-4 sm:grid-cols-2">
        {visibles.map((p) => (
          <li key={p.id} className="panel flex items-center gap-4">
            <Avatar nombre={nombreProfesional(p)} />
            <div className="min-w-0 flex-1">
              <p className="truncate font-display text-lg font-medium">{nombreProfesional(p)}</p>
              <p className="text-sm text-muted">{p.especialidad?.nombre || 'Sin especialidad'} · {p.telefono}</p>
            </div>
            <Link to={`/profesionales/${p.id}/reservar`} className="btn-primary">Reservar</Link>
          </li>
        ))}
      </ul>
    </div>
  );
}
