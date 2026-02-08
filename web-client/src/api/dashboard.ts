import apiClient from './client';

export interface DashboardGroup {
  id: number;
  name: string;
  myBalance: number;
}

export interface DashboardResponse {
  totalOwed: number;
  totalOwe: number;
  groups: DashboardGroup[];
}

export async function getDashboard(): Promise<DashboardResponse> {
  const response = await apiClient.get<DashboardResponse>('/api/dashboard');
  return response.data;
}
