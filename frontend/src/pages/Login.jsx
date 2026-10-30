import { useState } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import { Alert, Campo } from '../components/ui.jsx';

export default function Login() {
  const { usuario, login } = useAuth();
  const navigate = useNavigate();
  const { state } = useLocation();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [enviando, setEnviando] = useState(false);

  if (usuario) return <Navigate to="/" replace />;

  const enviar = async (e) => {
    e.preventDefault();
    setError(''); setEnviando(true);
    try { await login(email.trim(), password); navigate('/'); }
    catch (err) { setError(err.message); }
    finally { setEnviando(false); }
  };

  return (
    <div className="grid min-h-screen place-items-center px-4">
      <div className="w-full max-w-md">
        <h1 className="mb-1 text-center text-3xl font-bold">Turno<span className="text-tide">Fácil</span></h1>
        <p className="mb-6 text-center text-sm text-muted">Reservá y gestioná tus turnos sin vueltas.</p>
        <form onSubmit={enviar} className="panel space-y-4">
          <Alert tone="ok">{state?.registrado && 'Cuenta creada. Ya podés iniciar sesión.'}</Alert>
          <Alert>{error}</Alert>
          <Campo label="Correo electrónico" id="email">
            <input id="email" type="email" required autoComplete="email" className="input" value={email} onChange={(e) => setEmail(e.target.value)} />
          </Campo>
          <Campo label="Contraseña" id="password">
            <input id="password" type="password" required autoComplete="current-password" className="input" value={password} onChange={(e) => setPassword(e.target.value)} />
          </Campo>
          <button className="btn-primary w-full" disabled={enviando}>{enviando ? 'Ingresando…' : 'Iniciar sesión'}</button>
        </form>
        <p className="mt-4 text-center text-sm text-muted">
          ¿No tenés cuenta? <Link to="/registro" className="font-semibold text-tide-dark underline">Registrate</Link>
        </p>
      </div>
    </div>
  );
}
