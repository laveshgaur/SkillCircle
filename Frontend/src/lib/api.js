import axios from 'axios';
import { useAuthStore } from '../store/authStore';

/**
 * Base URL for the SkillCircle backend. Points at the Spring Boot API's
 * versioned root. Override in a `.env` with `VITE_API_URL` for other envs.
 */
export const API_BASE_URL =
  import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  timeout: 20000,
});

/* --------------------------------------------------------------------------
 * Request interceptor — attach the current access token.
 * ------------------------------------------------------------------------ */
api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

/* --------------------------------------------------------------------------
 * Response interceptor — single-flight refresh on 401.
 *
 * Concurrent requests that all 401 share one in-flight refresh call; each is
 * queued and replayed with the new token once the refresh resolves. If refresh
 * fails, auth is cleared (ProtectedRoute then redirects to /login).
 * ------------------------------------------------------------------------ */
let isRefreshing = false;
let pendingQueue = [];

const flushQueue = (error, token = null) => {
  pendingQueue.forEach(({ resolve, reject }) => {
    if (error) reject(error);
    else resolve(token);
  });
  pendingQueue = [];
};

// Endpoints where a 401 is an expected outcome, not a token-expiry we can fix.
const NO_RETRY = ['/auth/login', '/auth/register', '/auth/refresh'];

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const { config, response } = error;
    const originalRequest = config || {};

    if (
      !response ||
      response.status !== 401 ||
      originalRequest._retry ||
      NO_RETRY.some((p) => (originalRequest.url || '').includes(p))
    ) {
      return Promise.reject(error);
    }

    const { refreshToken, setAuth, clearAuth } = useAuthStore.getState();
    if (!refreshToken) {
      clearAuth();
      return Promise.reject(error);
    }

    if (isRefreshing) {
      // Wait for the in-flight refresh, then replay this request.
      return new Promise((resolve, reject) => {
        pendingQueue.push({ resolve, reject });
      })
        .then((token) => {
          originalRequest._retry = true;
          originalRequest.headers.Authorization = `Bearer ${token}`;
          return api(originalRequest);
        })
        .catch((err) => Promise.reject(err));
    }

    originalRequest._retry = true;
    isRefreshing = true;

    try {
      // Bare axios call (no interceptors) to avoid recursion.
      const { data } = await axios.post(
        `${API_BASE_URL}/auth/refresh`,
        { refreshToken },
        { headers: { 'Content-Type': 'application/json' } },
      );
      const auth = data?.data ?? data;
      setAuth(auth);
      flushQueue(null, auth.accessToken);
      originalRequest.headers.Authorization = `Bearer ${auth.accessToken}`;
      return api(originalRequest);
    } catch (refreshError) {
      flushQueue(refreshError, null);
      clearAuth();
      return Promise.reject(refreshError);
    } finally {
      isRefreshing = false;
    }
  },
);

/**
 * Unwrap the backend's ApiResponse envelope `{ success, message, data }`,
 * returning `data`. Throws a normalized Error carrying the server message
 * (and validation `fields`, when present) for anything else.
 */
export function unwrap(promise) {
  return promise.then(
    (res) => res.data?.data ?? res.data,
    (error) => {
      const payload = error.response?.data;
      const message =
        payload?.message ||
        error.message ||
        'Something went wrong. Please try again.';
      const normalized = new Error(message);
      normalized.status = error.response?.status;
      normalized.fields = payload?.errors || payload?.fields || null;
      throw normalized;
    },
  );
}

export default api;
