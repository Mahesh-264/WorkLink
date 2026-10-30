import axios from 'axios';

/**
 * Resolves the API base URL.
 * Prefers import.meta.env.VITE_API_BASE_URL.
 * If VITE_API_BASE_URL is not set:
 *   - In development: defaults to '/api' (proxied by Vite to localhost:8080)
 *   - In production: defaults to 'https://worklink-xsac.onrender.com/api'
 *
 * Normalizes the URL so that it always includes the '/api' prefix without trailing slashes.
 */
const getBaseUrl = (): string => {
  const envUrl = import.meta.env.VITE_API_BASE_URL;
  const raw = envUrl && envUrl.trim() !== ''
    ? envUrl.trim()
    : (import.meta.env.DEV ? '/api' : 'https://worklink-xsac.onrender.com/api');

  const trimmed = raw.replace(/\/+$/, '');
  return trimmed.endsWith('/api') ? trimmed : `${trimmed}/api`;
};

const api = axios.create({
  baseURL: getBaseUrl(),
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 15000,
});

/**
 * Request Interceptor:
 * 1. Normalizes endpoint path to avoid duplicate '/api/api/...' prefixes.
 * 2. Strips Authorization header on public endpoints (e.g. registration, login, health)
 *    so stale or expired tokens in storage never break public requests.
 * 3. Injects Bearer token for protected requests when available.
 */
api.interceptors.request.use(
  (config) => {
    // Prevent duplicate '/api' prefix if the request URL begins with '/api'
    if (config.url?.startsWith('/api/')) {
      config.url = config.url.replace(/^\/api/, '');
    }

    const url = config.url || '';
    const isPublicAuthOrHealth =
      url === '/health' ||
      url.startsWith('/health/') ||
      url.startsWith('/auth/register') ||
      url.startsWith('/auth/login');

    if (isPublicAuthOrHealth) {
      if (config.headers) {
        delete config.headers.Authorization;
      }
    } else {
      const token = localStorage.getItem('worklink_token');
      if (token && config.headers) {
        config.headers.Authorization = `Bearer ${token}`;
      }
    }

    return config;
  },
  (error) => Promise.reject(error)
);

export default api;
