import { cn } from '../../lib/cn';
import styles from './Spinner.module.css';

/**
 * Spinner — indeterminate loading indicator.
 * Pass `center` to wrap it in a padded flex box (e.g. route/data loading).
 */
export default function Spinner({ size = 20, thickness = 2, center = false, label = 'Loading', className }) {
  const el = (
    <span
      className={cn(styles.spinner, className)}
      style={{ width: size, height: size, borderWidth: thickness }}
      role="status"
      aria-label={label}
    />
  );
  return center ? <div className={styles.center}>{el}</div> : el;
}
