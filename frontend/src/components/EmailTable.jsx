import { useState, useEffect, useCallback } from 'react';
import { Search, ChevronLeft, ChevronRight, RefreshCw, Inbox, Filter } from 'lucide-react';
import { emailsApi } from '../api/emails';
import { useToast } from '../context/ToastContext';
import styles from './EmailTable.module.css';

/* ── Helpers ───────────────────────────────────────── */
const CATEGORY_COLORS = {
  CAREER_OPPORTUNITIES:  { bg: 'rgba(124,92,252,0.15)', color: '#9B7FFF', border: 'rgba(124,92,252,0.3)' },
  APPLICATION_UPDATES:   { bg: 'rgba(79,195,247,0.12)', color: '#4FC3F7', border: 'rgba(79,195,247,0.3)' },
  INTERVIEW_INVITATIONS: { bg: 'rgba(74,222,128,0.12)', color: '#4ADE80', border: 'rgba(74,222,128,0.3)' },
  CODING_ASSESSMENTS:    { bg: 'rgba(250,204,21,0.12)', color: '#FACC15', border: 'rgba(250,204,21,0.3)' },
  BANKING_AND_PAYMENTS:  { bg: 'rgba(74,222,128,0.12)', color: '#4ADE80', border: 'rgba(74,222,128,0.3)' },
  SECURITY_ALERTS:       { bg: 'rgba(255,92,122,0.12)', color: '#FF5C7A', border: 'rgba(255,92,122,0.3)' },
  COLLEGE_AND_ACADEMICS: { bg: 'rgba(124,92,252,0.15)', color: '#9B7FFF', border: 'rgba(124,92,252,0.3)' },
  LEARNING_PLATFORMS:    { bg: 'rgba(79,195,247,0.12)', color: '#4FC3F7', border: 'rgba(79,195,247,0.3)' },
  CERTIFICATIONS:        { bg: 'rgba(247,200,79,0.12)', color: '#F7C84F', border: 'rgba(247,200,79,0.3)' },
  CODING_PLATFORMS:      { bg: 'rgba(99,102,241,0.12)', color: '#818CF8', border: 'rgba(99,102,241,0.3)' },
  HACKATHONS:            { bg: 'rgba(250,204,21,0.12)', color: '#FACC15', border: 'rgba(250,204,21,0.3)' },
  OPEN_SOURCE:           { bg: 'rgba(74,222,128,0.12)', color: '#4ADE80', border: 'rgba(74,222,128,0.3)' },
  BLOGS:                 { bg: 'rgba(148,163,184,0.1)', color: '#94A3B8', border: 'rgba(148,163,184,0.2)' },
  NEWSLETTERS:           { bg: 'rgba(148,163,184,0.1)', color: '#94A3B8', border: 'rgba(148,163,184,0.2)' },
  NEWS_FEEDS:            { bg: 'rgba(148,163,184,0.1)', color: '#94A3B8', border: 'rgba(148,163,184,0.2)' },
  VIDEO_NOTIFICATIONS:   { bg: 'rgba(255,92,122,0.12)', color: '#FF5C7A', border: 'rgba(255,92,122,0.3)' },
  WEEKLY_DIGESTS:        { bg: 'rgba(148,163,184,0.1)', color: '#94A3B8', border: 'rgba(148,163,184,0.2)' },
  COMMUNITY_UPDATES:     { bg: 'rgba(79,195,247,0.12)', color: '#4FC3F7', border: 'rgba(79,195,247,0.3)' },
  PROMOTIONS:            { bg: 'rgba(250,204,21,0.12)', color: '#FACC15', border: 'rgba(250,204,21,0.3)' },
  MARKETING:             { bg: 'rgba(250,204,21,0.12)', color: '#FACC15', border: 'rgba(250,204,21,0.3)' },
  SPAM:                  { bg: 'rgba(255,92,122,0.12)', color: '#FF5C7A', border: 'rgba(255,92,122,0.3)' },
  GENERAL_UNIVERSITY:    { bg: 'rgba(124,92,252,0.15)', color: '#9B7FFF', border: 'rgba(124,92,252,0.3)' },
  COMMUNITY_ACTIVITIES:  { bg: 'rgba(247,200,79,0.12)', color: '#F7C84F', border: 'rgba(247,200,79,0.3)' },
  EVENT_INVITATIONS:     { bg: 'rgba(99,102,241,0.12)', color: '#818CF8', border: 'rgba(99,102,241,0.3)' },
};

