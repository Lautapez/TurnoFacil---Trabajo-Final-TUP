import { useEffect, useState } from 'react';
import { adminApi, especialidadesApi, profesionalesApi } from '../api/services.js';
import { useAuth } from '../context/AuthContext.jsx';
import { fechaLarga, hhmm, isoLocal } from '../lib/fechas.js';
import { Alert, Campo, Cargando, EstadoBadge, Vacio } from '../components/ui.jsx';

const ROLES = ['CLIENTE', 'PROFESIONAL', 'ADMINISTRADOR'];
const TABS = [['resumen', 'Resumen'], ['usuarios', 'Usuarios'], ['profesionales', 'Profesionales'], ['turnos', 'Turnos'], ['especialidades', 'Especialidades']];

// Módulo administrativo. Es el único lugar donde se asignan los roles PROFESIONAL y ADMINISTRADOR.
export default function Admin() {
  const { usuario } = useAuth();
  const [tab, setTab] = useState('resumen');

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Administración</h1>
        <p className="text-sm text-muted">Hola, {usuario.nombre}. Gestioná usuarios, profesionales y turnos.</p>
      </div>
      <div className="flex flex-wrap gap-2" role="tablist">
        {TABS.map(([k, t]) => (
          <button key={k} role="tab" aria-selected={tab === k} onClick={() => setTab(k)} className={tab === k ? 'btn-primary' : 'btn-ghost'}>{t}</button>
        ))}
      </div>
      {tab === 'resumen' && <Resumen />}
      {tab === 'usuarios' && <Usuarios yo={usuario} />}
      {tab === 'profesionales' && <Profesionales />}
      {tab === 'turnos' && <Turnos />}
      {tab === 'especialidades' && <Especialidades />}
    </div>
  );
}

function Resumen() {
  const [d, setD] = useState(null);
  const [error, setError] = useState('');
  useEffect(() => { adminApi.dashboard().then(setD).catch((e) => setError(e.message)); }, []);

  if (error) return <Alert>{error}</Alert>;
  if (!d) return <Cargando />;

  const tarjeta = (t, v) => (
    <div key={t} className="panel"><p className="text-sm text-muted">{t}</p><p className="font-display text-3xl font-bold">{v}</p></div>
  );
  const estados = ['PENDIENTE', 'CONFIRMADO', 'CANCELADO', 'FINALIZADO'];

  return (
    <div className="space-y-6">
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        {tarjeta('Usuarios', d.totalUsuarios)}
        {tarjeta('Clientes', d.clientes)}
        {tarjeta('Profesionales activos', d.profesionalesActivos)}
        {tarjeta('Turnos hoy', d.turnosHoy)}
      </div>
      <div className="grid gap-4 md:grid-cols-2">
        <section className="panel">
          <h2 className="mb-3 text-lg font-medium">Turnos de hoy por estado</h2>
          <ul className="space-y-2 text-sm">{estados.map((e) => <li key={e} className="flex justify-between"><EstadoBadge estado={e} /><span className="font-semibold">{d.turnosHoyPorEstado[e]}</span></li>)}</ul>
        </section>
        <section className="panel">
          <h2 className="mb-3 text-lg font-medium">Turnos totales por estado</h2>
          <ul className="space-y-2 text-sm">{estados.map((e) => <li key={e} className="flex justify-between"><EstadoBadge estado={e} /><span className="font-semibold">{d.turnosPorEstado[e]}</span></li>)}</ul>
        </section>
      </div>
    </div>
  );
}

