import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';

export default function Layout() {
  const { usuario, logout } = useAuth();
  const navigate = useNavigate();
  const esPro = usuario?.rol === 'PROFESIONAL' || usuario?.rol === 'ADMINISTRADOR';

  const links = esPro
    ? [['/panel', 'Panel']]
    : [['/profesionales', 'Profesionales'], ['/mis-turnos', 'Mis turnos']];

  const cls = ({ isActive }) =>
    `rounded-md px-3 py-1.5 text-sm font-medium ${isActive ? 'bg-tide-soft text-tide-dark' : 'text-muted hover:text-ink'}`;

  return (
    <div className="min-h-screen">
      <header className="border-b border-line bg-white">
        <div className="mx-auto flex max-w-5xl flex-wrap items-center justify-between gap-3 px-4 py-3">
          <NavLink to="/" className="font-display text-xl font-bold text-ink">
            Turno<span className="text-tide">Fácil</span>
          </NavLink>
          <nav className="flex items-center gap-1" aria-label="Principal">
            {links.map(([to, txt]) => <NavLink key={to} to={to} className={cls}>{txt}</NavLink>)}
          </nav>
          <div className="flex items-center gap-3 text-sm">
            <span className="hidden text-muted sm:inline">{usuario.nombre} {usuario.apellido}</span>
            <button className="btn-ghost" onClick={() => { logout(); navigate('/login'); }}>Cerrar sesión</button>
          </div>
        </div>
      </header>
      <main className="mx-auto max-w-5xl px-4 py-8"><Outlet /></main>
    </div>
  );
}
