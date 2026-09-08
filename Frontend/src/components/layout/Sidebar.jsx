import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard,
  Sparkles,
  MessagesSquare,
  KanbanSquare,
  UserCircle,
  Settings,
} from 'lucide-react';
import { useUIStore } from '../../store/uiStore';
import { cn } from '../../lib/cn';
import { Button } from '../ui';
import styles from './Sidebar.module.css';

const NAV_ITEMS = [
  { to: '/app', label: 'Dashboard', icon: LayoutDashboard, end: true },
  { to: '/app/matches', label: 'Matches', icon: Sparkles },
  { to: '/app/community', label: 'Community', icon: MessagesSquare },
  { to: '/app/projects', label: 'Projects', icon: KanbanSquare },
  { to: '/app/profile', label: 'Profile', icon: UserCircle },
  { to: '/app/settings', label: 'Settings', icon: Settings },
];

/**
 * Sidebar — primary app navigation. Persistent rail on desktop; slide-in drawer
 * on smaller screens (controlled by uiStore.sidebarOpen). Active route is
 * highlighted via NavLink.
 */
export default function Sidebar() {
  const sidebarOpen = useUIStore((s) => s.sidebarOpen);
  const setSidebar = useUIStore((s) => s.setSidebar);

  return (
    <>
      {sidebarOpen && (
        <div
          className={styles.backdrop}
          onClick={() => setSidebar(false)}
          aria-hidden="true"
        />
      )}
      <aside
        className={cn(styles.sidebar, sidebarOpen && styles.open)}
        aria-label="Main navigation"
      >
        <nav className={styles.group}>
          <span className={styles.groupLabel}>Workspace</span>
          {NAV_ITEMS.map(({ to, label, icon: Icon, end }) => (
            <NavLink
              key={to}
              to={to}
              end={end}
              className={({ isActive }) => cn(styles.item, isActive && styles.active)}
              onClick={() => setSidebar(false)}
            >
              <Icon size={18} aria-hidden="true" />
              {label}
            </NavLink>
          ))}
        </nav>

        <div className={styles.spacer} />

        <div className={styles.footerCard}>
          <p className={styles.footerTitle}>Find your next collaborator</p>
          <p className={styles.footerText}>
            Run the AI matcher to discover developers who fit your goals.
          </p>
          <Button as={NavLink} to="/app/matches" size="sm" fullWidth onClick={() => setSidebar(false)}>
            Find matches
          </Button>
        </div>
      </aside>
    </>
  );
}
