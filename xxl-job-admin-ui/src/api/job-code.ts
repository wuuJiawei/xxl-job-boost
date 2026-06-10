import { http } from './http';

type ApiResponse<T> = {
  code: number;
  msg: string;
  data: T;
};

export type JobCodeVersion = {
  id: number;
  glueType: string;
  glueTypeLabel: string;
  glueRemark: string;
  glueSource: string;
  updatedAt: string | null;
};

export type JobCodeDetail = {
  jobId: number;
  jobDesc: string;
  glueType: string;
  glueTypeLabel: string;
  currentVersion: JobCodeVersion;
  historyVersions: JobCodeVersion[];
};

export async function fetchJobCodeDetail(jobId: number) {
  const { data } = await http.get<ApiResponse<JobCodeDetail>>(`/api/admin-next/jobs/${jobId}/code`);
  return data;
}

export async function saveJobCode(jobId: number, glueSource: string, glueRemark: string) {
  const form = new URLSearchParams();
  form.set('glueSource', glueSource);
  form.set('glueRemark', glueRemark);
  const { data } = await http.post<ApiResponse<string>>(`/api/admin-next/jobs/${jobId}/code`, form, {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
  });
  return data;
}
