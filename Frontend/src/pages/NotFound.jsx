import { Link } from 'react-router-dom';
import { Button } from '../components/ui';
import Logo from '../components/layout/Logo';
import styles from './NotFound.module.css';

export default function NotFound() {
  return (
    <div className={styles.wrap}>
      <Link to="/" aria-label="SkillCircle home">
        <Logo />
      </Link>
      <p className={styles.code}>404</p>
      <h1 className={styles.title}>Page not found</h1>
      <p className={styles.text}>
        The page you&apos;re looking for doesn&apos;t exist or may have moved.
      </p>
      <Button as={Link} to="/">
        Back home
      </Button>
    </div>
  );
}
