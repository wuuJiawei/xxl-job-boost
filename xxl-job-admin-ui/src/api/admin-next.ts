import { http } from './http';

type ApiResponse<T> = {
  code: number;
  msg: string;
  data: T;
};

export type SessionInfo = {
  userId: string;
  userName: string;
  permissionSet: string[];
  isAdmin: boolean;
};

export type DashboardSummary = {
  jobInfoCount: number;
  jobLogCount: number;
  jobLogSuccessCount: number;
  executorCount: number;
};

export type JobGroupOption = {
  id: number;
  appname: string;
  title: string;
  addressType: number;
  addressList: string | null;
  registryList?: string[];
};

export type JobOption = {
  id: number;
  jobGroup: number;
  jobDesc: string;
  executorHandler: string;
};

export type LogDetailMeta = {
  id: number;
  jobId: number;
  jobGroup: number;
  jobDesc: string;
  triggerCode: number;
  handleCode: number;
  triggerTime: string | null;
  handleTime: string | null;
  executorAddress: string | null;
};

export type MetadataOption = {
  label: string;
  value: string;
};

export type AlarmChannelOption = {
  id: number;
  name: string;
  type: string;
  endpoint?: string | null;
  recipients?: string | null;
  headersJson?: string | null;
  enabled: number;
  remark?: string | null;
};

export type AlarmRuleDetail = {
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

export type JobMetadata = {
  scheduleTypes: MetadataOption[];
  routeStrategies: MetadataOption[];
  misfireStrategies: MetadataOption[];
  blockStrategies: MetadataOption[];
  glueTypes: MetadataOption[];
  alarmChannelTypes: MetadataOption[];
  alarmEventTypes: MetadataOption[];
  alarmChannels: AlarmChannelOption[];
};

export async function fetchSession() {
  const { data } = await http.get<ApiResponse<SessionInfo>>('/api/admin-next/session');
  return data;
}

export async function fetchDashboardSummary() {
  const { data } = await http.get<ApiResponse<DashboardSummary>>('/api/admin-next/dashboard/summary');
  return data;
}

export async function fetchJobGroups() {
  const { data } = await http.get<ApiResponse<JobGroupOption[]>>('/api/admin-next/jobgroups');
  return data;
}

export async function fetchJobsByGroup(jobGroupId: number) {
  const { data } = await http.get<ApiResponse<JobOption[]>>(`/api/admin-next/jobgroups/${jobGroupId}/jobs`);
  return data;
}

export async function fetchLogDetailMeta(logId: number) {
  const { data } = await http.get<ApiResponse<LogDetailMeta>>(`/api/admin-next/logs/${logId}`);
  return data;
}

export async function fetchJobMetadata() {
  const { data } = await http.get<ApiResponse<JobMetadata>>('/api/admin-next/job-metadata');
  return data;
}

export async function fetchAlarmChannelDetail(id: number) {
  const { data } = await http.get<ApiResponse<AlarmChannelOption>>(`/api/admin-next/alarm-channels/${id}`);
  return data;
}

export async function fetchAlarmRecords(params: {
  offset: number;
  pagesize: number;
  jobGroup: number;
  channelType: string;
  sendStatus: number;
}) {
  const { data } = await http.get<ApiResponse<{ data: unknown[]; total: number }>>('/api/admin-next/alarm-records', { params });
  return data;
}

export async function fetchAlarmRuleDetail(id: number) {
  const { data } = await http.get<ApiResponse<AlarmRuleDetail>>(`/api/admin-next/alarm-rules/${id}`);
  return data;
}
