import { cn } from '../../lib/cn';
import styles from './Tabs.module.css';

/**
 * Tabs — horizontal tab switcher.
 *
 * Props:
 *  - tabs: Array<{ id: string, label: string, icon?: ReactNode, badge?: string | number }>
 *  - activeId: the currently selected tab id
 *  - onChange: (id) => void
 */
export default function Tabs({ tabs = [], activeId, onChange, className }) {
  return (
    <div className={cn(styles.tabs, className)} role="tablist">
      {tabs.map((tab) => (
        <button
          key={tab.id}
          type="button"
          role="tab"
          aria-selected={tab.id === activeId}
          className={cn(styles.tab, tab.id === activeId && styles.active)}
          onClick={() => onChange?.(tab.id)}
        >
          {tab.icon && <span className={styles.tabIcon}>{tab.icon}</span>}
          {tab.label}
          {tab.badge != null && <span className={styles.badge}>{tab.badge}</span>}
        </button>
      ))}
    </div>
  );
}
