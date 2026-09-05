import { createContext, useContext, useState, useCallback, useRef } from 'react';
import { CheckCircle, XCircle, Info, AlertTriangle, X } from 'lucide-react';

const ToastContext = createContext(null);

const ICONS = {
  success: CheckCircle,
  error: XCircle,
  info: Info,
  warning: AlertTriangle,
};

const COLORS = {
  success: 'var(--color-success)',
  error: 'var(--color-danger)',
  info: 'var(--color-primary-light)',
  warning: 'var(--color-warning)',
};

function ToastItem({ id, type = 'info', message, onRemove }) {
  const Icon = ICONS[type];
  return (
    <div className={`toast toast-${type}`} role="alert">
      <Icon size={18} style={{ color: COLORS[type], flexShrink: 0 }} />
      <span style={{ flex: 1, fontSize: '0.88rem' }}>{message}</span>
      <button
        onClick={() => onRemove(id)}
        style={{
          background: 'none',
          border: 'none',
          cursor: 'pointer',
          color: 'var(--text-muted)',
          padding: '2px',
          display: 'flex',
          alignItems: 'center',
          borderRadius: '4px',
          transition: 'color var(--transition-fast)',
        }}
        onMouseEnter={(e) => (e.currentTarget.style.color = 'var(--text-primary)')}
        onMouseLeave={(e) => (e.currentTarget.style.color = 'var(--text-muted)')}
      >
        <X size={14} />
      </button>
    </div>
  );
}

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);
  const timersRef = useRef({});

  const remove = useCallback((id) => {
    clearTimeout(timersRef.current[id]);
    delete timersRef.current[id];
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const addToast = useCallback(
    ({ type = 'info', message, duration = 4000 }) => {
      const id = Date.now().toString();
      setToasts((prev) => [...prev, { id, type, message }]);
      if (duration > 0) {
        timersRef.current[id] = setTimeout(() => remove(id), duration);
      }
      return id;
    },
    [remove]
  );

  const toast = {
    success: (msg, opts) => addToast({ type: 'success', message: msg, ...opts }),
    error: (msg, opts) => addToast({ type: 'error', message: msg, ...opts }),
    info: (msg, opts) => addToast({ type: 'info', message: msg, ...opts }),
    warning: (msg, opts) => addToast({ type: 'warning', message: msg, ...opts }),
  };

  return (
    <ToastContext.Provider value={toast}>
      {children}
      <div className="toast-container" aria-live="polite">
        {toasts.map((t) => (
          <ToastItem key={t.id} {...t} onRemove={remove} />
        ))}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast() {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast must be used inside <ToastProvider>');
  return ctx;
}
