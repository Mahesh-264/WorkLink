import type { ReactNode } from 'react';
import { Navigate } from 'react-router-dom';
import { clearSession, dashboardPath, getStoredUser, hasSession, type UserRole } from '../lib/auth';

interface ProtectedRouteProps {
  role: UserRole;
  children: ReactNode;
}

export default function ProtectedRoute({ role, children }: ProtectedRouteProps) {
  if (!hasSession()) {
    clearSession();
    return <Navigate to={`/auth/${role === 'CLIENT' ? 'client' : 'freelancer'}/login`} replace />;
  }

  const user = getStoredUser();
  if (!user) {
    clearSession();
    return <Navigate to="/" replace />;
  }

  return user.role === role ? <>{children}</> : <Navigate to={dashboardPath(user.role)} replace />;
}
