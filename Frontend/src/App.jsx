import { Routes, Route } from 'react-router-dom';

import ProtectedRoute from './routes/ProtectedRoute';
import PublicOnlyRoute from './routes/PublicOnlyRoute';
import AppLayout from './components/layout/AppLayout';

import Landing from './pages/Landing';
import Dashboard from './pages/Dashboard';
import NotFound from './pages/NotFound';
import Login from './pages/auth/Login';
import Register from './pages/auth/Register';
import Callback from './pages/auth/Callback';
import AuthError from './pages/auth/AuthError';

// Phase 8 feature pages
import Profile from './pages/profile/Profile';
import Matches from './pages/matches/Matches';
import Community from './pages/community/Community';
import Projects from './pages/projects/Projects';
import Settings from './pages/settings/Settings';

/**
 * App — route tree.
 *  - Public marketing page at "/"
 *  - Auth pages behind PublicOnlyRoute (signed-in users skip them)
 *  - OAuth callback/error land outside guards (they establish the session)
 *  - The app shell ("/app/*") is gated by ProtectedRoute → AppLayout
 */
export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Landing />} />

      <Route element={<PublicOnlyRoute />}>
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
      </Route>

      <Route path="/auth/callback" element={<Callback />} />
      <Route path="/auth/error" element={<AuthError />} />

      <Route path="/app" element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route index element={<Dashboard />} />
          <Route path="matches" element={<Matches />} />
          <Route path="community" element={<Community />} />
          <Route path="projects" element={<Projects />} />
          <Route path="profile" element={<Profile />} />
          <Route path="settings" element={<Settings />} />
        </Route>
      </Route>

      <Route path="*" element={<NotFound />} />
    </Routes>
  );
}
