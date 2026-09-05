import { X, AlertTriangle, Mail } from 'lucide-react';
import styles from './InsufficientEmailsModal.module.css';

/**
 * Shown when backend returns 422 – not enough new emails to trigger AI analysis.
 * extra: { currentCount, requiredCount }
 */
export default function InsufficientEmailsModal({ extra, onClose }) {
  const current  = extra?.currentCount  ?? 0;
  const required = extra?.requiredCount ?? 10;
  const pct      = Math.min((current / required) * 100, 100);
  const remaining = required - current;

  return (
    <div className={styles.overlay} onClick={onClose} role="dialog" aria-modal="true">
      <div
        className={`glass-card ${styles.modal}`}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Close */}
        <button className={styles.closeBtn} onClick={onClose} id="insufficient-modal-close">
          <X size={18} />
        </button>

        {/* Icon */}
        <div className={styles.iconWrap}>
          <AlertTriangle size={28} className={styles.icon} />
        </div>

        {/* Title */}
        <h2 className={styles.title}>Not Enough Emails Yet</h2>
        <p className={styles.desc}>
          To protect against AI quota limits and rate limiting, analysis requires a minimum
          of <strong>{required} new emails</strong>. You currently have{' '}
          <strong>{current}</strong>.
        </p>

        {/* Progress */}
        <div className={styles.progressSection}>
          <div className={styles.progressHeader}>
            <span className={styles.progressLabel}>
              <Mail size={14} /> New emails collected
            </span>
            <span className={styles.progressCount}>
              {current} / {required}
            </span>
          </div>

          <div className={styles.progressTrack}>
            <div
              className={styles.progressFill}
              style={{ width: `${pct}%` }}
            />
          </div>

          <p className={styles.progressHint}>
            {remaining > 0
              ? `Waiting for ${remaining} more email${remaining !== 1 ? 's' : ''} to arrive…`
              : 'Threshold reached! Try analyzing again.'}
          </p>
        </div>

        {/* Tips */}
        <div className={styles.tipBox}>
          <p className={styles.tipTitle}>💡 Why this limit?</p>
          <p className={styles.tipText}>
            Google Gemini has per-minute token rate limits. Batching at least {required} emails
            ensures you get a complete, meaningful analysis without hitting quota errors.
          </p>
        </div>

        <button className="btn btn-ghost" onClick={onClose} style={{ width: '100%', marginTop: 4 }}>
          Got it, I'll wait
        </button>
      </div>
    </div>
  );
}
