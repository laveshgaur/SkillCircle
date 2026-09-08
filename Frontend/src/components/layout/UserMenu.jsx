import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ChevronDown, UserCircle, Settings, LogOut } from 'lucide-react';
import { useAuthStore } from '../../store/authStore';
import { logout } from '../../services/authService';
import { Avatar } from '../ui';
import styles from './UserMenu.module.css';

/**
 * UserMenu — avatar trigger opening a dropdown with profile/settings/logout.
 * Closes on outside click and Escape.
 */
export default function UserMenu() {
  const user = useAuthStore((s) => s.user);
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const wrapRef = useRef(null);

  useEffect(() => {
    if (!open) return undefined;
    const onDocClick = (e) => {
      if (wrapRef.current && !wrapRef.current.contains(e.target)) setOpen(false);
    };
    const onKey = (e) => e.key === 'Escape' && setOpen(false);
    document.addEventListener('mousedown', onDocClick);
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('mousedown', onDocClick);
      document.removeEventListener('keydown', onKey);
    };
  }, [open]);

  const handleLogout = async () => {
    setOpen(false);
    await logout();
    navigate('/login', { replace: true });
  };

  const displayName = user?.username || user?.email || 'Account';

  return (
    <div className={styles.wrap} ref={wrapRef}>
      <button
        type="button"
        className={styles.trigger}
        onClick={() => setOpen((v) => !v)}
        aria-haspopup="menu"
        aria-expanded={open}
        aria-label="Account menu"
      >
        <Avatar src={user?.avatarUrl} name={displayName} size="sm" />
        <span className={styles.name}>{displayName}</span>
        <ChevronDown size={16} aria-hidden="true" />
      </button>

      {open && (
        <div className={styles.menu} role="menu">
          <div className={styles.header}>
            <div className={styles.headerName}>{displayName}</div>
            {user?.email && <div className={styles.headerEmail}>{user.email}</div>}
          </div>
          <Link to="/app/profile" role="menuitem" className={styles.item} onClick={() => setOpen(false)}>
            <UserCircle size={16} aria-hidden="true" />
            Profile
          </Link>
          <Link to="/app/settings" role="menuitem" className={styles.item} onClick={() => setOpen(false)}>
            <Settings size={16} aria-hidden="true" />
            Settings
          </Link>
          <button
            type="button"
            role="menuitem"
            className={`${styles.item} ${styles.danger}`}
            onClick={handleLogout}
          >
            <LogOut size={16} aria-hidden="true" />
            Log out
          </button>
        </div>
      )}
    </div>
  );
}
