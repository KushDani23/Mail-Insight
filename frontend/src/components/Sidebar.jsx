import { NavLink, useNavigate } from 'react-router-dom';
import {
  LayoutDashboard,
  Mail,
  Settings,
  LogOut,
  ChevronLeft,
  ChevronRight,
  Zap,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import styles from './Sidebar.module.css';

const NAV_ITEMS = [
  { to: '/dashboard',          icon: LayoutDashboard, label: 'Overview'  },
  { to: '/dashboard/emails',   icon: Mail,            label: 'Emails'    },
  { to: '/dashboard/settings', icon: Settings,        label: 'Settings'  },
];

export default function Sidebar({ collapsed, onToggle }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/login', { replace: true });
  };

  return (
    <aside className={`${styles.sidebar} ${collapsed ? styles.collapsed : ''}`}>
      {/* Logo */}
      <div className={styles.logo}>
        <div className={styles.logoMark}>
          <svg width="20" height="20" viewBox="0 0 28 28" fill="none">
            <rect x="3" y="5" width="22" height="18" rx="3" stroke="white" strokeWidth="2" fill="none" />
            <path d="M3 9l11 7 11-7" stroke="white" strokeWidth="2" strokeLinecap="round" />
          </svg>
        </div>
        {!collapsed && (
          <span className={styles.logoText}>MailInsight</span>
        )}
      </div>

      {/* Nav */}
      <nav className={styles.nav}>
        {NAV_ITEMS.map(({ to, icon: Icon, label }) => (
          <NavLink
            key={to}
            to={to}
            end={to === '/dashboard'}
            className={({ isActive }) =>
              `${styles.navItem} ${isActive ? styles.active : ''}`
            }
          >
            <Icon size={20} className={styles.navIcon} />
            {!collapsed && <span className={styles.navLabel}>{label}</span>}
            {collapsed && (
              <span className={styles.tooltip}>{label}</span>
            )}
          </NavLink>
        ))}
      </nav>

      {/* AI badge */}
      {!collapsed && (
        <div className={styles.aiBadge}>
          <Zap size={14} />
          <span>Powered by Gemini AI</span>
        </div>
      )}

      {/* User footer */}
      <div className={styles.footer}>
        <div className={styles.userRow}>
          {user?.pictureUrl ? (
            <img
              src={user.pictureUrl}
              alt={user.name}
              className={styles.avatar}
            />
          ) : (
            <div className={styles.avatarFallback}>
              {user?.name?.[0]?.toUpperCase() ?? 'U'}
            </div>
          )}
          {!collapsed && (
            <div className={styles.userInfo}>
              <p className={styles.userName}>{user?.name}</p>
              <p className={styles.userEmail}>{user?.email}</p>
            </div>
          )}
        </div>

        <button
          id="logout-btn"
          className={styles.logoutBtn}
          onClick={handleLogout}
          title="Sign out"
        >
          <LogOut size={16} />
          {!collapsed && <span>Sign out</span>}
        </button>
      </div>

      {/* Collapse toggle */}
      <button
        className={styles.collapseBtn}
        onClick={onToggle}
        title={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
        id="sidebar-collapse-btn"
      >
        {collapsed ? <ChevronRight size={16} /> : <ChevronLeft size={16} />}
      </button>
    </aside>
  );
}
