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

export type ExecutorGroup = {
  id: number;
  appname: string;
  title: string;
  addressType: number;
  addressList: string | null;
  registryList?: string[];
  updateTime?: string;
};

export type ExecutorGroupQuery = {
  offset: number;
  pagesize: number;
  appname?: string;
  title?: string;
};

export async function fetchExecutorGroups(params: ExecutorGroupQuery) {
  const { data } = await http.get<ApiResponse<PageModel<ExecutorGroup>>>('/jobgroup/pageList', {
    params
  });
  return data;
}

export type ExecutorGroupPayload = {
  id?: number;
  appname: string;
  title: string;
  addressType: number;
  addressList: string;
};

export async function createExecutorGroup(payload: ExecutorGroupPayload) {
  const form = new URLSearchParams();
  form.set('appname', payload.appname);
  form.set('title', payload.title);
  form.set('addressType', String(payload.addressType));
  form.set('addressList', payload.addressList);
  const { data } = await http.post<ApiResponse<string>>('/jobgroup/insert', form, {
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    }
  });
  return data;
}

export async function updateExecutorGroup(payload: ExecutorGroupPayload) {
  const form = new URLSearchParams();
  form.set('id', String(payload.id));
  form.set('appname', payload.appname);
  form.set('title', payload.title);
  form.set('addressType', String(payload.addressType));
  form.set('addressList', payload.addressList);
  const { data } = await http.post<ApiResponse<string>>('/jobgroup/update', form, {
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    }
  });
  return data;
}

export async function deleteExecutorGroup(ids: number | number[]) {
  const form = new URLSearchParams();
  const values = Array.isArray(ids) ? ids : [ids];
  values.forEach((id) => {
    form.append('ids[]', String(id));
  });
  const { data } = await http.post<ApiResponse<string>>('/jobgroup/delete', form, {
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    }
  });
  return data;
}