const PRIORITY_CONFIG = {
  HIGH:   { label: 'High',   color: '#FF5C7A', dot: '#FF5C7A' },
  MEDIUM: { label: 'Med',    color: '#FACC15', dot: '#FACC15' },
  LOW:    { label: 'Low',    color: '#4ADE80', dot: '#4ADE80' },
};

function getPriority(p) {
  if (!p) return PRIORITY_CONFIG.LOW;
  const key = typeof p === 'string' ? p.toUpperCase() : (p?.name ?? '');
  return PRIORITY_CONFIG[key] ?? PRIORITY_CONFIG.LOW;
}

function formatDate(iso) {
  if (!iso) return '—';
  const d = new Date(iso);
  const now = new Date();
  const diff = now - d;
  if (diff < 86400000) {
    return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  }
  return d.toLocaleDateString([], { month: 'short', day: 'numeric' });
}

function CategoryBadge({ category }) {
  const c = CATEGORY_COLORS[category?.toUpperCase?.()] ?? { bg: 'rgba(148,163,184,0.1)', color: '#94A3B8', border: 'rgba(148,163,184,0.2)' };
  const label = category
    ? category.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase())
    : 'Other';
  return (
    <span
      className={styles.categoryBadge}
      style={{ background: c.bg, color: c.color, border: `1px solid ${c.border}` }}
    >
      {label}
    </span>
  );
}

function PriorityDot({ priority }) {
  const p = getPriority(priority);
  return (
    <span className={styles.priorityDot} title={p.label}>
      <span style={{ background: p.dot }} className={styles.dot} />
      <span style={{ color: p.color }}>{p.label}</span>
    </span>
  );
}

/* ── Skeleton row ──────────────────────────────────── */
function SkeletonRow() {
  return (
    <tr className={styles.skeletonRow}>
      {[60, 180, 280, 80, 60, 70].map((w, i) => (
        <td key={i}>
          <div className="skeleton" style={{ height: 14, width: w, borderRadius: 6 }} />
        </td>
      ))}
    </tr>
  );
}

