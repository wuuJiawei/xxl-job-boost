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

export type AuditLog = {
  id: number;
  operator: string;
  actionType: string;
  resourceType: string;
  resourceId?: string | null;
  resourceName?: string | null;
  jobGroup?: number | null;
  detailJson?: string | null;
  requestPath?: string | null;
  requestMethod?: string | null;
  source?: string | null;
  clientIp?: string | null;
  createTime: string | null;
};

export type AuditLogQuery = {
  offset: number;
  pagesize: number;
  operator: string;
  actionType: string;
  resourceType: string;
  jobGroup: number;
};

export async function fetchAuditLogs(params: AuditLogQuery) {
  const { data } = await http.get<ApiResponse<PageModel<AuditLog>>>('/api/admin-next/audit-logs', { params });
  return data;
}
