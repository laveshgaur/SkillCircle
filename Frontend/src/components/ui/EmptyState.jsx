import { cn } from '../../lib/cn';
import { Button } from './';
import styles from './EmptyState.module.css';

/**
 * EmptyState — consistent "no data" placeholder used across all pages.
 *
 * Props: icon (Lucide component), title, description, actionLabel, onAction, actionTo (Link)
 */
export default function EmptyState({
  icon: Icon,
  title,
  description,
  actionLabel,
  onAction,
  actionTo,
  className,
}) {
  return (
    <div className={cn(styles.empty, className)}>
      {Icon && (
        <span className={styles.iconWrap}>
          <Icon size={28} aria-hidden="true" />
        </span>
      )}
      {title && <h3 className={styles.title}>{title}</h3>}
      {description && <p className={styles.description}>{description}</p>}
      {actionLabel && (
        <Button
          onClick={onAction}
          {...(actionTo ? { as: 'a', href: actionTo } : {})}
          size="sm"
        >
          {actionLabel}
        </Button>
      )}
    </div>
  );
}
