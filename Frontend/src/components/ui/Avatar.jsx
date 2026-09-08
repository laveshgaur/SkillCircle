import { useState } from 'react';
import { cn } from '../../lib/cn';
import styles from './Avatar.module.css';

/** Derive up-to-2-letter initials from a name or username. */
function initialsFrom(name = '') {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return '?';
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

/**
 * Avatar — user image with a graceful initials fallback.
 *
 * Props: src, name (for initials + alt), size ('xs'|'sm'|'md'|'lg'|'xl'),
 * status ('online'|'busy'|'offline'), ring (brand outline).
 */
export default function Avatar({
  src,
  name = '',
  size = 'md',
  status,
  ring = false,
  className,
  ...props
}) {
  const [failed, setFailed] = useState(false);
  const showImg = src && !failed;

  return (
    <span
      className={cn(styles.avatar, styles[size], ring && styles.ring, className)}
      {...props}
    >
      {showImg ? (
        <img
          className={styles.img}
          src={src}
          alt={name ? `${name}'s avatar` : 'User avatar'}
          onError={() => setFailed(true)}
          loading="lazy"
        />
      ) : (
        <span aria-hidden="true">{initialsFrom(name)}</span>
      )}
      {status && (
        <span
          className={cn(styles.status, styles[status])}
          title={status}
          aria-label={status}
          role="img"
        />
      )}
    </span>
  );
}
