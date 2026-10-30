import { useEffect, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { turnosApi } from '../api/services.js';
import { useAuth } from '../context/AuthContext.jsx';
import { fechaLarga, hhmm, inicioTurno, puedeModificar } from '../lib/fechas.js';
import { Alert, Cargando, EstadoBadge, Vacio } from '../components/ui.jsx';

export default function MisTurnos() {
  const { usuario } = useAuth();
  const { state } = useLocation();
  const [turnos, setTurnos] = useState(null);
  const [error, setError] = useState('');
  const [vista, setVista] = useState('proximos');
  const [aCancelar, setACancelar] = useState(null);
  const [cancelando, setCancelando] = useState(false);

  const cargar = () => turnosApi.porUsuario(usuario.id).then(setTurnos).catch((e) => setError(e.message));
  useEffect(() => { cargar(); }, []); // eslint-disable-line

  const cancelar = async () => {
    setCancelando(true); setError('');
    try { await turnosApi.cancelar(aCancelar.id); setACancelar(null); await cargar(); }
    catch (e) { setError(e.message); setACancelar(null); }
    finally { setCancelando(false); }
  };

  const ahora = Date.now();
  const proximos = (turnos || []).filter((t) => t.estado !== 'CANCELADO' && t.estado !== 'FINALIZADO' && inicioTurno(t) >= ahora)
    .sort((a, b) => inicioTurno(a) - inicioTurno(b));
  const historial = (turnos || []).filter((t) => !proximos.includes(t)).sort((a, b) => inicioTurno(b) - inicioTurno(a));
  const lista = vista === 'proximos' ? proximos : historial;

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Mis turnos</h1>
      <Alert tone="ok">{state?.reservado && 'Turno reservado. Queda pendiente de confirmación.'}</Alert>
      <Alert>{error}</Alert>
      <div className="flex gap-2" role="tablist">
        {[['proximos', `Próximos (${proximos.length})`], ['historial', 'Historial']].map(([k, t]) => (
          <button key={k} role="tab" aria-selected={vista === k} onClick={() => setVista(k)}
            className={vista === k ? 'btn-primary' : 'btn-ghost'}>{t}</button>
        ))}
      </div>
      {!turnos && !error && <Cargando />}
      {turnos && lista.length === 0 && (
        <Vacio titulo={vista === 'proximos' ? 'No tenés turnos próximos' : 'Todavía no hay historial'}>
          <Link to="/profesionales" className="font-semibold text-tide-dark underline">Reservar un turno</Link>
        </Vacio>
      )}
      <ul className="space-y-3">
        {lista.map((t) => {
          const editable = puedeModificar(t);
          return (
            <li key={t.id} className="panel flex flex-wrap items-center justify-between gap-3">
              <div>
                <p className="font-display text-lg font-medium capitalize">{fechaLarga(t.fecha)} · {hhmm(t.hora)} hs</p>
                <p className="text-sm text-muted">Profesional #{t.profesionalId} · {t.duracionMinutos} min</p>
              </div>
              <div className="flex flex-wrap items-center gap-2">
                <EstadoBadge estado={t.estado} />
                {(t.estado === 'PENDIENTE' || t.estado === 'CONFIRMADO') && (
                  editable ? (
                    <>
                      <Link to={`/profesionales/${t.profesionalId}/reservar`} className="btn-ghost">Reprogramar</Link>
                      <button className="btn-danger" onClick={() => setACancelar(t)}>Cancelar</button>
                    </>
                  ) : <span className="text-xs text-muted">Ya no se puede modificar (falta menos de 2 h)</span>
                )}
              </div>
            </li>
          );
        })}
      </ul>
      {turnos && proximos.some(puedeModificar) && (
        <p className="text-xs text-muted">Para reprogramar, reservá el nuevo horario y luego cancelá el turno anterior. Se puede cancelar hasta 2 horas antes.</p>
      )}

      {aCancelar && (
        <div className="fixed inset-0 z-10 grid place-items-center bg-ink/50 px-4" role="dialog" aria-modal="true" aria-labelledby="t-cancel">
          <div className="panel w-full max-w-sm space-y-4">
            <h2 id="t-cancel" className="text-lg font-bold">¿Cancelar este turno?</h2>
            <p className="text-sm text-muted capitalize">{fechaLarga(aCancelar.fecha)} · {hhmm(aCancelar.hora)} hs. El horario quedará libre para otras personas.</p>
            <div className="flex justify-end gap-2">
              <button className="btn-ghost" onClick={() => setACancelar(null)}>Mantener turno</button>
              <button className="btn-danger" disabled={cancelando} onClick={cancelar}>{cancelando ? 'Cancelando…' : 'Cancelar turno'}</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
