import { createContext, useContext, useState, useCallback } from 'react';
import { authApi } from '../api/services.js';
import { setToken } from '../api/client.js';

const KEY = 'turnofacil.usuario';
const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [usuario, setUsuario] = useState(() => {
    try { return JSON.parse(localStorage.getItem(KEY)); } catch { return null; }
  });

  const guardar = (u) => { setUsuario(u); u ? localStorage.setItem(KEY, JSON.stringify(u)) : localStorage.removeItem(KEY); };

  const login = useCallback(async (email, password) => {
    const { token, ...u } = await authApi.login(email, password);
    setToken(token); // el JWT se guarda aparte y client.js lo envía como Authorization: Bearer
    guardar(u);
    return u;
  }, []);

  const logout = useCallback(() => { setToken(null); guardar(null); }, []);

  return <AuthContext.Provider value={{ usuario, login, logout }}>{children}</AuthContext.Provider>;
}

export const useAuth = () => useContext(AuthContext);
