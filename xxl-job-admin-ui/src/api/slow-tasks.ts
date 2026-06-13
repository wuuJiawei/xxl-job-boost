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

export type JobSlowAggregate = {
  jobGroup: number;
  jobId: number;
  jobDesc: string;
  author?: string;
  jobTag?: string;
  slowCount: number;
  avgDurationMs: number;
  maxDurationMs: number;
  lastSlowTime: string | null;
  lastLogId: number;
  lastDurationMs: number;
  lastTriggerCode: number;
  lastHandleCode: number;
};

export type JobSlowAggregateQuery = {
  offset: number;
  pagesize: number;
  jobGroup: number;
  jobId: number;
  author: string;
  jobTag: string;
  minDurationSeconds: number;
  filterTime: string;
};

export async function fetchSlowTasks(params: JobSlowAggregateQuery) {
  const { data } = await http.get<ApiResponse<PageModel<JobSlowAggregate>>>('/api/admin-next/slow-tasks', { params });
  return data;
}
