import { useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { authApi } from '../api/client';
import { useAuth } from '../context/AuthContext';

/**
 * OAuthCallbackPage
 * Spring Security redirects the browser to / after a successful OAuth2 login.
 * This page is also registered as the fallback for any /oauth2/callback path.
 * It simply re-fetches /api/auth/me to populate the AuthContext, then
 * redirects the user to the dashboard.
 */
export default function OAuthCallbackPage() {
  const navigate  = useNavigate();
  const calledRef = useRef(false); // prevent StrictMode double-invoke

  useEffect(() => {
    if (calledRef.current) return;
    calledRef.current = true;

    authApi
      .me()
      .then(() => navigate('/dashboard', { replace: true }))
      .catch(() => navigate('/login?error=oauth_failed', { replace: true }));
  }, [navigate]);

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        gap: '20px',
        background: 'var(--bg-base)',
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
          animation: 'pulse-glow 2s ease-in-out infinite',
        }}
      >
        <svg width="32" height="32" viewBox="0 0 28 28" fill="none">
          <rect x="3" y="5" width="22" height="18" rx="3" stroke="white" strokeWidth="2" fill="none" />
          <path d="M3 9l11 7 11-7" stroke="white" strokeWidth="2" strokeLinecap="round" />
        </svg>
      </div>

      <div className="spinner" style={{ width: 32, height: 32, borderWidth: 3 }} />

      <div style={{ textAlign: 'center' }}>
        <p style={{ color: 'var(--text-primary)', fontFamily: 'var(--font-display)', fontWeight: 600 }}>
          Signing you in…
        </p>
        <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem', marginTop: '4px' }}>
          Completing Google authentication
        </p>
      </div>
    </div>
  );
}
