import { Link, Outlet } from 'react-router-dom';
import { Menu, Bell } from 'lucide-react';
import { useUIStore } from '../../store/uiStore';
import Logo from './Logo';
import Sidebar from './Sidebar';
import ThemeToggle from './ThemeToggle';
import UserMenu from './UserMenu';
import styles from './AppLayout.module.css';

/**
 * AppLayout — authenticated application shell.
 *
 * Sticky app bar (mobile menu toggle, brand, theme, notifications, user menu),
 * a persistent Sidebar (drawer on mobile), and a routed content area. Feature
 * pages render into <Outlet /> and typically wrap themselves in PageWrapper.
 */
export default function AppLayout() {
  const toggleSidebar = useUIStore((s) => s.toggleSidebar);

  return (
    <div className={styles.shell}>
      <header className={styles.appbar}>
        <button
          type="button"
          className={styles.menuBtn}
          onClick={toggleSidebar}
          aria-label="Toggle navigation"
        >
          <Menu size={20} aria-hidden="true" />
        </button>

        <Link to="/app" className={styles.brand} aria-label="SkillCircle dashboard">
          <Logo />
        </Link>

        <div className={styles.spacer} />

        <div className={styles.appbarActions}>
          <ThemeToggle className={styles.iconBtn} />
          <button type="button" className={styles.iconBtn} aria-label="Notifications">
            <Bell size={18} aria-hidden="true" />
          </button>
          <UserMenu />
        </div>
      </header>

      <div className={styles.body}>
        <Sidebar />
        <main className={styles.main}>
          <Outlet />
        </main>
      </div>
    </div>
  );
}
