import { Link } from 'react-router-dom';
import { useAuthStore, selectIsAuthenticated } from '../../store/authStore';
import { Button } from '../ui';
import Logo from './Logo';
import ThemeToggle from './ThemeToggle';
import styles from './Navbar.module.css';

const SECTION_LINKS = [
  { href: '#features', label: 'Features' },
  { href: '#how', label: 'How it works' },
  { href: '#metrics', label: 'Results' },
];

/**
 * Navbar — top navigation for public / marketing pages. Glass, sticky.
 * Shows section anchors plus auth-aware CTAs (Dashboard when signed in,
 * Log in / Get started otherwise).
 */
export default function Navbar() {
  const isAuthenticated = useAuthStore(selectIsAuthenticated);

  return (
    <header className={styles.nav}>
      <div className={styles.inner}>
        <Link to="/" className={styles.brand} aria-label="SkillCircle home">
          <Logo />
        </Link>

        <nav className={styles.links} aria-label="Sections">
          {SECTION_LINKS.map((l) => (
            <a key={l.href} href={l.href} className={styles.link}>
              {l.label}
            </a>
          ))}
        </nav>

        <div className={styles.actions}>
          <ThemeToggle className={styles.iconBtn} />
          {isAuthenticated ? (
            <Button as={Link} to="/app" size="sm">
              Dashboard
            </Button>
          ) : (
            <>
              <Button as={Link} to="/login" variant="ghost" size="sm">
                Log in
              </Button>
              <Button as={Link} to="/register" size="sm">
                Get started
              </Button>
            </>
          )}
        </div>
      </div>
    </header>
  );
}
