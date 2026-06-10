import axios from 'axios';

export const http = axios.create({
  baseURL: '/xxl-job-admin',
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
