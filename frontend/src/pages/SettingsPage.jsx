import { useState, useEffect } from 'react';
import { Key, Link2, Trash2, Plus, ExternalLink } from 'lucide-react';
import { userApi, accountsApi } from '../api/user';
import { useToast } from '../context/ToastContext';
import ApiKeyModal from '../components/ApiKeyModal';
import styles from './SettingsPage.module.css';

export default function SettingsPage() {
  const toast = useToast();
  const [hasKey, setHasKey]         = useState(false);
  const [keyLoading, setKeyLoading] = useState(true);
  const [accounts, setAccounts]     = useState([]);
  const [accLoading, setAccLoading] = useState(true);
  const [showKeyModal, setShowKeyModal] = useState(false);
  const [connecting, setConnecting]    = useState(false);
  const [disconnecting, setDisconnecting] = useState(null);

  const loadKeyStatus = () => {
    setKeyLoading(true);
    userApi.getApiKeyStatus()
      .then((d) => setHasKey(d.hasKey ?? false))
      .catch(() => {})
      .finally(() => setKeyLoading(false));
  };

  const loadAccounts = () => {
    setAccLoading(true);
    accountsApi.list()
      .then(setAccounts)
      .catch(() => {})
      .finally(() => setAccLoading(false));
  };

  useEffect(() => {
    loadKeyStatus();
    loadAccounts();
  }, []);

  const handleConnect = async () => {
    setConnecting(true);
    try {
      const { authUrl } = await accountsApi.getConnectUrl();
      window.location.href = authUrl;
    } catch (err) {
      toast.error(err.message || 'Could not get connect URL.');
      setConnecting(false);
    }
  };

  const handleDisconnect = async (id, email) => {
    if (!window.confirm(`Disconnect ${email}?`)) return;
    setDisconnecting(id);
    try {
      await accountsApi.disconnect(id);
      toast.success(`${email} disconnected.`);
      loadAccounts();
    } catch (err) {
      toast.error(err.message || 'Failed to disconnect account.');
    } finally {
      setDisconnecting(null);
    }
  };

  return (
    <div className={styles.wrapper}>
      {/* ── Gemini API Key ── */}
      <section className={`glass-card ${styles.section}`}>
        <div className={styles.sectionHeader}>
          <div className={styles.sectionIcon} style={{ background: 'rgba(124,92,252,0.12)', borderColor: 'rgba(124,92,252,0.3)' }}>
            <Key size={20} style={{ color: 'var(--color-primary-light)' }} />
          </div>
          <div>
            <h2 className={styles.sectionTitle}>Gemini API Key</h2>
            <p className={styles.sectionDesc}>
              Required for AI analysis. Stored AES-256 encrypted.{' '}
              <a href="https://aistudio.google.com/app/apikey" target="_blank" rel="noreferrer">
                Get a free key <ExternalLink size={11} style={{ display: 'inline', verticalAlign: 'middle' }} />
              </a>
            </p>
          </div>
        </div>

        <div className={styles.keyRow}>
          {keyLoading ? (
            <div className="skeleton" style={{ width: 140, height: 16, borderRadius: 6 }} />
          ) : (
            <div className={`${styles.keyStatus} ${hasKey ? styles.keyStatusActive : styles.keyStatusMissing}`}>
              {hasKey ? '✓ API key configured' : '✗ No API key set'}
            </div>
          )}
          <button
            id="open-apikey-modal-btn"
            className="btn btn-primary btn-sm"
            onClick={() => setShowKeyModal(true)}
          >
            <Key size={14} />
            {hasKey ? 'Update Key' : 'Add Key'}
          </button>
        </div>
      </section>

      {/* ── Connected Accounts ── */}
      <section className={`glass-card ${styles.section}`}>
        <div className={styles.sectionHeader}>
          <div className={styles.sectionIcon} style={{ background: 'rgba(79,195,247,0.1)', borderColor: 'rgba(79,195,247,0.25)' }}>
            <Link2 size={20} style={{ color: 'var(--color-accent)' }} />
          </div>
          <div>
            <h2 className={styles.sectionTitle}>Connected Gmail Accounts</h2>
            <p className={styles.sectionDesc}>
              Add secondary Gmail accounts to sync and analyze across multiple inboxes.
            </p>
          </div>
        </div>

        {accLoading ? (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            {[1,2].map(i => <div key={i} className="skeleton" style={{ height: 50, borderRadius: 10 }} />)}
          </div>
        ) : accounts.length === 0 ? (
          <div className={styles.emptyAccounts}>
            <Link2 size={28} style={{ color: 'var(--text-muted)', opacity: 0.5 }} />
            <p>No additional accounts connected</p>
          </div>
        ) : (
          <div className={styles.accountsList}>
            {accounts.map((acc) => (
              <div key={acc.id} className={styles.accountRow}>
                <div className={styles.accountAvatar}>
                  {acc.gmailAddress?.[0]?.toUpperCase() ?? 'G'}
                </div>
                <div className={styles.accountInfo}>
                  <p className={styles.accountEmail}>{acc.gmailAddress}</p>
                </div>
                <button
                  className={`btn btn-ghost btn-sm ${styles.disconnectBtn}`}
                  onClick={() => handleDisconnect(acc.id, acc.gmailAddress)}
                  disabled={disconnecting === acc.id}
                  id={`disconnect-account-${acc.id}`}
                >
                  {disconnecting === acc.id
                    ? <div className="spinner" style={{ width: 13, height: 13 }} />
                    : <Trash2 size={13} />}
                  Disconnect
                </button>
              </div>
            ))}
          </div>
        )}

        <button
          id="connect-account-btn"
          className="btn btn-ghost btn-sm"
          style={{ marginTop: 12, alignSelf: 'flex-start', display: 'flex', gap: 6, alignItems: 'center' }}
          onClick={handleConnect}
          disabled={connecting}
        >
          {connecting
            ? <div className="spinner" style={{ width: 14, height: 14 }} />
            : <Plus size={14} />}
          Connect another Gmail account
        </button>
      </section>

      {showKeyModal && (
        <ApiKeyModal
          hasKey={hasKey}
          onClose={() => setShowKeyModal(false)}
          onSaved={loadKeyStatus}
        />
      )}
    </div>
  );
}
