import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import styles from './LoginPage.module.css';

/* ── Feature bullets shown on the left panel ─────────────────── */
const FEATURES = [
  {
    icon: '🧠',
    title: 'AI-Powered Analysis',
    desc: 'Gemini AI reads, categorizes, and summarizes your inbox automatically.',
  },
  {
    icon: '📊',
    title: 'Visual Insights',
    desc: 'Interactive charts show category breakdowns, priorities, and sentiment.',
  },
  {
    icon: '⚡',
    title: 'Priority Detection',
    desc: 'Never miss critical emails — AI flags High, Medium, and Low priority.',
  },
  {
    icon: '🔒',
    title: 'Secure & Private',
    desc: 'Your Gemini API key is AES-256 encrypted. We never store raw email content.',
  },
];

export default function LoginPage() {
  const { user, loading, login } = useAuth();
  const navigate                 = useNavigate();
  const [searchParams]           = useSearchParams();
  const [loginLoading, setLoginLoading] = useState(false);
  const [showError, setShowError]       = useState(false);

  // If already logged in, skip to dashboard
  useEffect(() => {
    if (!loading && user) navigate('/dashboard', { replace: true });
  }, [user, loading, navigate]);

  // Show error if OAuth redirect returned ?error=oauth_failed
  useEffect(() => {
    if (searchParams.get('error')) setShowError(true);
  }, [searchParams]);

  const handleLogin = () => {
    setLoginLoading(true);
    login(); // full-page redirect — page unloads, so no need to reset state
  };

  if (loading) return null; // ProtectedRoute handles spinner

  return (
    <div className={styles.wrapper}>
      {/* Ambient background orbs */}
      <div className={styles.orb1} />
      <div className={styles.orb2} />
      <div className={styles.orb3} />

      {/* ── Left panel – Branding ───────────────────────────── */}
      <div className={styles.leftPanel}>
        <div className={styles.logoRow}>
          <div className={styles.logoMark}>
            <svg width="26" height="26" viewBox="0 0 28 28" fill="none">
              <rect x="3" y="5" width="22" height="18" rx="3" stroke="white" strokeWidth="2" fill="none" />
              <path d="M3 9l11 7 11-7" stroke="white" strokeWidth="2" strokeLinecap="round" />
            </svg>
          </div>
          <span className={styles.logoText}>MailInsight</span>
        </div>

        <div className={styles.heroText}>
          <h1 className={styles.headline}>
            Your inbox,{' '}
            <span className="gradient-text">intelligently</span>
            <br />understood.
          </h1>
          <p className={styles.subheadline}>
            Connect your Gmail account and let AI do the heavy lifting — categorize,
            prioritize, and surface what actually matters.
          </p>
        </div>

        <ul className={styles.featureList}>
          {FEATURES.map((f) => (
            <li key={f.title} className={styles.featureItem}>
              <span className={styles.featureIcon}>{f.icon}</span>
              <div>
                <p className={styles.featureTitle}>{f.title}</p>
                <p className={styles.featureDesc}>{f.desc}</p>
              </div>
            </li>
          ))}
        </ul>

        <p className={styles.footerNote}>
          Trusted with your privacy · Powered by Google Gemini
        </p>
      </div>

      {/* ── Right panel – Login card ────────────────────────── */}
      <div className={styles.rightPanel}>
        <div className={`glass-card ${styles.card}`}>
          {/* Card header */}
          <div className={styles.cardHeader}>
            <div className={styles.cardLogoMark}>
              <svg width="22" height="22" viewBox="0 0 28 28" fill="none">
                <rect x="3" y="5" width="22" height="18" rx="3" stroke="white" strokeWidth="2" fill="none" />
                <path d="M3 9l11 7 11-7" stroke="white" strokeWidth="2" strokeLinecap="round" />
              </svg>
            </div>
            <h2 className={styles.cardTitle}>Welcome to MailInsight</h2>
            <p className={styles.cardSubtitle}>
              Sign in with Google to analyze your Gmail inbox with AI.
            </p>
          </div>

          {/* Error banner */}
          {showError && (
            <div className={styles.errorBanner} role="alert">
              <span>⚠️</span>
              <span>Google sign-in failed. Please try again.</span>
              <button className={styles.errorClose} onClick={() => setShowError(false)}>✕</button>
            </div>
          )}

          {/* Google Sign-in button */}
          <button
            id="google-signin-btn"
            className={styles.googleBtn}
            onClick={handleLogin}
            disabled={loginLoading}
          >
            {loginLoading ? (
              <>
                <div className="spinner" style={{ width: 20, height: 20 }} />
                <span>Redirecting…</span>
              </>
            ) : (
              <>
                {/* Google 'G' logo */}
                <svg width="20" height="20" viewBox="0 0 48 48" aria-hidden="true">
                  <path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z"/>
                  <path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z"/>
                  <path fill="#FBBC05" d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z"/>
                  <path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"/>
                  <path fill="none" d="M0 0h48v48H0z"/>
                </svg>
                <span>Continue with Google</span>
              </>
            )}
          </button>

          {/* Divider */}
          <div className={styles.dividerRow}>
            <div className="divider" style={{ flex: 1 }} />
            <span className={styles.dividerText}>What you get</span>
            <div className="divider" style={{ flex: 1 }} />
          </div>

          {/* Mini feature pills */}
          <div className={styles.pillRow}>
            {['AI Summaries', 'Priority Ranking', 'Category Tags', 'Visual Charts'].map((p) => (
              <span key={p} className={`badge badge-primary ${styles.pill}`}>{p}</span>
            ))}
          </div>

          <p className={styles.privacyNote}>
            We request read-only access to your Gmail. Your data is never sold or shared.
          </p>
        </div>
      </div>
    </div>
  );
}
