import { http } from './http';

type ApiResponse<T> = {
  code: number;
  msg: string;
  data: T;
};

export type LegacySyncConfig = {
  sourceUrl: string;
  username: string;
  password: string;
};

export type LegacyJob = {
  id: number;
  groupId: number;
  appname: string;
  groupTitle: string;
  jobDesc: string;
  author: string;
  scheduleType: string;
  scheduleConf: string;
  executorHandler: string;
  executorParam: string;
};

export type LegacySyncPreview = {
  groups: Array<{ id: number; appname: string; title: string; jobCount: number }>;
  jobs: LegacyJob[];
};

export type LegacySyncResult = {
  groupsCreated: number;
  jobsCreated: number;
  jobsSkipped: number;
  warning: string;
};

export async function previewLegacyJobs(config: LegacySyncConfig) {
  const { data } = await http.post<ApiResponse<LegacySyncPreview>>('/api/admin-next/legacy-sync/preview', config);
  return data;
}

export async function importLegacyJobs(config: LegacySyncConfig, jobIds: number[]) {
  const { data } = await http.post<ApiResponse<LegacySyncResult>>('/api/admin-next/legacy-sync/import', {
    ...config,
    jobIds
  });
  return data;
}
