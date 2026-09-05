import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

/**
 * Renders children only when authenticated.
 * Shows a full-screen spinner while the session check is in-flight.
 * Redirects to /login if not authenticated.
 */
export default function ProtectedRoute({ children }) {
  const { user, loading } = useAuth();

  if (loading) {
    return (
      <div
        style={{
          minHeight: '100vh',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          gap: '16px',
          background: 'var(--bg-base)',
        }}
      >
        {/* Animated logo mark */}
        <div
          style={{
            width: 56,
            height: 56,
            borderRadius: '16px',
            background: 'var(--gradient-brand)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            animation: 'pulse-glow 2s ease-in-out infinite',
            boxShadow: '0 0 30px rgba(124,92,252,0.4)',
          }}
        >
          <svg width="28" height="28" viewBox="0 0 28 28" fill="none">
            <rect x="3" y="5" width="22" height="18" rx="3" stroke="white" strokeWidth="2" fill="none" />
            <path d="M3 9l11 7 11-7" stroke="white" strokeWidth="2" strokeLinecap="round" />
          </svg>
        </div>
        <div className="spinner" style={{ width: 28, height: 28, borderWidth: 3 }} />
        <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>Loading MailInsight…</p>
      </div>
    );
  }

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  return children;
}
