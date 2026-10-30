import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './context/AuthContext.jsx';
import Layout from './components/Layout.jsx';
import Login from './pages/Login.jsx';
import Registro from './pages/Registro.jsx';
import Profesionales from './pages/Profesionales.jsx';
import Reservar from './pages/Reservar.jsx';
import MisTurnos from './pages/MisTurnos.jsx';
import Panel from './pages/Panel.jsx';

const esPro = (u) => u.rol === 'PROFESIONAL' || u.rol === 'ADMINISTRADOR';

// RN-05: el CLIENTE no accede al panel; el profesional no usa las pantallas de cliente.
function Guard({ rol, children }) {
  const { usuario } = useAuth();
  if (!usuario) return <Navigate to="/login" replace />;
  if (rol === 'CLIENTE' && esPro(usuario)) return <Navigate to="/panel" replace />;
  if (rol === 'PRO' && !esPro(usuario)) return <Navigate to="/profesionales" replace />;
  return children;
}

function Inicio() {
  const { usuario } = useAuth();
  if (!usuario) return <Navigate to="/login" replace />;
  return <Navigate to={esPro(usuario) ? '/panel' : '/profesionales'} replace />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/registro" element={<Registro />} />
      <Route path="/" element={<Inicio />} />
      <Route element={<Guard rol="AUTH"><Layout /></Guard>}>
        <Route path="/profesionales" element={<Guard rol="CLIENTE"><Profesionales /></Guard>} />
        <Route path="/profesionales/:id/reservar" element={<Guard rol="CLIENTE"><Reservar /></Guard>} />
        <Route path="/mis-turnos" element={<Guard rol="CLIENTE"><MisTurnos /></Guard>} />
        <Route path="/panel" element={<Guard rol="PRO"><Panel /></Guard>} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
