import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

const client = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true, // send session cookie on every request
  headers: {
    'Content-Type': 'application/json',
  },
});

// ── Response Interceptor ─────────────────────────────────────────
// Normalize error shape so consuming code can check error.status
client.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      // Attach a normalized error object
      const apiError = {
        status: error.response.status,
        message:
          error.response.data?.message ||
          error.response.data?.error ||
          'An unexpected error occurred.',
        extra: error.response.data?.extra || null,
      };
      return Promise.reject(apiError);
    }

    if (error.request) {
      return Promise.reject({
        status: 0,
        message: 'Network error – unable to reach the server.',
        extra: null,
      });
    }

    return Promise.reject({
      status: -1,
      message: error.message || 'Unknown error.',
      extra: null,
    });
  }
);

// ── Auth ─────────────────────────────────────────────────────────
export const authApi = {
  /** Get the currently authenticated user */
  me: () => client.get('/api/auth/me').then((r) => r.data),

  /** Trigger logout on the backend (clears session cookie) */
  logout: () => client.get('/api/auth/logout'),

  /** Redirect to Google OAuth2 login (full page redirect) */
  loginWithGoogle: () => {
    window.location.href = `${API_BASE_URL}/oauth2/authorization/google`;
  },
};

export default client;
