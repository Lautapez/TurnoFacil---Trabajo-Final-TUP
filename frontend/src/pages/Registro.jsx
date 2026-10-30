import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { authApi } from '../api/services.js';
import { Alert, Campo } from '../components/ui.jsx';

export default function Registro() {
  const navigate = useNavigate();
  const [f, setF] = useState({ nombre: '', apellido: '', email: '', password: '', rol: 'CLIENTE' });
  const [error, setError] = useState('');
  const [enviando, setEnviando] = useState(false);
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  const enviar = async (e) => {
    e.preventDefault();
    setError(''); setEnviando(true);
    try { await authApi.registrar({ ...f, email: f.email.trim() }); navigate('/login', { state: { registrado: true } }); }
    catch (err) { setError(err.message); }
    finally { setEnviando(false); }
  };

  return (
    <div className="grid min-h-screen place-items-center px-4 py-8">
      <div className="w-full max-w-md">
        <h1 className="mb-6 text-center text-3xl font-bold">Crear cuenta</h1>
        <form onSubmit={enviar} className="panel space-y-4">
          <Alert>{error}</Alert>
          <div className="grid grid-cols-2 gap-3">
            <Campo label="Nombre" id="nombre"><input id="nombre" required className="input" value={f.nombre} onChange={set('nombre')} /></Campo>
            <Campo label="Apellido" id="apellido"><input id="apellido" required className="input" value={f.apellido} onChange={set('apellido')} /></Campo>
          </div>
          <Campo label="Correo electrónico" id="email"><input id="email" type="email" required className="input" value={f.email} onChange={set('email')} /></Campo>
          <Campo label="Contraseña (mínimo 6 caracteres)" id="password"><input id="password" type="password" minLength={6} required className="input" value={f.password} onChange={set('password')} /></Campo>
          <Campo label="Quiero usar TurnoFácil como" id="rol">
            <select id="rol" className="input" value={f.rol} onChange={set('rol')}>
              <option value="CLIENTE">Cliente / paciente</option>
              <option value="PROFESIONAL">Profesional</option>
            </select>
          </Campo>
          <button className="btn-primary w-full" disabled={enviando}>{enviando ? 'Creando cuenta…' : 'Crear cuenta'}</button>
        </form>
        <p className="mt-4 text-center text-sm text-muted">
          ¿Ya tenés cuenta? <Link to="/login" className="font-semibold text-tide-dark underline">Iniciá sesión</Link>
        </p>
      </div>
    </div>
  );
}
