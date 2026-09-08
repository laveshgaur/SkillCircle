import { Link, useSearchParams } from 'react-router-dom';
import { AlertTriangle } from 'lucide-react';
import { Button, Card } from '../../components/ui';
import styles from './Auth.module.css';

/**
 * OAuth error landing. The backend redirects here as
 *   /auth/error?message=..
 * when social sign-in fails.
 */
export default function AuthError() {
  const [params] = useSearchParams();
  const message = params.get('message') || 'Something went wrong during sign-in.';

  return (
    <div className={styles.status}>
      <Card padded glass className={styles.statusCard}>
        <span className={`${styles.statusIcon} ${styles.statusIconError}`}>
          <AlertTriangle size={26} aria-hidden="true" />
        </span>
        <h1 className={styles.statusTitle}>Sign-in failed</h1>
        <p className={styles.statusText}>{message}</p>
        <Button as={Link} to="/login" fullWidth>
          Back to sign in
        </Button>
      </Card>
    </div>
  );
}
