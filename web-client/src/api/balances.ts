import apiClient from './client';
import type { User } from './groups';

export interface Balance {
  fromUser: User;
  toUser: User;
  amountCents: number;
}

export interface BalancesResponse {
  groupName: string;
  balances: Balance[];
}

export interface SettleRequest {
  fromUserId: number;
  toUserId: number;
  amountCents: number;
}

export async function getBalances(groupId: number): Promise<BalancesResponse> {
  const response = await apiClient.get<BalancesResponse>(`/api/groups/${groupId}/balances`);
  return response.data;
}

export async function settle(groupId: number, data: SettleRequest): Promise<{ message: string }> {
  const response = await apiClient.post<{ message: string }>(`/api/groups/${groupId}/settle`, data);
  return response.data;
}
