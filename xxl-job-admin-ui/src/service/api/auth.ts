import { request } from '../request';

export function fetchLogin(userName: string, password: string) {
  const form = new URLSearchParams();
  form.set('userName', userName);
  form.set('password', password);
  form.set('ifRemember', 'on');

  return request<string>({
    url: '/auth/doLogin',
    method: 'post',
    data: form,
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    }
  });
}

export function fetchGetUserInfo() {
  return request<{
    userId: string;
    userName: string;
    isAdmin: boolean;
    permissionSet: string[] | null;
  }>({
    url: '/api/admin-next/session'
  });
}
