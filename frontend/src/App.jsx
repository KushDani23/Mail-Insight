import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider }      from './context/AuthContext';
import { ToastProvider }     from './context/ToastContext';
import { useAuth }           from './context/AuthContext';
import ProtectedRoute        from './components/ProtectedRoute';
import LoginPage             from './pages/LoginPage';
import OAuthCallbackPage     from './pages/OAuthCallbackPage';

/* ── Placeholder – Phase 2 will replace this ─── */
function DashboardPlaceholder() {
  const { user, logout } = useAuth();

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        gap: '24px',
        background: 'var(--bg-base)',
        fontFamily: 'var(--font-display)',
        textAlign: 'center',
        padding: '24px',
      }}
    >
      <div
        style={{
          width: 64,
          height: 64,
          borderRadius: '18px',
          background: 'var(--gradient-brand)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          boxShadow: '0 0 40px rgba(124,92,252,0.45)',
        }}
      >
        <svg width="32" height="32" viewBox="0 0 28 28" fill="none">
          <rect x="3" y="5" width="22" height="18" rx="3" stroke="white" strokeWidth="2" fill="none" />
          <path d="M3 9l11 7 11-7" stroke="white" strokeWidth="2" strokeLinecap="round" />
        </svg>
      </div>
      <div>
        <h1 style={{ fontSize: '2rem', marginBottom: '8px' }} className="gradient-text">
          Welcome, {user?.name?.split(' ')[0]}! 👋
        </h1>
        <p style={{ color: 'var(--text-muted)' }}>
          Authentication is working. Dashboard coming in Phase 2.
        </p>
      </div>
      <div
        className="glass-card"
        style={{ padding: '16px 24px', display: 'flex', alignItems: 'center', gap: '12px' }}
      >
        {user?.pictureUrl && (
          <img
            src={user.pictureUrl}
            alt={user.name}
            style={{ width: 40, height: 40, borderRadius: '50%', border: '2px solid rgba(124,92,252,0.4)' }}
          />
        )}
        <div style={{ textAlign: 'left' }}>
          <p style={{ color: 'var(--text-primary)', fontWeight: 600, fontSize: '0.95rem' }}>{user?.name}</p>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.8rem' }}>{user?.email}</p>
        </div>
      </div>
      <button className="btn btn-ghost btn-sm" onClick={logout}>
        Sign out
      </button>
    </div>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <ToastProvider>
          <Routes>
            {/* Public routes */}
            <Route path="/login"            element={<LoginPage />} />
            <Route path="/oauth2/callback"  element={<OAuthCallbackPage />} />

            {/* Protected routes */}
            <Route
              path="/dashboard"
              element={
                <ProtectedRoute>
                  <DashboardPlaceholder />
                </ProtectedRoute>
              }
            />

            {/* Default redirect */}
            <Route path="*" element={<Navigate to="/dashboard" replace />} />
          </Routes>
        </ToastProvider>
      </AuthProvider>
    </BrowserRouter>
  );
}
