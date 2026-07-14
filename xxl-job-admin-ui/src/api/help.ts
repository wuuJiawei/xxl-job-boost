import { http } from './http';

type ApiResponse<T> = {
  code: number;
  msg: string;
  data: T;
};

export type HelpInfo = {
  productName: string;
  githubUrl: string;
  documentUrl: string;
  boostGithubUrl: string;
};

export async function fetchHelpInfo() {
  const { data } = await http.get<ApiResponse<HelpInfo>>('/api/admin-next/help');
  return data;
}
