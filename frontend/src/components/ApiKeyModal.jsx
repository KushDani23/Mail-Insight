import { useState } from 'react';
import { X, Key, Eye, EyeOff, Trash2, CheckCircle } from 'lucide-react';
import { userApi } from '../api/user';
import { useToast } from '../context/ToastContext';
import styles from './ApiKeyModal.module.css';

export default function ApiKeyModal({ hasKey, onClose, onSaved }) {
  const toast = useToast();
  const [apiKey, setApiKey]       = useState('');
  const [show, setShow]           = useState(false);
  const [saving, setSaving]       = useState(false);
  const [deleting, setDeleting]   = useState(false);

  const handleSave = async (e) => {
    e.preventDefault();
    if (!apiKey.trim()) { toast.error('Please enter your Gemini API key.'); return; }
    setSaving(true);
    try {
      await userApi.saveApiKey(apiKey.trim());
      toast.success('Gemini API key saved successfully!');
      setApiKey('');
      onSaved?.();
      onClose();
    } catch (err) {
      toast.error(err.message || 'Failed to save API key.');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!window.confirm('Remove your Gemini API key? You won\'t be able to analyze emails until you add a new one.')) return;
    setDeleting(true);
    try {
      await userApi.deleteApiKey();
      toast.success('API key removed.');
      onSaved?.();
      onClose();
    } catch (err) {
      toast.error(err.message || 'Failed to remove key.');
    } finally {
      setDeleting(false);
    }
  };

  return (
    <div className={styles.overlay} onClick={onClose}>
      <div className={`glass-card ${styles.modal}`} onClick={(e) => e.stopPropagation()}>
        <button className={styles.closeBtn} onClick={onClose} id="apikey-modal-close">
          <X size={18} />
        </button>

        <div className={styles.iconWrap}>
          <Key size={24} className={styles.icon} />
        </div>

        <div className={styles.header}>
          <h2 className={styles.title}>Gemini API Key</h2>
          <p className={styles.desc}>
            Your key is stored AES-256 encrypted and used only to call the Gemini API on your behalf.
          </p>
        </div>

        {/* Status */}
        <div className={`${styles.statusRow} ${hasKey ? styles.statusActive : styles.statusMissing}`}>
          {hasKey ? (
            <>
              <CheckCircle size={15} />
              <span>API key is configured</span>
            </>
          ) : (
            <>
              <Key size={15} />
              <span>No API key set — add one below</span>
            </>
          )}
        </div>

        <form onSubmit={handleSave} className={styles.form}>
          <div className={styles.inputWrap}>
            <input
              id="gemini-api-key-input"
              className="input"
              type={show ? 'text' : 'password'}
              placeholder={hasKey ? 'Enter new key to replace existing…' : 'AIza…'}
              value={apiKey}
              onChange={(e) => setApiKey(e.target.value)}
              autoComplete="off"
              spellCheck={false}
            />
            <button
              type="button"
              className={styles.eyeBtn}
              onClick={() => setShow((s) => !s)}
              tabIndex={-1}
            >
              {show ? <EyeOff size={16} /> : <Eye size={16} />}
            </button>
          </div>

          <p className={styles.hint}>
            Get your free key at{' '}
            <a href="https://aistudio.google.com/app/apikey" target="_blank" rel="noreferrer">
              Google AI Studio ↗
            </a>
          </p>

          <div className={styles.actions}>
            {hasKey && (
              <button
                type="button"
                className={`btn btn-ghost btn-sm ${styles.deleteBtn}`}
                onClick={handleDelete}
                disabled={deleting}
                id="delete-api-key-btn"
              >
                {deleting ? <div className="spinner" style={{ width: 14, height: 14 }} /> : <Trash2 size={14} />}
                Remove key
              </button>
            )}
            <button
              type="submit"
              className="btn btn-primary btn-sm"
              disabled={saving || !apiKey.trim()}
              id="save-api-key-btn"
              style={{ flex: 1 }}
            >
              {saving ? (
                <><div className="spinner" style={{ width: 14, height: 14 }} /> Saving…</>
              ) : (
                <>{hasKey ? 'Update Key' : 'Save Key'}</>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
