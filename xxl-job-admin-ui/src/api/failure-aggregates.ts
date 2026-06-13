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

export type JobFailureAggregate = {
  jobGroup: number;
  jobId: number;
  jobDesc: string;
  author?: string;
  jobTag?: string;
  failureCount: number;
  lastFailureTime: string | null;
  lastLogId: number;
  lastTriggerCode: number;
  lastHandleCode: number;
  lastTriggerMsg?: string | null;
  lastHandleMsg?: string | null;
};

export type JobFailureAggregateQuery = {
  offset: number;
  pagesize: number;
  jobGroup: number;
  jobId: number;
  author: string;
  jobTag: string;
  filterTime: string;
};

export async function fetchFailureAggregates(params: JobFailureAggregateQuery) {
  const { data } = await http.get<ApiResponse<PageModel<JobFailureAggregate>>>('/api/admin-next/failure-aggregates', { params });
  return data;
}
