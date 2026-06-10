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

export type JobLog = {
  id: number;
  jobGroup: number;
  jobId: number;
  triggerTime: string | null;
  triggerCode: number;
  triggerMsg: string;
  handleTime: string | null;
  handleCode: number;
  handleMsg: string;
  executorAddress: string | null;
};

export type JobLogQuery = {
  offset: number;
  pagesize: number;
  jobGroup: number;
  jobId: number;
  logStatus: number;
  filterTime: string;
};

export type LogChunk = {
  fromLineNum: number;
  toLineNum: number;
  logContent: string;
  end: boolean;
};

export async function fetchLogs(params: JobLogQuery) {
  const { data } = await http.get<ApiResponse<PageModel<JobLog>>>('/joblog/pageList', { params });
  return data;
}

export async function killLog(id: number) {
  const form = new URLSearchParams();
  form.set('id', String(id));
  const { data } = await http.post<ApiResponse<string>>('/joblog/logKill', form, {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
  });
  return data;
}

export async function fetchLogChunk(logId: number, fromLineNum: number) {
  const form = new URLSearchParams();
  form.set('logId', String(logId));
  form.set('fromLineNum', String(fromLineNum));
  const { data } = await http.post<ApiResponse<LogChunk>>('/joblog/logDetailCat', form, {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
  });
  return data;
}
