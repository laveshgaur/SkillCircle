import { cn } from '../../lib/cn';
import styles from './PageWrapper.module.css';

/**
 * PageWrapper — consistent page shell for authed views. Provides a max-width
 * column, an optional titled header, and an actions slot (buttons, filters).
 *
 * Props: title, subtitle, actions (node), children.
 */
export default function PageWrapper({ title, subtitle, actions, className, children }) {
  return (
    <div className={cn(styles.page, className)}>
      {(title || actions) && (
        <div className={styles.header}>
          <div className={styles.heading}>
            {title && <h1 className={styles.title}>{title}</h1>}
            {subtitle && <p className={styles.subtitle}>{subtitle}</p>}
          </div>
          {actions && <div className={styles.actions}>{actions}</div>}
        </div>
      )}
      {children}
    </div>
  );
}
