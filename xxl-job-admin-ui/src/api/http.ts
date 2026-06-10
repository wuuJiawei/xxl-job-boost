import axios from 'axios';

const baseURL =
  import.meta.env.DEV && import.meta.env.VITE_HTTP_PROXY === 'Y'
    ? '/proxy-default'
    : import.meta.env.VITE_SERVICE_BASE_URL;

export const http = axios.create({
  baseURL,
  headers: {
    'X-Requested-With': 'XMLHttpRequest'
  },
  withCredentials: true
});

http.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      window.location.href = `${import.meta.env.BASE_URL}login`;
    }
    return Promise.reject(error);
  }
);
