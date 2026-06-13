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

export type AlarmChannel = {
  id: number;
  name: string;
  type: string;
  endpoint?: string | null;
  recipients?: string | null;
  secret?: string | null;
  headersJson?: string | null;
  enabled: number;
  remark?: string | null;
  updateTime?: string | null;
};

export type AlarmRecord = {
  id: number;
  jobGroup: number;
  jobId: number;
  jobLogId: number;
  jobDesc: string;
  channelId?: number | null;
  channelName: string;
  channelType: string;
  alarmEvent: string;
  target?: string | null;
  alarmTitle: string;
  alarmContent?: string | null;
  sendStatus: number;
  responseCode?: number | null;
  responseBody?: string | null;
  errorMsg?: string | null;
  createTime?: string | null;
};

export type AlarmRule = {
  id: number;
  name: string;
  jobGroup: number;
  jobId?: number | null;
  alarmEvent: string;
  channelIds: string;
  enabled: number;
  remark?: string | null;
  updateTime?: string | null;
};

export async function fetchAlarmChannels(params: {
  offset: number;
  pagesize: number;
  name: string;
  type: string;
  enabled: number;
}) {
  const { data } = await http.get<ApiResponse<PageModel<AlarmChannel>>>('/alarmchannel/pageList', { params });
  return data;
}

export async function createAlarmChannel(payload: Record<string, string>) {
  const form = new URLSearchParams();
  Object.entries(payload).forEach(([key, value]) => form.set(key, value));
  const { data } = await http.post<ApiResponse<string>>('/alarmchannel/insert', form, {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
  });
  return data;
}

export async function updateAlarmChannel(payload: Record<string, string>) {
  const form = new URLSearchParams();
  Object.entries(payload).forEach(([key, value]) => form.set(key, value));
  const { data } = await http.post<ApiResponse<string>>('/alarmchannel/update', form, {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
  });
  return data;
}

export async function deleteAlarmChannel(id: number) {
  const form = new URLSearchParams();
  form.append('ids[]', String(id));
  const { data } = await http.post<ApiResponse<string>>('/alarmchannel/delete', form, {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
  });
  return data;
}

export async function fetchAlarmRules(params: {
  offset: number;
  pagesize: number;
  jobGroup: number;
  jobId: number;
  alarmEvent: string;
  enabled: number;
}) {
  const { data } = await http.get<ApiResponse<PageModel<AlarmRule>>>('/api/admin-next/alarm-rules', { params });
  return data;
}

export async function createAlarmRule(payload: Record<string, string>) {
  const form = new URLSearchParams();
  Object.entries(payload).forEach(([key, value]) => form.set(key, value));
  const { data } = await http.post<ApiResponse<string>>('/alarmrule/insert', form, {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
  });
  return data;
}

export async function updateAlarmRule(payload: Record<string, string>) {
  const form = new URLSearchParams();
  Object.entries(payload).forEach(([key, value]) => form.set(key, value));
  const { data } = await http.post<ApiResponse<string>>('/alarmrule/update', form, {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
  });
  return data;
}

export async function deleteAlarmRule(id: number) {
  const form = new URLSearchParams();
  form.append('ids[]', String(id));
  const { data } = await http.post<ApiResponse<string>>('/alarmrule/delete', form, {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
  });
  return data;
}