/* ── Main Component ────────────────────────────────── */
export default function EmailTable({ refreshKey, onSelectEmail }) {
  const toast = useToast();
  const [emails, setEmails]     = useState([]);
  const [loading, setLoading]   = useState(false);
  const [page, setPage]         = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalItems, setTotalItems] = useState(0);
  const [search, setSearch]     = useState('');
  const [filterCat, setFilterCat]  = useState('');

  const PAGE_SIZE = 20;

  const fetchEmails = useCallback(async (pg = 0) => {
    setLoading(true);
    try {
      let data;
      if (filterCat) {
        data = await emailsApi.getByCategory(filterCat, pg, PAGE_SIZE);
      } else {
        data = await emailsApi.getAll(pg, PAGE_SIZE);
      }
      setEmails(data.content ?? []);
      setTotalPages(data.totalPages ?? 0);
      setTotalItems(data.totalElements ?? 0);
      setPage(data.number ?? 0);
    } catch (err) {
      toast.error(err.message || 'Failed to load emails.');
    } finally {
      setLoading(false);
    }
  }, [filterCat, toast]);

  useEffect(() => { fetchEmails(0); }, [fetchEmails, refreshKey]);

  const handleFilterChange = (cat) => { setFilterCat(cat); setPage(0); };

  // Client-side search filter
  const displayed = search.trim()
    ? emails.filter(
        (e) =>
          e.subject?.toLowerCase().includes(search.toLowerCase()) ||
          e.sender?.toLowerCase().includes(search.toLowerCase()) ||
          e.summary?.toLowerCase().includes(search.toLowerCase())
      )
    : emails;

  const CATEGORIES = [
    'CAREER_OPPORTUNITIES', 'APPLICATION_UPDATES', 'INTERVIEW_INVITATIONS',
    'CODING_ASSESSMENTS', 'BANKING_AND_PAYMENTS', 'SECURITY_ALERTS', 'COLLEGE_AND_ACADEMICS',
    'LEARNING_PLATFORMS', 'CERTIFICATIONS', 'CODING_PLATFORMS', 'HACKATHONS', 'OPEN_SOURCE',
    'BLOGS', 'NEWSLETTERS', 'NEWS_FEEDS', 'VIDEO_NOTIFICATIONS', 'WEEKLY_DIGESTS', 'COMMUNITY_UPDATES',
    'PROMOTIONS', 'MARKETING', 'SPAM', 'GENERAL_UNIVERSITY',
    'COMMUNITY_ACTIVITIES', 'EVENT_INVITATIONS',
  ];

  // Format category name: CAREER_OPPORTUNITIES → Career Opportunities
  const formatCat = (c) => c.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase());

  return (
    <div className={styles.wrapper}>
      {/* ── Toolbar ─── */}
      <div className={styles.toolbar}>
        {/* Search */}
        <div className={styles.searchWrap}>
          <Search size={16} className={styles.searchIcon} />
          <input
            id="email-search-input"
            className={styles.searchInput}
            type="text"
            placeholder="Search emails…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
          {search && (
            <button className={styles.clearSearch} onClick={() => setSearch('')}>✕</button>
          )}
        </div>

        {/* Category filter */}
        <div className={styles.filterWrap}>
          <Filter size={14} style={{ color: 'var(--text-muted)' }} />
          <select
            id="email-category-filter"
            className={styles.filterSelect}
            value={filterCat}
            onChange={(e) => handleFilterChange(e.target.value)}
          >
            <option value="">All Categories</option>
            {CATEGORIES.map((c) => (
              <option key={c} value={c}>{formatCat(c)}</option>
            ))}
          </select>
        </div>

        {/* Refresh */}
        <button
          id="refresh-emails-btn"
          className={`btn btn-ghost btn-sm ${styles.refreshBtn}`}
          onClick={() => fetchEmails(page)}
          disabled={loading}
          title="Refresh"
        >
          <RefreshCw size={14} className={loading ? styles.spinning : ''} />
        </button>

        <span className={styles.countLabel}>{totalItems.toLocaleString()} email{totalItems !== 1 ? 's' : ''}</span>
      </div>

      {/* ── Table ─── */}
      <div className={styles.tableWrap}>
        <table className={styles.table}>
          <thead>
            <tr>
              <th>Priority</th>
              <th>Sender</th>
              <th>Subject</th>
              <th>Category</th>
              <th>Received</th>
              <th>Analyzed</th>
            </tr>
          </thead>
          <tbody>
            {loading
              ? Array.from({ length: 6 }).map((_, i) => <SkeletonRow key={i} />)
              : displayed.length === 0
              ? (
                <tr>
                  <td colSpan={6}>
                    <div className={styles.empty}>
                      <Inbox size={40} style={{ color: 'var(--text-muted)', opacity: 0.5 }} />
                      <p>No emails found</p>
                      <span>
                        {search ? 'Try a different search term' : 'Sync your inbox or run AI analysis'}
                      </span>
                    </div>
                  </td>
                </tr>
              )
              : displayed.map((email) => (
                <tr key={email.id} className={styles.row} onClick={() => onSelectEmail?.(email)}>
                  <td><PriorityDot priority={email.priority} /></td>
                  <td>
                    <span className={styles.sender} title={email.sender}>
                      {email.sender?.replace(/<.*>/, '').trim() || email.sourceAccount || '—'}
                    </span>
                  </td>
                  <td>
                    <div className={styles.subjectCell}>
                      <span className={styles.subject} title={email.subject}>
                        {email.subject || '(No subject)'}
                      </span>
                      {email.summary && (
                        <span className={styles.summary}>{email.summary}</span>
                      )}
                    </div>
                  </td>
                  <td><CategoryBadge category={email.category} /></td>
                  <td className={styles.date}>{formatDate(email.receivedAt)}</td>
                  <td className={styles.date}>
                    {email.analyzedAt ? formatDate(email.analyzedAt) : <span className={styles.notAnalyzed}>—</span>}
                  </td>
                </tr>
              ))
            }
          </tbody>
        </table>
      </div>

      {/* ── Pagination ─── */}
      {totalPages > 1 && (
        <div className={styles.pagination}>
          <button
            className={`btn btn-ghost btn-sm ${styles.pageBtn}`}
            disabled={page === 0}
            onClick={() => fetchEmails(page - 1)}
          >
            <ChevronLeft size={15} /> Prev
          </button>
          <span className={styles.pageInfo}>
            Page {page + 1} of {totalPages}
          </span>
          <button
            className={`btn btn-ghost btn-sm ${styles.pageBtn}`}
            disabled={page >= totalPages - 1}
            onClick={() => fetchEmails(page + 1)}
          >
            Next <ChevronRight size={15} />
          </button>
        </div>
      )}
    </div>
  );
}
