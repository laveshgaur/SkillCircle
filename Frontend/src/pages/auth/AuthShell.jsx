import { Link } from 'react-router-dom';
import { Sparkles, Users, ShieldCheck } from 'lucide-react';
import Logo from '../../components/layout/Logo';
import styles from './Auth.module.css';

const POINTS = [
  { icon: Sparkles, text: 'AI matching that understands skills and goals' },
  { icon: Users, text: 'A real-time community of builders' },
  { icon: ShieldCheck, text: 'Secure sign-in with rotating sessions' },
];

/**
 * AuthShell — split-panel layout shared by Login and Register. Renders the
 * brand aside (hidden on mobile) and centers the passed form on the right.
 */
export default function AuthShell({ asideTitle, asideText, children }) {
  return (
    <div className={styles.wrap}>
      <aside className={styles.aside}>
        <span className={`${styles.blob} ${styles.blobA}`} aria-hidden="true" />
        <span className={`${styles.blob} ${styles.blobB}`} aria-hidden="true" />

        <Link to="/" className={styles.asideBrand} aria-label="SkillCircle home">
          <Logo />
        </Link>

        <div className={styles.asideBody}>
          <h2 className={styles.asideTitle}>{asideTitle}</h2>
          <p className={styles.asideText}>{asideText}</p>
        </div>

        <ul className={styles.asidePoints}>
          {POINTS.map(({ icon: Icon, text }) => (
            <li key={text} className={styles.asidePoint}>
              <Icon size={18} aria-hidden="true" />
              {text}
            </li>
          ))}
        </ul>
      </aside>

      <div className={styles.panel}>
        <div className={styles.form}>
          <Link to="/" className={styles.mobileBrand} aria-label="SkillCircle home">
            <Logo />
          </Link>
          {children}
        </div>
      </div>
    </div>
  );
}
