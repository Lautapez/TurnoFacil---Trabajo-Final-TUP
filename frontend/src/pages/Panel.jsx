import { useEffect, useState } from 'react';
import { ApiError } from '../api/client.js';
import { profesionalesApi, turnosApi } from '../api/services.js';
import { useAuth } from '../context/AuthContext.jsx';
import { DIAS, fechaLarga, hhmm, inicioTurno, isoLocal } from '../lib/fechas.js';
import { Alert, Campo, Cargando, EstadoBadge, Vacio } from '../components/ui.jsx';

// Panel del PROFESIONAL. El perfil lo crea un administrador (el registro público solo crea clientes).
export default function Panel() {
  const { usuario } = useAuth();
  const [perfil, setPerfil] = useState(undefined); // undefined = cargando, null = sin perfil
  const [error, setError] = useState('');
  const [tab, setTab] = useState('hoy');

  useEffect(() => {
    profesionalesApi.miPerfil()
      .then(setPerfil)
      .catch((e) => {
        if (!(e instanceof ApiError && e.status === 404)) setError(e.message);
        setPerfil(null);
      });
  }, []);

  if (perfil === undefined) return <Cargando />;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Hola, {usuario.nombre}</h1>
        <p className="text-sm text-muted">
          {perfil ? `${perfil.especialidad || 'Profesional'} · ${perfil.telefono}` : 'Todavía no tenés un perfil profesional.'}
        </p>
      </div>
      <Alert>{error}</Alert>
      {!perfil ? (
        <Vacio titulo="Tu perfil profesional todavía no fue creado">
          Un administrador debe crear tu perfil (especialidad y teléfono). Cuando lo haga, vas a poder cargar tu agenda y recibir turnos.
        </Vacio>
      ) : (
        <>
          {!perfil.activo && <Alert>Tu perfil está dado de baja: no recibís turnos nuevos ni podés modificar tu agenda.</Alert>}
          <div className="flex flex-wrap gap-2" role="tablist">
            {[['hoy', 'Turnos de hoy'], ['proximos', 'Próximos turnos'], ['agenda', 'Mi agenda semanal']].map(([k, t]) => (
              <button key={k} role="tab" aria-selected={tab === k} onClick={() => setTab(k)} className={tab === k ? 'btn-primary' : 'btn-ghost'}>{t}</button>
            ))}
          </div>
          {tab === 'agenda' ? <Agenda activo={perfil.activo} /> : <Turnos profesionalId={perfil.id} modo={tab} />}
        </>
      )}
    </div>
  );
}

function Turnos({ profesionalId, modo }) {
  const [turnos, setTurnos] = useState(null);
  const [error, setError] = useState('');
  const [trabajando, setTrabajando] = useState(null);

  const cargar = () => turnosApi.porProfesional(profesionalId).then(setTurnos).catch((e) => setError(e.message));
  useEffect(() => { setTurnos(null); cargar(); }, [profesionalId, modo]); // eslint-disable-line

  const accion = async (fn, t) => {
    setError(''); setTrabajando(t.id);
    try { await fn(t.id); await cargar(); }
    catch (e) { setError(e.message); }
    finally { setTrabajando(null); }
  };

  if (error && !turnos) return <Alert>{error}</Alert>;
  if (!turnos) return <Cargando />;

  const hoyIso = isoLocal(new Date());
  const hoy = turnos.filter((t) => t.fecha === hoyIso).sort((a, b) => a.hora.localeCompare(b.hora));
  const proximos = turnos
    .filter((t) => t.fecha > hoyIso && t.estado !== 'CANCELADO')
    .sort((a, b) => inicioTurno(a) - inicioTurno(b));
  const lista = modo === 'hoy' ? hoy : proximos;

  const cuenta = (e) => hoy.filter((t) => t.estado === e).length;
  const tarjetas = [['Pendientes', 'PENDIENTE', 'border-amber-300'], ['Confirmados', 'CONFIRMADO', 'border-emerald-300'], ['Cancelados', 'CANCELADO', 'border-red-300']];

  return (
    <div className="space-y-4">
      <Alert>{error}</Alert>
      {modo === 'hoy' && (
        <div className="grid gap-3 sm:grid-cols-3">
          {tarjetas.map(([t, e, c]) => (
            <div key={e} className={`panel border-l-4 ${c}`}>
              <p className="text-sm text-muted">{t}</p>
              <p className="font-display text-3xl font-bold">{cuenta(e)}</p>
            </div>
          ))}
        </div>
      )}
      {lista.length === 0 ? (
        <Vacio titulo={modo === 'hoy' ? 'No tenés turnos para hoy' : 'No tenés turnos próximos'}>Cuando alguien reserve, va a aparecer acá.</Vacio>
      ) : (
        <ul className="space-y-2">
          {lista.map((t) => (
            <li key={t.id} className="panel flex flex-wrap items-center justify-between gap-3">
              <div>
                <p className="font-medium">
                  {modo === 'proximos' && <span className="capitalize">{fechaLarga(t.fecha)} · </span>}
                  {hhmm(t.hora)} hs · {t.clienteNombre}
                </p>
                <p className="text-sm text-muted">{t.duracionMinutos} minutos</p>
              </div>
              <div className="flex flex-wrap items-center gap-2">
                <EstadoBadge estado={t.estado} />
                {t.estado === 'PENDIENTE' && (
                  <button className="btn-primary" disabled={trabajando === t.id} onClick={() => accion(turnosApi.confirmar, t)}>Confirmar</button>
                )}
                {t.estado === 'CONFIRMADO' && (
                  <button className="btn-ghost" disabled={trabajando === t.id} onClick={() => accion(turnosApi.finalizar, t)}>Finalizar</button>
                )}
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

function Agenda({ activo }) {
  const [horarios, setHorarios] = useState(null);
  const [perfilId, setPerfilId] = useState(null);
  const [f, setF] = useState({ diaSemana: 1, horaInicio: '09:00', horaFin: '13:00' });
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');
  const [enviando, setEnviando] = useState(false);

  const cargar = (id) => profesionalesApi.horarios(id).then(setHorarios).catch((e) => setError(e.message));
  useEffect(() => {
    profesionalesApi.miPerfil().then((p) => { setPerfilId(p.id); return cargar(p.id); }).catch((e) => setError(e.message));
  }, []); // eslint-disable-line

  const agregar = async (e) => {
    e.preventDefault(); setError(''); setOk('');
    if (f.horaFin <= f.horaInicio) return setError('La hora de fin debe ser posterior a la hora de inicio.');
    setEnviando(true);
    try {
      // El backend toma el profesional del token: no se envía profesionalId
      await profesionalesApi.guardarHorario({ diaSemana: Number(f.diaSemana), horaInicio: f.horaInicio, horaFin: f.horaFin });
      setOk('Franja guardada. Ya está disponible para los clientes.'); await cargar(perfilId);
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
        <button className="btn-primary" disabled={enviando || !activo}>{enviando ? 'Guardando…' : 'Guardar franja'}</button>
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
