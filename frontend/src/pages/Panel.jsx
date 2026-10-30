import { useEffect, useState } from 'react';
import { ApiError } from '../api/client.js';
import { profesionalesApi, turnosApi } from '../api/services.js';
import { useAuth } from '../context/AuthContext.jsx';
import { DIAS, hhmm, isoLocal } from '../lib/fechas.js';
import { Alert, Campo, Cargando, EstadoBadge, Vacio } from '../components/ui.jsx';

export default function Panel() {
  const { usuario } = useAuth();
  const [perfil, setPerfil] = useState(undefined); // undefined = cargando, null = sin perfil
  const [error, setError] = useState('');
  const [tab, setTab] = useState('hoy');

  const cargarPerfil = () =>
    profesionalesApi.listar()
      .then((l) => setPerfil(l.find((p) => p.usuario?.id === usuario.id) || null))
      .catch((e) => { setError(e.message); setPerfil(null); });
  useEffect(() => { cargarPerfil(); }, []); // eslint-disable-line

  if (perfil === undefined) return <Cargando />;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Hola, {usuario.nombre}</h1>
        <p className="text-sm text-muted">{perfil ? `${perfil.especialidad?.nombre || 'Profesional'} · ${perfil.telefono}` : 'Completá tu perfil para empezar a recibir turnos.'}</p>
      </div>
      <Alert>{error}</Alert>
      {!perfil ? <CrearPerfil usuarioId={usuario.id} onListo={cargarPerfil} /> : (
        <>
          <div className="flex gap-2" role="tablist">
            {[['hoy', 'Turnos de hoy'], ['agenda', 'Mi agenda semanal']].map(([k, t]) => (
              <button key={k} role="tab" aria-selected={tab === k} onClick={() => setTab(k)} className={tab === k ? 'btn-primary' : 'btn-ghost'}>{t}</button>
            ))}
          </div>
          {tab === 'hoy' ? <Dashboard profesionalId={perfil.id} /> : <Agenda profesionalId={perfil.id} />}
        </>
      )}
    </div>
  );
}

function CrearPerfil({ usuarioId, onListo }) {
  const [f, setF] = useState({ especialidadId: '', telefono: '' });
  const [error, setError] = useState('');
  const [enviando, setEnviando] = useState(false);

  const enviar = async (e) => {
    e.preventDefault(); setError(''); setEnviando(true);
    try { await profesionalesApi.crear({ usuarioId, especialidadId: Number(f.especialidadId), telefono: f.telefono }); await onListo(); }
    catch (err) { setError(err.message); }
    finally { setEnviando(false); }
  };

  return (
    <form onSubmit={enviar} className="panel max-w-md space-y-4">
      <h2 className="text-lg font-medium">Tu perfil profesional</h2>
      <Alert>{error}</Alert>
      <Campo label="ID de especialidad" id="esp">
        <input id="esp" type="number" min="1" required className="input" value={f.especialidadId} onChange={(e) => setF({ ...f, especialidadId: e.target.value })} />
        <p className="mt-1 text-xs text-muted">Es el ID de la tabla especialidades de la base de datos.</p>
      </Campo>
      <Campo label="Teléfono de contacto" id="tel">
        <input id="tel" required className="input" value={f.telefono} onChange={(e) => setF({ ...f, telefono: e.target.value })} />
      </Campo>
      <button className="btn-primary" disabled={enviando}>{enviando ? 'Guardando…' : 'Crear perfil'}</button>
    </form>
  );
}

