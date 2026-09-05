import React from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import AuthPage from '../pages/AuthPage';
import ClientDashboardPage from '../pages/ClientDashboardPage';
import FreelancerDashboardPage from '../pages/FreelancerDashboardPage';
import LandingPage from '../pages/LandingPage';
import ProtectedRoute from './ProtectedRoute';

export const AppRoutes: React.FC = () => {
  return (
    <Routes>
      <Route path="/" element={<LandingPage />} />
      <Route path="/auth/:role/:mode" element={<AuthPage />} />
      <Route path="/auth/:role" element={<Navigate to="login" replace />} />
      <Route path="/client/dashboard" element={<ProtectedRoute role="CLIENT"><ClientDashboardPage /></ProtectedRoute>} />
      <Route path="/freelancer/dashboard" element={<ProtectedRoute role="FREELANCER"><FreelancerDashboardPage /></ProtectedRoute>} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
};

export default AppRoutes;
