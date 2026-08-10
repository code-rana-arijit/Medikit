import { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { api, tokenStore } from '../lib/api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  const loadMe = useCallback(async () => {
    if (!tokenStore.access) {
      setUser(null);
      setLoading(false);
      return;
    }
    try {
      const me = await api.get('/users/me');
      setUser(me);
    } catch {
      setUser(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadMe();
    const onLogout = () => setUser(null);
    window.addEventListener('medikit:logout', onLogout);
    return () => window.removeEventListener('medikit:logout', onLogout);
  }, [loadMe]);

  const login = async (credentials) => {
    const data = await api.post('/auth/login', credentials, { auth: false });
    tokenStore.set(data.accessToken, data.refreshToken);
    await loadMe();
    return data;
  };

  const register = async (payload) => {
    const data = await api.post('/auth/register', payload, { auth: false });
    return data;
  };

  const logout = () => {
    try { api.post('/auth/logout', {}, { auth: false }).catch(() => {}); } catch { /* ignore */ }
    tokenStore.clear();
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout, loadMe }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
