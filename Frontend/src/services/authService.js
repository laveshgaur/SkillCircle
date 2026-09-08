import api, { unwrap, API_BASE_URL } from '../lib/api';
import { useAuthStore } from '../store/authStore';

/**
 * Auth service — wraps the `/api/v1/auth/*` endpoints and keeps the auth store
 * in sync. All methods return the unwrapped payload and throw a normalized
 * Error (with `.status` and `.fields`) on failure.
 */

/** POST /auth/register — create an account and start a session. */
export async function register({ email, username, password }) {
  const auth = await unwrap(
    api.post('/auth/register', { email, username, password }),
  );
  useAuthStore.getState().setAuth(auth);
  return auth;
}

/** POST /auth/login — email/password sign-in. */
export async function login({ email, password }) {
  const auth = await unwrap(api.post('/auth/login', { email, password }));
  useAuthStore.getState().setAuth(auth);
  return auth;
}

/** POST /auth/logout — best-effort server invalidation, always clears locally. */
export async function logout() {
  const { refreshToken, clearAuth } = useAuthStore.getState();
  try {
    await api.post('/auth/logout', refreshToken ? { refreshToken } : {});
  } catch {
    // Ignore — the session is being torn down regardless.
  } finally {
    clearAuth();
  }
}

/** GET /auth/me — refresh the current user into the store. */
export async function fetchMe() {
  const user = await unwrap(api.get('/auth/me'));
  useAuthStore.getState().setUser(user);
  return user;
}

/**
 * Backend origin (without the `/api/v1` suffix) — OAuth2 authorize endpoints
 * live at the server root, not under the versioned API.
 */
const API_ORIGIN = API_BASE_URL.replace(/\/api\/v1\/?$/, '');

/** Full URL to kick off an OAuth2 login for the given provider. */
export function oauthUrl(provider) {
  return `${API_ORIGIN}/oauth2/authorization/${provider}`;
}
