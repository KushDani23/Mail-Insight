import { X, Mail, User, Clock, Tag, AlertTriangle, Sparkles, Calendar } from 'lucide-react';
import styles from './EmailDetailDrawer.module.css';

const CATEGORY_COLORS = {
  WORK:       '#9B7FFF', PERSONAL:   '#4FC3F7', FINANCE:    '#4ADE80',
  PROMOTIONS: '#FACC15', SOCIAL:     '#F7C84F', SPAM:       '#FF5C7A',
  UPDATES:    '#818CF8', OTHER:      '#94A3B8',
};

const PRIORITY_CONFIG = {
  HIGH:   { label: 'High',   color: '#FF5C7A', icon: AlertTriangle },
  MEDIUM: { label: 'Medium', color: '#FACC15', icon: Tag },
  LOW:    { label: 'Low',    color: '#4ADE80', icon: Tag },
};

function formatFullDate(iso) {
  if (!iso) return '—';
  return new Date(iso).toLocaleString([], {
    year: 'numeric', month: 'short', day: 'numeric',
    hour: '2-digit', minute: '2-digit',
  });
}

function getPriority(p) {
  if (!p) return PRIORITY_CONFIG.LOW;
  const key = typeof p === 'string' ? p.toUpperCase() : (p?.name ?? '');
  return PRIORITY_CONFIG[key] ?? PRIORITY_CONFIG.LOW;
}

export default function EmailDetailDrawer({ email, onClose }) {
  if (!email) return null;

  const priority = getPriority(email.priority);
  const PriorityIcon = priority.icon;
  const catColor = CATEGORY_COLORS[email.category?.toUpperCase?.()] ?? '#94A3B8';

  return (
    <div className={styles.overlay} onClick={onClose}>
      <aside className={styles.drawer} onClick={(e) => e.stopPropagation()}>
        {/* Header */}
        <div className={styles.header}>
          <h2 className={styles.drawerTitle}>Email Details</h2>
          <button className={styles.closeBtn} onClick={onClose} id="email-drawer-close">
            <X size={18} />
          </button>
        </div>

        {/* Subject */}
        <div className={styles.subjectBlock}>
          <h3 className={styles.subject}>{email.subject || '(No subject)'}</h3>
          <div className={styles.badges}>
            <span
              className={styles.catBadge}
              style={{
                background: catColor + '20',
                color: catColor,
                border: `1px solid ${catColor}40`,
              }}
            >
              {email.category ?? 'Other'}
            </span>
            <span
              className={styles.prioBadge}
              style={{
                background: priority.color + '18',
                color: priority.color,
                border: `1px solid ${priority.color}35`,
              }}
            >
              <PriorityIcon size={12} />
              {priority.label}
            </span>
          </div>
        </div>

        <div className="divider" />

        {/* Meta rows */}
        <div className={styles.metaGrid}>
          <div className={styles.metaRow}>
            <User size={15} className={styles.metaIcon} />
            <div>
              <p className={styles.metaLabel}>From</p>
              <p className={styles.metaValue}>{email.sender || '—'}</p>
            </div>
          </div>
          <div className={styles.metaRow}>
            <Mail size={15} className={styles.metaIcon} />
            <div>
              <p className={styles.metaLabel}>Account</p>
              <p className={styles.metaValue}>{email.sourceAccount || '—'}</p>
            </div>
          </div>
          <div className={styles.metaRow}>
            <Calendar size={15} className={styles.metaIcon} />
            <div>
              <p className={styles.metaLabel}>Received</p>
              <p className={styles.metaValue}>{formatFullDate(email.receivedAt)}</p>
            </div>
          </div>
          <div className={styles.metaRow}>
            <Clock size={15} className={styles.metaIcon} />
            <div>
              <p className={styles.metaLabel}>Analyzed</p>
              <p className={styles.metaValue}>
                {email.analyzedAt ? formatFullDate(email.analyzedAt) : 'Not yet analyzed'}
              </p>
            </div>
          </div>
        </div>

        <div className="divider" />

        {/* AI Summary */}
        <div className={styles.summarySection}>
          <div className={styles.summaryHeader}>
            <Sparkles size={16} style={{ color: 'var(--color-primary-light)' }} />
            <h4 className={styles.summaryTitle}>AI Summary</h4>
          </div>
          {email.summary ? (
            <p className={styles.summaryText}>{email.summary}</p>
          ) : (
            <div className={styles.noSummary}>
              <p>No AI summary available yet.</p>
              <span>Run "Analyze Emails" to generate insights.</span>
            </div>
          )}
        </div>

        {/* Quick info footer */}
        <div className={styles.footer}>
          <span className={styles.footerItem}>
            ID: <code>{email.gmailMessageId?.slice(0, 12) ?? '—'}…</code>
          </span>
        </div>
      </aside>
    </div>
  );
}
