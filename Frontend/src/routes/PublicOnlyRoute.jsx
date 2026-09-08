import { Navigate, Outlet } from 'react-router-dom';
import { useAuthStore, selectIsAuthenticated } from '../store/authStore';

/**
 * PublicOnlyRoute — for auth screens (login/register). Signed-in users are
 * bounced to the app so they don't re-authenticate.
 */
export default function PublicOnlyRoute() {
  const isAuthenticated = useAuthStore(selectIsAuthenticated);
  if (isAuthenticated) {
    return <Navigate to="/app" replace />;
  }
  return <Outlet />;
}
