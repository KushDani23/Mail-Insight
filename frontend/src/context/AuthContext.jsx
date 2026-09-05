import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { authApi } from '../api/client';

const AuthContext = createContext(null);

/**
 * AuthProvider – wraps the entire app.
 * Exposes: user, loading, login, logout
 */
export function AuthProvider({ children }) {
  const [user, setUser]       = useState(null);
  const [loading, setLoading] = useState(true); // true while we check session

  // On mount, check if user already has an active session
  useEffect(() => {
    authApi
      .me()
      .then((userData) => setUser(userData))
      .catch(() => setUser(null))
      .finally(() => setLoading(false));
  }, []);

  const login = useCallback(() => {
    authApi.loginWithGoogle();
  }, []);

  const logout = useCallback(async () => {
    try {
      await authApi.logout();
    } catch (_) {
      // ignore – session may already be gone
    } finally {
      setUser(null);
    }
  }, []);

  return (
    <AuthContext.Provider value={{ user, loading, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

/** Hook to consume auth state anywhere in the tree */
export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>');
  return ctx;
}
