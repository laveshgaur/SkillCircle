import { cn } from '../../lib/cn';
import styles from './ProgressBar.module.css';

/**
 * ProgressBar — animated fill bar.
 *
 * Props:
 *  - value: 0–100
 *  - size: 'sm' | 'md' | 'lg'
 *  - showLabel: boolean (show "XX%")
 *  - variant: 'brand' | 'success' | 'warning' | 'danger'
 */
export default function ProgressBar({
  value = 0,
  size = 'md',
  showLabel = false,
  variant = 'brand',
  className,
}) {
  const clamped = Math.max(0, Math.min(100, value));
  return (
    <div className={cn(styles.wrap, styles[size], className)} role="progressbar" aria-valuenow={clamped} aria-valuemin={0} aria-valuemax={100}>
      <div className={styles.track}>
        <div
          className={cn(styles.fill, styles[variant])}
          style={{ width: `${clamped}%` }}
        />
      </div>
      {showLabel && <span className={styles.label}>{Math.round(clamped)}%</span>}
    </div>
  );
}
