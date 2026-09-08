import { useEffect, useState } from 'react';
import { Moon, Sun } from 'lucide-react';
import { useUIStore } from '../../store/uiStore';

/**
 * ThemeToggle — flips between light and dark, persisted via the UI store.
 * Reads the resolved theme after mount to render the correct icon (the store's
 * `theme` may be null = "follow system", so we check the actual <html> attr).
 */
export default function ThemeToggle({ className }) {
  const toggleTheme = useUIStore((s) => s.toggleTheme);
  const theme = useUIStore((s) => s.theme);
  const [isDark, setIsDark] = useState(false);

  useEffect(() => {
    const attr = document.documentElement.getAttribute('data-theme');
    const resolved =
      attr ||
      (window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light');
    setIsDark(resolved === 'dark');
  }, [theme]);

  return (
    <button
      type="button"
      className={className}
      onClick={toggleTheme}
      aria-label={isDark ? 'Switch to light theme' : 'Switch to dark theme'}
      title={isDark ? 'Light mode' : 'Dark mode'}
    >
      {isDark ? <Sun size={18} aria-hidden="true" /> : <Moon size={18} aria-hidden="true" />}
    </button>
  );
}
