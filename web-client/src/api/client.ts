import axios from 'axios';

const apiClient = axios.create({
  baseURL: localStorage.getItem('serverUrl') || 'http://localhost:9090',
  headers: { 'Content-Type': 'application/json' },
});

// Request interceptor: attach Bearer token
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token && !config.url?.includes('/api/auth/')) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response interceptor: handle 401 (expired token) → logout
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.clear();
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export function updateBaseURL(url: string) {
  apiClient.defaults.baseURL = url;
}

export default apiClient;
