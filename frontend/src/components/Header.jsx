import { useState } from 'react';
import { BrainCircuit, RefreshCw } from 'lucide-react';
import { emailsApi } from '../api/emails';
import { useToast } from '../context/ToastContext';
import styles from './Header.module.css';

export default function Header({ title, subtitle, onAnalyzeSuccess, onInsufficientEmails }) {
  const toast = useToast();
  const [analyzing, setAnalyzing] = useState(false);

  const handleAnalyze = async () => {
    setAnalyzing(true);
    try {
      const result = await emailsApi.analyze();
      toast.success(`Analysis complete! ${result.analyzedCount ?? ''} emails processed.`);
      onAnalyzeSuccess?.();
    } catch (err) {
      if (err.status === 422) {
        // Insufficient emails — bubble up to parent to show the modal
        onInsufficientEmails?.(err.extra);
      } else if (err.status === 400) {
        toast.error('Gemini API key not configured. Please add it in Settings.');
      } else {
        toast.error(err.message || 'Analysis failed. Please try again.');
      }
    } finally {
      setAnalyzing(false);
    }
  };

  return (
    <header className={styles.header}>
      <div className={styles.titleBlock}>
        <h1 className={styles.title}>{title}</h1>
        {subtitle && <p className={styles.subtitle}>{subtitle}</p>}
      </div>

      <div className={styles.actions}>
        <button
          id="analyze-emails-btn"
          className={`btn btn-primary ${styles.analyzeBtn}`}
          onClick={handleAnalyze}
          disabled={analyzing}
        >
          {analyzing ? (
            <>
              <div className="spinner" style={{ width: 16, height: 16 }} />
              <span>Analyzing…</span>
            </>
          ) : (
            <>
              <BrainCircuit size={17} />
              <span>Analyze Emails</span>
            </>
          )}
        </button>
      </div>
    </header>
  );
}
