import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './context/AuthContext.jsx';
import Layout from './components/Layout.jsx';
import Login from './pages/Login.jsx';
import Registro from './pages/Registro.jsx';
import Profesionales from './pages/Profesionales.jsx';
import Reservar from './pages/Reservar.jsx';
import MisTurnos from './pages/MisTurnos.jsx';
import Panel from './pages/Panel.jsx';
import Admin from './pages/Admin.jsx';

// Pantalla de inicio de cada rol
const inicioDe = (u) => (u.rol === 'ADMINISTRADOR' ? '/admin' : u.rol === 'PROFESIONAL' ? '/panel' : '/profesionales');

// Cada ruta declara qué roles pueden verla. El backend vuelve a validarlo con @PreAuthorize:
// esto solo evita mostrar pantallas que van a fallar con 403.
function Guard({ roles, children }) {
  const { usuario } = useAuth();
  if (!usuario) return <Navigate to="/login" replace />;
  if (roles && !roles.includes(usuario.rol)) return <Navigate to={inicioDe(usuario)} replace />;
  return children;
}

function Inicio() {
  const { usuario } = useAuth();
  if (!usuario) return <Navigate to="/login" replace />;
  return <Navigate to={inicioDe(usuario)} replace />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/registro" element={<Registro />} />
      <Route path="/" element={<Inicio />} />
      <Route element={<Guard><Layout /></Guard>}>
        <Route path="/profesionales" element={<Guard roles={['CLIENTE']}><Profesionales /></Guard>} />
        <Route path="/profesionales/:id/reservar" element={<Guard roles={['CLIENTE']}><Reservar /></Guard>} />
        <Route path="/mis-turnos" element={<Guard roles={['CLIENTE']}><MisTurnos /></Guard>} />
        <Route path="/panel" element={<Guard roles={['PROFESIONAL']}><Panel /></Guard>} />
        <Route path="/admin" element={<Guard roles={['ADMINISTRADOR']}><Admin /></Guard>} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
