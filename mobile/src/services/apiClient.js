import axios from 'axios';

const API_BASE_URL = process.env.EXPO_PUBLIC_API_URL;

if (!API_BASE_URL) {
  console.warn('EXPO_PUBLIC_API_URL nije postavljen. Kopiraj mobile/.env.example u mobile/.env.');
}

let authToken = null;
let onUnauthorized = null;

export const setAuthToken = (token) => { authToken = token; };
export const setOnUnauthorized = (callback) => { onUnauthorized = callback; };

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

apiClient.interceptors.request.use((config) => {
  if (authToken) {
    config.headers.Authorization = `Bearer ${authToken}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && authToken && onUnauthorized) {
      onUnauthorized();
    }
    return Promise.reject(error);
  },
);

export default apiClient;