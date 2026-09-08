import { createContext, useCallback, useContext, useEffect, useId, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { X, CheckCircle2, AlertTriangle, Info, XCircle } from 'lucide-react';
import { cn } from '../../lib/cn';
import styles from './Toast.module.css';

const ToastContext = createContext(null);

const ICONS = {
  success: CheckCircle2,
  error: XCircle,
  warning: AlertTriangle,
  info: Info,
};

const AUTO_DISMISS_MS = 4500;

/**
 * ToastProvider — wrap the app to enable `useToast()`.
 * Renders a fixed stack of toasts via portal.
 */
export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);
  const timers = useRef(new Map());

  const dismiss = useCallback((id) => {
    setToasts((prev) => prev.map((t) => (t.id === id ? { ...t, exiting: true } : t)));
    // Remove from DOM after exit animation (300ms matches CSS)
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
      if (timers.current.has(id)) {
        clearTimeout(timers.current.get(id));
        timers.current.delete(id);
      }
    }, 300);
  }, []);

  const toast = useCallback(
    ({ type = 'info', title, message, duration = AUTO_DISMISS_MS }) => {
      const id = `toast-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`;
      setToasts((prev) => [...prev, { id, type, title, message, exiting: false }]);
      if (duration > 0) {
        timers.current.set(id, setTimeout(() => dismiss(id), duration));
      }
      return id;
    },
    [dismiss],
  );

  // Convenience helpers
  const success = useCallback((msg, opts) => toast({ type: 'success', message: msg, ...opts }), [toast]);
  const error = useCallback((msg, opts) => toast({ type: 'error', message: msg, duration: 6000, ...opts }), [toast]);
  const warning = useCallback((msg, opts) => toast({ type: 'warning', message: msg, ...opts }), [toast]);
  const info = useCallback((msg, opts) => toast({ type: 'info', message: msg, ...opts }), [toast]);

  return (
    <ToastContext.Provider value={{ toast, success, error, warning, info, dismiss }}>
      {children}
      {createPortal(
        <div className={styles.container} aria-live="polite" aria-label="Notifications">
          {toasts.map((t) => (
            <ToastItem key={t.id} toast={t} onDismiss={() => dismiss(t.id)} />
          ))}
        </div>,
        document.body,
      )}
    </ToastContext.Provider>
  );
}

function ToastItem({ toast: t, onDismiss }) {
  const Icon = ICONS[t.type] || Info;
  return (
    <div
      className={cn(styles.toast, styles[t.type], t.exiting && styles.exit)}
      role="alert"
    >
      <span className={styles.icon}>
        <Icon size={18} aria-hidden="true" />
      </span>
      <div className={styles.content}>
        {t.title && <div className={styles.title}>{t.title}</div>}
        {t.message && <div className={styles.message}>{t.message}</div>}
      </div>
      <button type="button" className={styles.close} onClick={onDismiss} aria-label="Dismiss">
        <X size={14} />
      </button>
    </div>
  );
}

/** Hook — call `toast.success('Saved!')`, `toast.error('Failed')`, etc. */
export function useToast() {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast must be used within <ToastProvider>');
  return ctx;
}
