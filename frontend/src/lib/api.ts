import axios from 'axios';

/**
 * Reusable Axios API client for WorkLink.
 * Defaults to '/api' using Vite proxy during development,
 * or customizable via VITE_API_BASE_URL.
 */
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 15000,
});

// Adds the access token issued by the backend to protected API calls.
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('worklink_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

export default api;
