import { useState } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import Sidebar from '../components/Sidebar';
import Header  from '../components/Header';
import InsufficientEmailsModal from '../components/InsufficientEmailsModal';
import styles from './DashboardPage.module.css';

const PAGE_META = {
  '/dashboard':          { title: 'Overview',  subtitle: 'Your inbox at a glance'          },
  '/dashboard/emails':   { title: 'Emails',    subtitle: 'Browse and search your inbox'    },
  '/dashboard/settings': { title: 'Settings',  subtitle: 'Manage your API key and accounts'},
};

export default function DashboardPage() {
  const [collapsed, setCollapsed]             = useState(false);
  const [refreshKey, setRefreshKey]           = useState(0);
  const [insufficientExtra, setInsufficient]  = useState(null);
  const location = useLocation();

  const meta = PAGE_META[location.pathname] ?? PAGE_META['/dashboard'];

  const handleAnalyzeSuccess = () => setRefreshKey((k) => k + 1);
  const handleInsufficientEmails = (extra) => setInsufficient(extra);

  return (
    <div className={styles.layout}>
      <Sidebar collapsed={collapsed} onToggle={() => setCollapsed((c) => !c)} />

      <div className={styles.main}>
        <Header
          title={meta.title}
          subtitle={meta.subtitle}
          onAnalyzeSuccess={handleAnalyzeSuccess}
          onInsufficientEmails={handleInsufficientEmails}
        />
        <div className={styles.content}>
          {/* Pass refreshKey down via context would be more elegant,
              but passing as prop via Outlet context is clean enough */}
          <Outlet context={{ refreshKey }} />
        </div>
      </div>

      {insufficientExtra !== null && (
        <InsufficientEmailsModal
          extra={insufficientExtra}
          onClose={() => setInsufficient(null)}
        />
      )}
    </div>
  );
}
