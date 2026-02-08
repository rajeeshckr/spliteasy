import apiClient from './client';
import type { User } from './groups';

export interface ExpenseSplit {
  user: User;
  amountCents: number;
}

export interface Expense {
  id: number;
  description: string;
  amountCents: number;
  paidBy: User;
  splits: ExpenseSplit[];
  createdAt: string;
}

export interface CreateExpenseRequest {
  description: string;
  amountCents: number;
  paidByUserId: number;
}

export async function getExpenses(groupId: number): Promise<Expense[]> {
  const response = await apiClient.get<Expense[]>(`/api/groups/${groupId}/expenses`);
  return response.data;
}

export async function createExpense(groupId: number, data: CreateExpenseRequest): Promise<Expense> {
  const response = await apiClient.post<Expense>(`/api/groups/${groupId}/expenses`, data);
  return response.data;
}
