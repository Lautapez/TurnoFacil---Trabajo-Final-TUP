import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { profesionalesApi, turnosApi } from '../api/services.js';
import { useAuth } from '../context/AuthContext.jsx';
import { DIAS_CORTOS, bloquesDelDia, diaBackend, fechaLarga, isoLocal, nombreProfesional, proximosDias } from '../lib/fechas.js';
import { Alert, Cargando } from '../components/ui.jsx';

export default function Reservar() {
  const { id } = useParams();
  const { usuario } = useAuth();
  const navigate = useNavigate();
  const [pro, setPro] = useState(null);
  const [horarios, setHorarios] = useState(null);
  const [error, setError] = useState('');
  const [fecha, setFecha] = useState(null);
  const [hora, setHora] = useState('');
  const [duracion, setDuracion] = useState(30);
  const [ocupados, setOcupados] = useState([]); // horarios rechazados por el backend
  const [enviando, setEnviando] = useState(false);

  useEffect(() => {
    Promise.all([profesionalesApi.listar(), profesionalesApi.horarios(id)])
      .then(([lista, h]) => { setPro(lista.find((p) => String(p.id) === id) || null); setHorarios(h); })
      .catch((e) => setError(e.message));
  }, [id]);

  const dias = useMemo(() => proximosDias(14), []);
  const diasAtendidos = useMemo(() => new Set((horarios || []).map((h) => h.diaSemana)), [horarios]);
  const bloques = useMemo(
    () => (fecha && horarios ? bloquesDelDia(horarios, fecha, duracion) : []),
    [fecha, horarios, duracion]);

  const elegirFecha = (d) => { setFecha(d); setHora(''); setError(''); };

  const confirmar = async () => {
    setError(''); setEnviando(true);
    try {
      await turnosApi.crear({ clienteId: usuario.id, profesionalId: Number(id), fecha: isoLocal(fecha), hora, duracionMinutos: duracion });
      navigate('/mis-turnos', { state: { reservado: true } });
    } catch (err) {
      setError(err.message);
      if (err.status === 409 || err.status === 400) { setOcupados((o) => [...o, `${isoLocal(fecha)} ${hora}`]); setHora(''); }
    } finally { setEnviando(false); }
  };

  if (!horarios && !error) return <Cargando />;

  return (
    <div className="space-y-6">
      <Link to="/profesionales" className="text-sm font-medium text-tide-dark underline">Volver a profesionales</Link>
      <div>
        <h1 className="text-2xl font-bold">{pro ? nombreProfesional(pro) : 'Reservar turno'}</h1>
        {pro?.especialidad && <p className="text-sm text-muted">{pro.especialidad.nombre}</p>}
      </div>
      <Alert>{error}</Alert>

      {horarios?.length === 0 && (
        <Alert tone="error">Este profesional todavía no configuró sus días de atención.</Alert>
      )}

      {horarios?.length > 0 && (
        <>
          <section className="panel">
            <h2 className="mb-3 text-lg font-medium">1. Elegí el día</h2>
            <div className="flex gap-2 overflow-x-auto pb-1">
              {dias.map((d) => {
                const atiende = diasAtendidos.has(diaBackend(d));
                const activo = fecha && isoLocal(d) === isoLocal(fecha);
                return (
                  <button key={isoLocal(d)} disabled={!atiende} onClick={() => elegirFecha(d)} aria-pressed={activo}
                    className={`flex w-16 shrink-0 flex-col items-center rounded-lg border px-2 py-2 text-sm transition
                      ${activo ? 'border-tide bg-tide text-white' : 'border-line bg-white hover:border-tide'}
                      disabled:cursor-not-allowed disabled:border-dashed disabled:bg-mist disabled:text-muted/60 disabled:hover:border-line`}>
                    <span>{DIAS_CORTOS[diaBackend(d)]}</span>
                    <span className="font-display text-xl font-bold">{d.getDate()}</span>
                  </button>
                );
              })}
            </div>
          </section>

          {fecha && (
            <section className="panel space-y-4">
              <div className="flex flex-wrap items-end justify-between gap-3">
                <h2 className="text-lg font-medium">2. Elegí el horario · <span className="capitalize">{fechaLarga(isoLocal(fecha))}</span></h2>
                <div>
                  <label htmlFor="dur" className="label">Duración</label>
                  <select id="dur" className="input" value={duracion} onChange={(e) => { setDuracion(Number(e.target.value)); setHora(''); }}>
                    {[30, 60, 90].map((m) => <option key={m} value={m}>{m} minutos</option>)}
                  </select>
                </div>
              </div>
              {bloques.length === 0 && <p className="text-sm text-muted">No quedan horarios disponibles para este día.</p>}
              <div className="grid grid-cols-3 gap-2 sm:grid-cols-5 md:grid-cols-6">
                {bloques.map((b) => {
                  const tomado = ocupados.includes(`${isoLocal(fecha)} ${b}`);
                  return (
                    <button key={b} disabled={tomado} aria-pressed={hora === b} onClick={() => setHora(b)}
                      className={`rounded-lg border px-2 py-2 text-sm font-semibold transition
                        ${hora === b ? 'border-tide bg-tide text-white' : 'border-line bg-white hover:border-tide'}
                        disabled:cursor-not-allowed disabled:bg-mist disabled:text-muted/60 disabled:line-through`}>
                      {b}
                    </button>
                  );
                })}
              </div>
              {hora && (
                <div className="flex flex-wrap items-center justify-between gap-3 rounded-lg bg-tide-soft px-4 py-3">
                  <p className="text-sm"><strong>{hora} hs</strong>, {duracion} minutos, <span className="capitalize">{fechaLarga(isoLocal(fecha))}</span></p>
                  <button className="btn-primary" disabled={enviando} onClick={confirmar}>{enviando ? 'Reservando…' : 'Confirmar turno'}</button>
                </div>
              )}
            </section>
          )}
        </>
      )}
    </div>
  );
}
