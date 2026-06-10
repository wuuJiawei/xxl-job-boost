import { http } from './http';

type ApiResponse<T> = {
  code: number;
  msg: string;
  data: T;
};

export type ChartInfo = {
  triggerDayList: string[];
  triggerDayCountRunningList: number[];
  triggerDayCountSucList: number[];
  triggerDayCountFailList: number[];
  triggerCountRunningTotal: number;
  triggerCountSucTotal: number;
  triggerCountFailTotal: number;
};

export async function fetchChartInfo(startDate: string, endDate: string) {
  const form = new URLSearchParams();
  form.set('startDate', startDate);
  form.set('endDate', endDate);
  const { data } = await http.post<ApiResponse<ChartInfo>>('/chartInfo', form, {
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    }
  });
  return data;
}
