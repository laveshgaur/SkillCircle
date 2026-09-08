import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Mail, Lock, User, AlertCircle } from 'lucide-react';
import { Button, Input } from '../../components/ui';
import { register } from '../../services/authService';
import AuthShell from './AuthShell';
import OAuthButtons from './OAuthButtons';
import styles from './Auth.module.css';

/** Client-side guards mirroring the backend's RegisterRequest constraints. */
function validate({ username, password }) {
  const errors = {};
  if (username && (username.length < 3 || username.length > 100)) {
    errors.username = 'Username must be 3–100 characters.';
  }
  if (password && (password.length < 8 || password.length > 128)) {
    errors.password = 'Password must be at least 8 characters.';
  }
  return errors;
}

export default function Register() {
  const navigate = useNavigate();

  const [form, setForm] = useState({ email: '', username: '', password: '' });
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
    const clientErrors = validate(form);
    if (Object.keys(clientErrors).length > 0) {
      setFieldErrors(clientErrors);
      return;
    }

    setLoading(true);
    setFormError('');
    setFieldErrors({});
    try {
      await register(form);
      navigate('/app', { replace: true });
    } catch (err) {
      if (err.fields) setFieldErrors(err.fields);
      setFormError(err.message || 'Unable to create your account. Please try again.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthShell
      asideTitle="Start building together."
      asideText="Create your profile, connect GitHub, and get matched with developers who share your goals."
    >
      <h1 className={styles.title}>Create account</h1>
      <p className={styles.subtitle}>Join SkillCircle — it&apos;s free to get started.</p>

      {formError && (
        <div className={styles.formError} role="alert">
          <AlertCircle size={16} aria-hidden="true" />
          <span>{formError}</span>
        </div>
      )}

      <form onSubmit={handleSubmit} noValidate>
        <div className={styles.fields}>
          <Input
            type="text"
            label="Username"
            placeholder="yourhandle"
            autoComplete="username"
            required
            leftIcon={<User size={16} />}
            value={form.username}
            onChange={update('username')}
            error={fieldErrors.username}
          />
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
            placeholder="At least 8 characters"
            autoComplete="new-password"
            required
            leftIcon={<Lock size={16} />}
            value={form.password}
            onChange={update('password')}
            error={fieldErrors.password}
            helpText={!fieldErrors.password ? 'Use 8 or more characters.' : undefined}
          />
        </div>

        <Button type="submit" fullWidth size="lg" loading={loading}>
          Create account
        </Button>
      </form>

      <div className={styles.divider}>or continue with</div>
      <OAuthButtons />

      <p className={styles.foot}>
        Already have an account? <Link to="/login">Sign in</Link>
      </p>
    </AuthShell>
  );
}
