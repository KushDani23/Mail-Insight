import { useState, useEffect } from 'react';
import { Mail, Layers, Star, Wifi } from 'lucide-react';
import { emailsApi } from '../api/emails';
import { userApi } from '../api/user';
import CategoryDonutChart from '../components/charts/CategoryDonutChart';
import PriorityBarChart   from '../components/charts/PriorityBarChart';
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

  return (
    <div className={styles.wrapper}>
      {/* ── Stat Cards ── */}
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

      {/* ── Charts Row ── */}
      <div className={styles.chartsRow}>
        <div className={`glass-card ${styles.chartCard}`}>
          <h3 className={styles.cardTitle}>Category Breakdown</h3>
          {loading ? (
            <div className="skeleton" style={{ height: 260, borderRadius: 12 }} />
          ) : (
            <CategoryDonutChart countByCategory={stats?.countByCategory} />
          )}
        </div>

        <div className={`glass-card ${styles.chartCard}`}>
          <h3 className={styles.cardTitle}>Priority Distribution</h3>
          {loading ? (
            <div className="skeleton" style={{ height: 260, borderRadius: 12 }} />
          ) : (
            <PriorityBarChart countByPriority={stats?.countByPriority} />
          )}
        </div>
      </div>

      {/* ── Priority Summary Cards ── */}
      {!loading && stats?.countByPriority && (
        <div className={styles.priorityCards}>
          {[
            { key: 'HIGH',   label: 'High Priority',   color: '#FF5C7A', emoji: '🔴' },
            { key: 'MEDIUM', label: 'Medium Priority',  color: '#FACC15', emoji: '🟡' },
            { key: 'LOW',    label: 'Low Priority',     color: '#4ADE80', emoji: '🟢' },
          ].map(({ key, label, color, emoji }) => {
            const count = stats.countByPriority[key] ?? 0;
            const total = stats.totalEmails || 1;
            const pct   = ((count / total) * 100).toFixed(1);
            return (
              <div key={key} className={`glass-card ${styles.prioCard}`}>
                <div className={styles.prioHeader}>
                  <span className={styles.prioEmoji}>{emoji}</span>
                  <span className={styles.prioLabel}>{label}</span>
                </div>
                <p className={styles.prioCount} style={{ color }}>{count.toLocaleString()}</p>
                <div className={styles.prioTrack}>
                  <div className={styles.prioFill} style={{ width: `${pct}%`, background: color }} />
                </div>
                <p className={styles.prioPct}>{pct}% of total</p>
              </div>
            );
          })}
        </div>
      )}

      {/* ── New Emails Indicator ── */}
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
