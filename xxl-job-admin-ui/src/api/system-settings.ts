import { http } from './http';

type ApiResponse<T> = {
  code: number;
  msg: string;
  data: T;
};

export type EmailSettings = {
  enabled: boolean;
  host: string;
  port: number;
  username: string;
  from: string;
  password: string;
  passwordConfigured: boolean;
  smtpAuth: boolean;
  starttlsEnabled: boolean;
  starttlsRequired: boolean;
  sslEnabled: boolean;
};

export async function fetchEmailSettings() {
  const { data } = await http.get<ApiResponse<EmailSettings>>('/api/admin-next/system-settings/email');
  return data;
}

export async function updateEmailSettings(payload: EmailSettings) {
  const { data } = await http.post<ApiResponse<EmailSettings>>('/api/admin-next/system-settings/email', payload);
  return data;
}
