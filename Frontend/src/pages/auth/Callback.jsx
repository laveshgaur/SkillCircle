import { useEffect, useRef } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Spinner } from '../../components/ui';
import { useAuthStore } from '../../store/authStore';
import { fetchMe } from '../../services/authService';
import styles from './Auth.module.css';

/**
 * OAuth callback landing. The backend redirects here as
 *   /auth/callback?access_token=..&refresh_token=..
 * We persist the tokens, hydrate the user, then enter the app. Missing tokens
 * bounce to the error page.
 */
export default function Callback() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const ran = useRef(false);

  useEffect(() => {
    // Guard against React 18 StrictMode double-invoke in dev.
    if (ran.current) return;
    ran.current = true;

    const accessToken = params.get('access_token');
    const refreshToken = params.get('refresh_token');

    if (!accessToken) {
      navigate('/auth/error?message=Missing%20access%20token', { replace: true });
      return;
    }

    useAuthStore.getState().setAuth({ accessToken, refreshToken });
    fetchMe()
      .catch(() => {
        // Non-fatal: tokens are valid; profile can hydrate later in-app.
      })
      .finally(() => navigate('/app', { replace: true }));
  }, [params, navigate]);

  return (
    <div className={styles.status}>
      <Spinner center label="Signing you in…" />
    </div>
  );
}
