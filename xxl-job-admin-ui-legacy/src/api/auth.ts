import { http } from './http';

type LoginPayload = {
  userName: string;
  password: string;
  ifRemember: boolean;
};

type ApiResponse<T> = {
  code: number;
  msg: string;
  data: T;
};

export async function login(payload: LoginPayload) {
  const form = new URLSearchParams();
  form.set('userName', payload.userName);
  form.set('password', payload.password);
  if (payload.ifRemember) {
    form.set('ifRemember', 'on');
  }

  const { data } = await http.post<ApiResponse<string>>('/auth/doLogin', form, {
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    }
  });

  return data;
}

export async function logout() {
  const { data } = await http.post<ApiResponse<string>>('/auth/logout');
  return data;
}
