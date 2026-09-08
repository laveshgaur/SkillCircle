import { create } from 'zustand';
import { persist } from 'zustand/middleware';

/**
 * Resolve the initial theme: explicit stored choice wins; otherwise fall back
 * to the OS preference at first paint.
 */
function systemPrefersDark() {
  return (
    typeof window !== 'undefined' &&
    window.matchMedia &&
    window.matchMedia('(prefers-color-scheme: dark)').matches
  );
}

/**
 * UI store — theme and layout chrome (sidebar). Only the user's explicit theme
 * choice is persisted; `applyTheme` writes `data-theme` onto <html> so the
 * token stylesheet swaps palettes.
 */
export const useUIStore = create(
  persist(
    (set, get) => ({
      theme: null, // 'light' | 'dark' | null (follow system)
      sidebarOpen: false, // mobile drawer

      applyTheme: () => {
        const { theme } = get();
        const root = document.documentElement;
        if (theme) root.setAttribute('data-theme', theme);
        else root.removeAttribute('data-theme');
      },

      setTheme: (theme) => {
        set({ theme });
        get().applyTheme();
      },

      toggleTheme: () => {
        const current = get().theme ?? (systemPrefersDark() ? 'dark' : 'light');
        get().setTheme(current === 'dark' ? 'light' : 'dark');
      },

      toggleSidebar: () => set((s) => ({ sidebarOpen: !s.sidebarOpen })),
      setSidebar: (open) => set({ sidebarOpen: open }),
    }),
    {
      name: 'skillcircle-ui',
      partialize: (state) => ({ theme: state.theme }),
    },
  ),
);