function Dashboard({ profesionalId }) {
  const [turnos, setTurnos] = useState(null);
  const [pendienteApi, setPendienteApi] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    turnosApi.porProfesional(profesionalId).then(setTurnos).catch((e) => {
      if (e instanceof ApiError && [404, 405].includes(e.status)) setPendienteApi(true); else setError(e.message);
    });
  }, [profesionalId]);

  if (pendienteApi) {
    return (
      <Vacio titulo="El listado de turnos del profesional todavía no está disponible">
        El servicio ya existe en el backend, pero el controlador no expone <code>GET /api/appointments/professional/{'{id}'}</code>. Al agregarlo, esta pantalla se completa sola.
      </Vacio>
    );
  }
  if (error) return <Alert>{error}</Alert>;
  if (!turnos) return <Cargando />;

  const hoy = turnos.filter((t) => t.fecha === isoLocal(new Date())).sort((a, b) => a.hora.localeCompare(b.hora));
  const cuenta = (e) => hoy.filter((t) => t.estado === e).length;
  const tarjetas = [['Pendientes', 'PENDIENTE', 'border-amber-300'], ['Confirmados', 'CONFIRMADO', 'border-emerald-300'], ['Cancelados', 'CANCELADO', 'border-red-300']];

  return (
    <div className="space-y-4">
      <div className="grid gap-3 sm:grid-cols-3">
        {tarjetas.map(([t, e, c]) => (
          <div key={e} className={`panel border-l-4 ${c}`}>
            <p className="text-sm text-muted">{t}</p>
            <p className="font-display text-3xl font-bold">{cuenta(e)}</p>
          </div>
        ))}
      </div>
      {hoy.length === 0 ? <Vacio titulo="No tenés turnos para hoy">Cuando alguien reserve, va a aparecer acá.</Vacio> : (
        <ul className="space-y-2">
          {hoy.map((t) => (
            <li key={t.id} className="panel flex items-center justify-between gap-3">
              <div>
                <p className="font-medium">{hhmm(t.hora)} hs · {t.clienteNombre}</p>
                <p className="text-sm text-muted">{t.duracionMinutos} minutos</p>
              </div>
              <EstadoBadge estado={t.estado} />
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

function Agenda({ profesionalId }) {
  const [horarios, setHorarios] = useState(null);
  const [f, setF] = useState({ diaSemana: 1, horaInicio: '09:00', horaFin: '13:00' });
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');
  const [enviando, setEnviando] = useState(false);

  const cargar = () => profesionalesApi.horarios(profesionalId).then(setHorarios).catch((e) => setError(e.message));
  useEffect(() => { cargar(); }, [profesionalId]); // eslint-disable-line

  const agregar = async (e) => {
    e.preventDefault(); setError(''); setOk('');
    if (f.horaFin <= f.horaInicio) return setError('La hora de fin debe ser posterior a la hora de inicio.');
    setEnviando(true);
    try {
      await profesionalesApi.guardarHorario({ profesionalId, diaSemana: Number(f.diaSemana), horaInicio: f.horaInicio, horaFin: f.horaFin });
      setOk('Franja guardada. Ya está disponible para los clientes.'); await cargar();
    } catch (err) { setError(err.message); }
    finally { setEnviando(false); }
  };

  const porDia = (d) => (horarios || []).filter((h) => h.diaSemana === d).sort((a, b) => a.horaInicio.localeCompare(b.horaInicio));

  return (
    <div className="grid gap-6 md:grid-cols-2">
      <form onSubmit={agregar} className="panel space-y-4 self-start">
        <h2 className="text-lg font-medium">Agregar franja de atención</h2>
        <Alert>{error}</Alert>
        <Alert tone="ok">{ok}</Alert>
        <Campo label="Día" id="dia">
          <select id="dia" className="input" value={f.diaSemana} onChange={(e) => setF({ ...f, diaSemana: e.target.value })}>
            {DIAS.slice(1).map((d, i) => <option key={d} value={i + 1}>{d}</option>)}
          </select>
        </Campo>
        <div className="grid grid-cols-2 gap-3">
          <Campo label="Desde" id="ini"><input id="ini" type="time" required className="input" value={f.horaInicio} onChange={(e) => setF({ ...f, horaInicio: e.target.value })} /></Campo>
          <Campo label="Hasta" id="fin"><input id="fin" type="time" required className="input" value={f.horaFin} onChange={(e) => setF({ ...f, horaFin: e.target.value })} /></Campo>
        </div>
        <button className="btn-primary" disabled={enviando}>{enviando ? 'Guardando…' : 'Guardar franja'}</button>
      </form>

      <section className="panel">
        <h2 className="mb-3 text-lg font-medium">Tu semana</h2>
        {!horarios ? <Cargando /> : (
          <ul className="divide-y divide-line">
            {DIAS.slice(1).map((d, i) => (
              <li key={d} className="flex items-center justify-between gap-3 py-2.5 text-sm">
                <span className="font-medium">{d}</span>
                <span className={porDia(i + 1).length ? '' : 'text-muted'}>
                  {porDia(i + 1).length ? porDia(i + 1).map((h) => `${hhmm(h.horaInicio)} a ${hhmm(h.horaFin)}`).join(' y ') : 'No atiende'}
                </span>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
