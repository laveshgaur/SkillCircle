import { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { Mail, Lock, AlertCircle } from 'lucide-react';
import { Button, Input } from '../../components/ui';
import { login } from '../../services/authService';
import AuthShell from './AuthShell';
import OAuthButtons from './OAuthButtons';
import styles from './Auth.module.css';

export default function Login() {
  const navigate = useNavigate();
  const location = useLocation();
  const from = location.state?.from?.pathname || '/app';

  const [form, setForm] = useState({ email: '', password: '' });
  const [fieldErrors, setFieldErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [loading, setLoading] = useState(false);

  const update = (key) => (e) => {
    setForm((f) => ({ ...f, [key]: e.target.value }));
    setFieldErrors((fe) => ({ ...fe, [key]: undefined }));
    setFormError('');
  };

  async function handleSubmit(e) {
    e.preventDefault();
    setLoading(true);
    setFormError('');
    setFieldErrors({});
    try {
      await login(form);
      navigate(from, { replace: true });
    } catch (err) {
      if (err.fields) setFieldErrors(err.fields);
      setFormError(err.message || 'Unable to sign in. Please try again.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthShell
      asideTitle="Welcome back."
      asideText="Sign in to pick up where you left off — your matches, spaces, and projects are waiting."
    >
      <h1 className={styles.title}>Sign in</h1>
      <p className={styles.subtitle}>Continue to your SkillCircle account.</p>

      {formError && (
        <div className={styles.formError} role="alert">
          <AlertCircle size={16} aria-hidden="true" />
          <span>{formError}</span>
        </div>
      )}

      <form onSubmit={handleSubmit} noValidate>
        <div className={styles.fields}>
          <Input
            type="email"
            label="Email"
            placeholder="you@example.com"
            autoComplete="email"
            required
            leftIcon={<Mail size={16} />}
            value={form.email}
            onChange={update('email')}
            error={fieldErrors.email}
          />
          <Input
            type="password"
            label="Password"
            placeholder="••••••••"
            autoComplete="current-password"
            required
            leftIcon={<Lock size={16} />}
            value={form.password}
            onChange={update('password')}
            error={fieldErrors.password}
          />
        </div>

        <Button type="submit" fullWidth size="lg" loading={loading}>
          Sign in
        </Button>
      </form>

      <div className={styles.divider}>or continue with</div>
      <OAuthButtons />

      <p className={styles.foot}>
        Don&apos;t have an account? <Link to="/register">Create one</Link>
      </p>
    </AuthShell>
  );
}
