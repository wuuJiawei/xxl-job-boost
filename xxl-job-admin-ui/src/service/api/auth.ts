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
    passwordChangeRequired: boolean;
    permissionSet: string[] | null;
  }>({
    url: '/api/admin-next/session'
  });
}

export function updatePassword(oldPassword: string, password: string) {
  const form = new URLSearchParams();
  form.set('oldPassword', oldPassword);
  form.set('password', password);

  return request<string>({
    url: '/auth/updatePwd',
    method: 'post',
    data: form,
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
  });
}
