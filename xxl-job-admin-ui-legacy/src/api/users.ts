import { http } from './http';

type ApiResponse<T> = {
  code: number;
  msg: string;
  data: T;
};

type PageModel<T> = {
  data: T[];
  total: number;
};

export type UserInfo = {
  id: number;
  username: string;
  password?: string | null;
  role: number;
  permission?: string;
};

export type UserQuery = {
  offset: number;
  pagesize: number;
  username: string;
  role: number;
};

export type UserRoleOption = {
  value: number;
  label: string;
};

export type UserMetadata = {
  groups: Array<{
    id: number;
    appname: string;
    title: string;
  }>;
  roles: UserRoleOption[];
};

export async function fetchUsers(params: UserQuery) {
  const { data } = await http.get<ApiResponse<PageModel<UserInfo>>>('/user/pageList', { params });
  return data;
}

export async function fetchUserMetadata() {
  const { data } = await http.get<ApiResponse<UserMetadata>>('/api/admin-next/user-metadata');
  return data;
}

export async function fetchUserDetail(id: number) {
  const { data } = await http.get<ApiResponse<UserInfo>>(`/api/admin-next/users/${id}`);
  return data;
}

export async function createUser(payload: Record<string, string | number>) {
  const form = new URLSearchParams();
  Object.entries(payload).forEach(([key, value]) => {
    form.append(key, String(value));
  });
  const { data } = await http.post<ApiResponse<string>>('/user/insert', form, {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
  });
  return data;
}

export async function updateUser(payload: Record<string, string | number>) {
  const form = new URLSearchParams();
  Object.entries(payload).forEach(([key, value]) => {
    form.append(key, String(value));
  });
  const { data } = await http.post<ApiResponse<string>>('/user/update', form, {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
  });
  return data;
}

export async function deleteUser(id: number) {
  const form = new URLSearchParams();
  form.append('ids[]', String(id));
  const { data } = await http.post<ApiResponse<string>>('/user/delete', form, {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
  });
  return data;
}
