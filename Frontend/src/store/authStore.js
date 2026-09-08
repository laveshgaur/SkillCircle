import { create } from 'zustand';
import { persist } from 'zustand/middleware';

/**
 * Authentication store — holds JWT tokens and the current user.
 *
 * Tokens are persisted to localStorage so a page refresh keeps the session.
 * `api.js` reads `accessToken` on every request and calls `setAuth`/`clearAuth`
 * from its refresh interceptor, so this store deliberately imports nothing
 * app-specific (no circular dependency with the API layer).
 */
export const useAuthStore = create(
  persist(
    (set) => ({
      accessToken: null,
      refreshToken: null,
      user: null,

      /** Store the AuthResponse `{ accessToken, refreshToken, user }`. */
      setAuth: (auth) =>
        set({
          accessToken: auth?.accessToken ?? null,
          refreshToken: auth?.refreshToken ?? null,
          // /auth/refresh omits user; keep the existing one in that case.
          ...(auth?.user ? { user: auth.user } : {}),
        }),

      /** Update just the user (e.g. after GET /auth/me). */
      setUser: (user) => set({ user }),

      /** Clear the session. */
      clearAuth: () => set({ accessToken: null, refreshToken: null, user: null }),
    }),
    {
      name: 'skillcircle-auth',
      partialize: (state) => ({
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
        user: state.user,
      }),
    },
  ),
);

/** Selector: is there an active session? */
export const selectIsAuthenticated = (state) => Boolean(state.accessToken);
