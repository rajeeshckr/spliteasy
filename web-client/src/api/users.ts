import apiClient from './client';
import type { User } from './groups';

export async function searchUsers(query: string): Promise<User[]> {
  const response = await apiClient.get<User[]>(`/api/users/search?q=${encodeURIComponent(query)}`);
  return response.data;
}
