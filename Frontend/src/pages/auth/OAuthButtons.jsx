import { Button } from '../../components/ui';
import { GithubIcon, GoogleIcon } from '../../components/icons/BrandIcons';
import { oauthUrl } from '../../services/authService';
import styles from './Auth.module.css';

/**
 * OAuthButtons — GitHub + Google sign-in. Navigates to the backend's OAuth2
 * authorization endpoint via a full-page redirect (not an XHR).
 */
export default function OAuthButtons() {
  return (
    <div className={styles.oauthRow}>
      <Button
        as="a"
        href={oauthUrl('github')}
        variant="outline"
        leftIcon={<GithubIcon size={18} />}
      >
        GitHub
      </Button>
      <Button
        as="a"
        href={oauthUrl('google')}
        variant="outline"
        leftIcon={<GoogleIcon size={18} />}
      >
        Google
      </Button>
    </div>
  );
}
