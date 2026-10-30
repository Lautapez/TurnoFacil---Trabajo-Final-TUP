const ESTADOS = {
  PENDIENTE: ['Pendiente', 'bg-amber-100 text-amber-800'],
  CONFIRMADO: ['Confirmado', 'bg-emerald-100 text-emerald-800'],
  CANCELADO: ['Cancelado', 'bg-red-100 text-red-800'],
  FINALIZADO: ['Finalizado', 'bg-slate-200 text-slate-700'],
};

export function EstadoBadge({ estado }) {
  const [txt, cls] = ESTADOS[estado] || [estado, 'bg-slate-100 text-slate-700'];
  return <span className={`rounded-full px-2.5 py-0.5 text-xs font-semibold ${cls}`}>{txt}</span>;
}

export function Alert({ children, tone = 'error' }) {
  const cls = tone === 'error' ? 'border-red-200 bg-red-50 text-red-800' : 'border-emerald-200 bg-emerald-50 text-emerald-800';
  return children ? <div role="alert" className={`rounded-lg border px-4 py-3 text-sm ${cls}`}>{children}</div> : null;
}

export function Cargando({ texto = 'Cargando…' }) {
  return <p className="py-10 text-center text-sm text-muted" role="status">{texto}</p>;
}

export function Vacio({ titulo, children }) {
  return (
    <div className="panel py-10 text-center">
      <p className="font-display text-lg font-medium">{titulo}</p>
      <div className="mt-2 text-sm text-muted">{children}</div>
    </div>
  );
}

export function Campo({ label, id, children }) {
  return (
    <div>
      <label htmlFor={id} className="label">{label}</label>
      {children}
    </div>
  );
}

export function Avatar({ nombre }) {
  const ini = (nombre || '?').split(' ').map((s) => s[0]).slice(0, 2).join('').toUpperCase();
  return <div className="grid h-12 w-12 shrink-0 place-items-center rounded-full bg-tide-soft font-display font-bold text-tide-dark">{ini}</div>;
}
