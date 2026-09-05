import { BrowserRouter, Routes, Route, Navigate, useOutletContext } from 'react-router-dom';
import { AuthProvider }       from './context/AuthContext';
import { ToastProvider }      from './context/ToastContext';
import ProtectedRoute         from './components/ProtectedRoute';
import LoginPage              from './pages/LoginPage';
import OAuthCallbackPage      from './pages/OAuthCallbackPage';
import DashboardPage          from './pages/DashboardPage';
import OverviewPage           from './pages/OverviewPage';
import EmailsPage             from './pages/EmailsPage';
import SettingsPage           from './pages/SettingsPage';

/** EmailsPage needs the refreshKey from the dashboard layout */
function EmailsPageWrapper() {
  const { refreshKey } = useOutletContext();
  return <EmailsPage refreshKey={refreshKey} />;
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <ToastProvider>
          <Routes>
            {/* ── Public ── */}
            <Route path="/login"           element={<LoginPage />} />
            <Route path="/oauth2/callback" element={<OAuthCallbackPage />} />

            {/* ── Protected dashboard with nested routes ── */}
            <Route
              path="/dashboard"
              element={
                <ProtectedRoute>
                  <DashboardPage />
                </ProtectedRoute>
              }
            >
              <Route index         element={<OverviewPage />} />
              <Route path="emails"   element={<EmailsPageWrapper />} />
              <Route path="settings" element={<SettingsPage />} />
            </Route>

            {/* Catch-all */}
            <Route path="*" element={<Navigate to="/dashboard" replace />} />
          </Routes>
        </ToastProvider>
      </AuthProvider>
    </BrowserRouter>
  );
}
