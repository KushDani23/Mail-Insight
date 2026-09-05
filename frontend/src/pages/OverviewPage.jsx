import { useState, useEffect } from 'react';
import { Mail, Layers, Star, Wifi } from 'lucide-react';
import { emailsApi } from '../api/emails';
import { userApi } from '../api/user';
import styles from './OverviewPage.module.css';

function StatCard({ icon: Icon, label, value, color, loading }) {
  return (
    <div className={`glass-card ${styles.statCard}`}>
      <div className={styles.statIcon} style={{ background: color + '20', border: `1px solid ${color}40` }}>
        <Icon size={20} style={{ color }} />
      </div>
      <div>
        {loading
          ? <div className="skeleton" style={{ width: 60, height: 28, borderRadius: 6, marginBottom: 4 }} />
          : <p className={styles.statValue}>{value}</p>
        }
        <p className={styles.statLabel}>{label}</p>
      </div>
    </div>
  );
}

export default function OverviewPage() {
  const [stats, setStats]     = useState(null);
  const [newCount, setNewCount] = useState(null);
  const [hasKey, setHasKey]   = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    Promise.allSettled([
      emailsApi.getStats(),
      emailsApi.getNewCount(),
      userApi.getApiKeyStatus(),
    ]).then(([s, n, k]) => {
      if (s.status === 'fulfilled') setStats(s.value);
      if (n.status === 'fulfilled') setNewCount(n.value);
      if (k.status === 'fulfilled') setHasKey(k.value?.hasKey ?? false);
    }).finally(() => setLoading(false));
  }, []);

  const topCategories = stats?.countByCategory
    ? Object.entries(stats.countByCategory)
        .sort((a, b) => b[1] - a[1])
        .slice(0, 5)
    : [];

  const totalForPct = topCategories.reduce((s, [, v]) => s + v, 0) || 1;

  const CAT_COLORS = {
    WORK: '#9B7FFF', PERSONAL: '#4FC3F7', FINANCE: '#4ADE80',
    PROMOTIONS: '#FACC15', SOCIAL: '#F7C84F', SPAM: '#FF5C7A',
    UPDATES: '#818CF8', OTHER: '#94A3B8',
  };

  return (
    <div className={styles.wrapper}>
      {/* Stat cards */}
      <div className={styles.statsGrid}>
        <StatCard icon={Mail}   label="Total Emails"      value={stats?.totalEmails?.toLocaleString() ?? '—'} color="#9B7FFF" loading={loading} />
        <StatCard icon={Layers} label="New (Unanalyzed)"  value={newCount?.count?.toLocaleString() ?? '—'}    color="#4FC3F7" loading={loading} />
        <StatCard icon={Wifi}   label="Connected Accounts" value={stats?.connectedAccountsCount ?? '—'}        color="#4ADE80" loading={loading} />
        <StatCard icon={Star}   label="Gemini API Key"
          value={hasKey === null ? '…' : hasKey ? 'Configured ✓' : 'Not Set ✗'}
          color={hasKey ? '#4ADE80' : '#FACC15'}
          loading={loading}
        />
      </div>

      {/* Category breakdown */}
      <div className={`glass-card ${styles.breakdownCard}`}>
        <h3 className={styles.cardTitle}>Category Breakdown</h3>
        {loading ? (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12, marginTop: 12 }}>
            {[1,2,3].map(i => <div key={i} className="skeleton" style={{ height: 36, borderRadius: 8 }} />)}
          </div>
        ) : topCategories.length === 0 ? (
          <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem', marginTop: 12 }}>
            No data yet — run an AI analysis first.
          </p>
        ) : (
          <div className={styles.categories}>
            {topCategories.map(([cat, count]) => {
              const color = CAT_COLORS[cat] ?? '#94A3B8';
              const pct   = ((count / totalForPct) * 100).toFixed(1);
              return (
                <div key={cat} className={styles.catRow}>
                  <div className={styles.catMeta}>
                    <span className={styles.catDot} style={{ background: color }} />
                    <span className={styles.catName}>{cat.charAt(0) + cat.slice(1).toLowerCase()}</span>
                    <span className={styles.catCount}>{count.toLocaleString()}</span>
                    <span className={styles.catPct}>{pct}%</span>
                  </div>
                  <div className={styles.catTrack}>
                    <div className={styles.catFill} style={{ width: `${pct}%`, background: color }} />
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* New emails indicator */}
      {newCount !== null && (
        <div className={`glass-card ${styles.newEmailsCard}`}>
          <div className={styles.newEmailsIcon}>📬</div>
          <div>
            <p className={styles.newEmailsCount}>
              {newCount.count} new email{newCount.count !== 1 ? 's' : ''} ready
            </p>
            <p className={styles.newEmailsHint}>
              {newCount.count >= 10
                ? 'You have enough for AI analysis! Click "Analyze Emails" above.'
                : `Need ${10 - newCount.count} more to unlock AI analysis (min. 10 required).`
              }
            </p>
            {newCount.count < 10 && (
              <div className={styles.miniProgress}>
                <div className={styles.miniFill} style={{ width: `${(newCount.count / 10) * 100}%` }} />
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
