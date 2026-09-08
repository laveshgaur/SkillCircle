import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuthStore, selectIsAuthenticated } from '../store/authStore';

/**
 * ProtectedRoute — gate for authenticated areas. Unauthenticated visitors are
 * redirected to /login, and the attempted path is stored in location state so
 * the login page can send them back after sign-in.
 */
export default function ProtectedRoute() {
  const isAuthenticated = useAuthStore(selectIsAuthenticated);
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }
  return <Outlet />;
}