function Usuarios({ yo }) {
  const [lista, setLista] = useState(null);
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');

  const cargar = () => adminApi.usuarios().then(setLista).catch((e) => setError(e.message));
  useEffect(() => { cargar(); }, []);

  const ejecutar = async (fn, msg) => {
    setError(''); setOk('');
    try { await fn(); setOk(msg); await cargar(); } catch (e) { setError(e.message); }
  };

  if (!lista) return error ? <Alert>{error}</Alert> : <Cargando />;

  return (
    <div className="space-y-4">
      <Alert>{error}</Alert>
      <Alert tone="ok">{ok}</Alert>
      <div className="panel overflow-x-auto p-0">
        <table className="w-full text-left text-sm">
          <thead className="border-b border-line text-muted">
            <tr><th className="px-4 py-3">Usuario</th><th className="px-4 py-3">Email</th><th className="px-4 py-3">Rol</th><th className="px-4 py-3">Estado</th></tr>
          </thead>
          <tbody className="divide-y divide-line">
            {lista.map((u) => {
              const soyYo = u.id === yo.id;
              return (
                <tr key={u.id}>
                  <td className="px-4 py-3 font-medium">{u.nombre} {u.apellido}</td>
                  <td className="px-4 py-3">{u.email}</td>
                  <td className="px-4 py-3">
                    <select aria-label={`Rol de ${u.nombre}`} className="input !w-auto" disabled={soyYo} value={u.rol}
                      onChange={(e) => ejecutar(() => adminApi.cambiarRol(u.id, e.target.value), `Rol de ${u.nombre} actualizado a ${e.target.value}.`)}>
                      {ROLES.map((r) => <option key={r} value={r}>{r}</option>)}
                    </select>
                  </td>
                  <td className="px-4 py-3">
                    <button className={u.activo ? 'btn-danger' : 'btn-ghost'} disabled={soyYo}
                      onClick={() => ejecutar(() => adminApi.cambiarEstado(u.id, !u.activo), u.activo ? `${u.nombre} fue desactivado.` : `${u.nombre} fue reactivado.`)}>
                      {u.activo ? 'Desactivar' : 'Reactivar'}
                    </button>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
      <p className="text-xs text-muted">Para que un usuario atienda turnos: asignale el rol PROFESIONAL acá y después creá su perfil en la pestaña Profesionales.</p>
    </div>
  );
}

function Profesionales() {
  const [pros, setPros] = useState(null);
  const [usuarios, setUsuarios] = useState([]);
  const [esps, setEsps] = useState([]);
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');
  const [f, setF] = useState({ usuarioId: '', especialidadId: '', telefono: '' });
  const [enviando, setEnviando] = useState(false);

  const cargar = () => Promise.all([adminApi.profesionales(), adminApi.usuarios(), especialidadesApi.listar()])
    .then(([p, u, e]) => { setPros(p); setUsuarios(u); setEsps(e); })
    .catch((e) => setError(e.message));
  useEffect(() => { cargar(); }, []);

  if (!pros) return error ? <Alert>{error}</Alert> : <Cargando />;

  // Usuarios con rol PROFESIONAL que todavía no tienen perfil
  const conPerfil = new Set(pros.map((p) => p.usuarioId));
  const sinPerfil = usuarios.filter((u) => u.rol === 'PROFESIONAL' && !conPerfil.has(u.id));

  const crear = async (e) => {
    e.preventDefault(); setError(''); setOk(''); setEnviando(true);
    try {
      await profesionalesApi.crear({ usuarioId: Number(f.usuarioId), especialidadId: Number(f.especialidadId), telefono: f.telefono });
      setOk('Perfil profesional creado.'); setF({ usuarioId: '', especialidadId: '', telefono: '' }); await cargar();
    } catch (err) { setError(err.message); }
    finally { setEnviando(false); }
  };

  const cambiar = async (p) => {
    setError(''); setOk('');
    try { await (p.activo ? profesionalesApi.desactivar(p.id) : profesionalesApi.activar(p.id)); await cargar(); }
    catch (err) { setError(err.message); }
  };

  return (
    <div className="grid gap-6 md:grid-cols-2">
      <form onSubmit={crear} className="panel space-y-4 self-start">
        <h2 className="text-lg font-medium">Crear perfil profesional</h2>
        <Alert>{error}</Alert>
        <Alert tone="ok">{ok}</Alert>
        {sinPerfil.length === 0 ? (
          <p className="text-sm text-muted">No hay usuarios con rol PROFESIONAL pendientes de perfil. Asigná el rol desde la pestaña Usuarios.</p>
        ) : esps.length === 0 ? (
          <p className="text-sm text-muted">Primero creá al menos una especialidad en la pestaña Especialidades.</p>
        ) : (
          <>
            <Campo label="Usuario" id="usr">
              <select id="usr" required className="input" value={f.usuarioId} onChange={(e) => setF({ ...f, usuarioId: e.target.value })}>
                <option value="">Elegí un usuario</option>
                {sinPerfil.map((u) => <option key={u.id} value={u.id}>{u.nombre} {u.apellido} ({u.email})</option>)}
              </select>
            </Campo>
            <Campo label="Especialidad" id="esp">
              <select id="esp" required className="input" value={f.especialidadId} onChange={(e) => setF({ ...f, especialidadId: e.target.value })}>
                <option value="">Elegí una especialidad</option>
                {esps.map((s) => <option key={s.id} value={s.id}>{s.nombre}</option>)}
              </select>
            </Campo>
            <Campo label="Teléfono de contacto" id="tel">
              <input id="tel" required className="input" value={f.telefono} onChange={(e) => setF({ ...f, telefono: e.target.value })} />
            </Campo>
            <button className="btn-primary" disabled={enviando}>{enviando ? 'Guardando…' : 'Crear perfil'}</button>
          </>
        )}
      </form>

      <section className="panel">
        <h2 className="mb-3 text-lg font-medium">Profesionales</h2>
        {pros.length === 0 ? <p className="text-sm text-muted">Todavía no hay profesionales.</p> : (
          <ul className="divide-y divide-line">
            {pros.map((p) => (
              <li key={p.id} className="flex items-center justify-between gap-3 py-3">
                <div>
                  <p className="font-medium">{p.nombre} {p.apellido}</p>
                  <p className="text-sm text-muted">{p.especialidad} · {p.telefono} · {p.activo ? 'Activo' : 'Dado de baja'}</p>
                </div>
                <button className={p.activo ? 'btn-danger' : 'btn-ghost'} onClick={() => cambiar(p)}>{p.activo ? 'Dar de baja' : 'Reactivar'}</button>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}

function Turnos() {
  const [fecha, setFecha] = useState(isoLocal(new Date()));
  const [turnos, setTurnos] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    setTurnos(null); setError('');
    adminApi.turnos(fecha).then(setTurnos).catch((e) => setError(e.message));
  }, [fecha]);

  return (
    <div className="space-y-4">
      <div className="max-w-xs">
        <Campo label="Fecha" id="fecha">
          <input id="fecha" type="date" className="input" value={fecha} onChange={(e) => setFecha(e.target.value)} />
        </Campo>
      </div>
      <Alert>{error}</Alert>
      {!turnos && !error && <Cargando />}
      {turnos && turnos.length === 0 && <Vacio titulo="No hay turnos para esta fecha" />}
      {turnos && turnos.length > 0 && (
        <ul className="space-y-2">
          {turnos.map((t) => (
            <li key={t.id} className="panel flex flex-wrap items-center justify-between gap-3">
              <div>
                <p className="font-medium"><span className="capitalize">{fechaLarga(t.fecha)}</span> · {hhmm(t.hora)} hs · {t.clienteNombre}</p>
                <p className="text-sm text-muted">Profesional #{t.profesionalId} · {t.duracionMinutos} min</p>
              </div>
              <EstadoBadge estado={t.estado} />
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

function Especialidades() {
  const [lista, setLista] = useState(null);
  const [nombre, setNombre] = useState('');
  const [error, setError] = useState('');

  const cargar = () => especialidadesApi.listar().then(setLista).catch((e) => setError(e.message));
  useEffect(() => { cargar(); }, []);

  const crear = async (e) => {
    e.preventDefault(); setError('');
    try { await especialidadesApi.crear(nombre.trim()); setNombre(''); await cargar(); }
    catch (err) { setError(err.message); }
  };

  return (
    <div className="grid gap-6 md:grid-cols-2">
      <form onSubmit={crear} className="panel space-y-4 self-start">
        <h2 className="text-lg font-medium">Nueva especialidad</h2>
        <Alert>{error}</Alert>
        <Campo label="Nombre" id="nom"><input id="nom" required maxLength={100} className="input" value={nombre} onChange={(e) => setNombre(e.target.value)} /></Campo>
        <button className="btn-primary">Crear especialidad</button>
      </form>
      <section className="panel">
        <h2 className="mb-3 text-lg font-medium">Especialidades</h2>
        {!lista ? <Cargando /> : lista.length === 0 ? <p className="text-sm text-muted">Todavía no hay especialidades.</p> : (
          <ul className="divide-y divide-line">{lista.map((s) => <li key={s.id} className="py-2.5 text-sm">{s.nombre}</li>)}</ul>
        )}
      </section>
    </div>
  );
}
