import { cn } from '../../lib/cn';
import styles from './Badge.module.css';

/**
 * Badge — compact status/label pill.
 *
 * Props:
 *  - variant: 'default' | 'brand' | 'success' | 'warning' | 'danger' | 'info' | 'outline'
 *  - size: 'sm' | 'md'
 *  - dot: show a leading status dot
 */
export default function Badge({
  variant = 'default',
  size = 'md',
  dot = false,
  className,
  children,
  ...props
}) {
  return (
    <span
      className={cn(styles.badge, styles[variant], styles[size], className)}
      {...props}
    >
      {dot && <span className={styles.dot} aria-hidden="true" />}
      {children}
    </span>
  );
}
