import { http } from './http';

type ApiResponse<T> = {
  code: number;
  msg: string;
  data: T;
};

export type GovernanceFailureAggregate = {
  jobGroup: number;
  jobId: number;
  jobDesc: string;
  author?: string | null;
  jobTag?: string | null;
  failureCount: number;
  lastFailureTime: string | null;
  lastLogId: number;
  lastTriggerCode: number;
  lastHandleCode: number;
  lastTriggerMsg?: string | null;
  lastHandleMsg?: string | null;
};

export type GovernanceSlowAggregate = {
  jobGroup: number;
  jobId: number;
  jobDesc: string;
  author?: string | null;
  jobTag?: string | null;
  slowCount: number;
  avgDurationMs: number;
  maxDurationMs: number;
  lastSlowTime: string | null;
  lastLogId: number;
  lastDurationMs: number;
  lastTriggerCode: number;
  lastHandleCode: number;
};

export type GovernanceAuditLog = {
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

export type GovernanceOverview = {
  totalJobs: number;
  taggedJobs: number;
  ownedJobs: number;
  auditEvents: number;
  failureTopList: GovernanceFailureAggregate[];
  slowTopList: GovernanceSlowAggregate[];
  recentAuditList: GovernanceAuditLog[];
};

export async function fetchGovernanceOverview() {
  const { data } = await http.get<ApiResponse<GovernanceOverview>>('/api/admin-next/governance/overview');
  return data;
}
