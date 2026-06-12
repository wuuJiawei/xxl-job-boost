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

export type JobInfo = {
  id: number;
  jobGroup: number;
  jobDesc: string;
  author: string;
  jobTag?: string;
  alarmEmail?: string;
  alarmChannelIds?: string;
  alarmEventTypes?: string;
  scheduleType: string;
  scheduleConf: string;
  executorHandler: string;
  executorParam?: string;
  glueType: string;
  triggerStatus: number;
  executorRouteStrategy?: string;
  executorBlockStrategy?: string;
  childJobId?: string;
  executorTimeout?: number;
  executorFailRetryCount?: number;
  misfireStrategy?: string;
  glueSource?: string;
  glueRemark?: string;
};

export type JobQuery = {
  offset: number;
  pagesize: number;
  jobGroup: number;
  triggerStatus: number;
  jobDesc: string;
  executorHandler: string;
  author: string;
  jobTag: string;
};

export async function fetchJobs(params: JobQuery) {
  const { data } = await http.get<ApiResponse<PageModel<JobInfo>>>('/jobinfo/pageList', { params });
  return data;
}

export async function fetchJobDetail(id: number) {
  const { data } = await http.get<ApiResponse<JobInfo>>(`/api/admin-next/jobs/${id}`);
  return data;
}

export async function createJob(payload: Record<string, string>) {
  const form = new URLSearchParams();
  Object.entries(payload).forEach(([key, value]) => {
    form.set(key, value);
  });
  const { data } = await http.post<ApiResponse<string>>('/jobinfo/insert', form, {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
  });
  return data;
}

export async function updateJob(payload: Record<string, string>) {
  const form = new URLSearchParams();
  Object.entries(payload).forEach(([key, value]) => {
    form.set(key, value);
  });
  const { data } = await http.post<ApiResponse<string>>('/jobinfo/update', form, {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
  });
  return data;
}

export async function deleteJob(id: number) {
  const form = new URLSearchParams();
  form.append('ids[]', String(id));
  const { data } = await http.post<ApiResponse<string>>('/jobinfo/delete', form, {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
  });
  return data;
}

export async function startJob(id: number) {
  const form = new URLSearchParams();
  form.append('ids[]', String(id));
  const { data } = await http.post<ApiResponse<string>>('/jobinfo/start', form, {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
  });
  return data;
}

export async function stopJob(id: number) {
  const form = new URLSearchParams();
  form.append('ids[]', String(id));
  const { data } = await http.post<ApiResponse<string>>('/jobinfo/stop', form, {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
  });
  return data;
}

export async function triggerJob(id: number, executorParam: string, addressList: string) {
  const form = new URLSearchParams();
  form.set('id', String(id));
  form.set('executorParam', executorParam);
  form.set('addressList', addressList);
  const { data } = await http.post<ApiResponse<string>>('/jobinfo/trigger', form, {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
  });
  return data;
}

export async function fetchNextTriggerTime(scheduleType: string, scheduleConf: string) {
  const form = new URLSearchParams();
  form.set('scheduleType', scheduleType);
  form.set('scheduleConf', scheduleConf);
  const { data } = await http.post<ApiResponse<string[]>>('/jobinfo/nextTriggerTime', form, {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
  });
  return data;
}
