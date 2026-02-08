import apiClient from './client';

export interface User {
  id: number;
  username: string;
  email: string;
}

export interface Group {
  id: number;
  name: string;
  description: string;
  members: User[];
  creatorId: number;
}

export interface CreateGroupRequest {
  name: string;
  description?: string;
}

export interface AddMemberRequest {
  userId: number;
}

export async function createGroup(data: CreateGroupRequest): Promise<Group> {
  const response = await apiClient.post<Group>('/api/groups', data);
  return response.data;
}

export async function getGroup(id: number): Promise<Group> {
  const response = await apiClient.get<Group>(`/api/groups/${id}`);
  return response.data;
}

export async function addMember(groupId: number, data: AddMemberRequest): Promise<{ message: string }> {
  const response = await apiClient.post<{ message: string }>(`/api/groups/${groupId}/members`, data);
  return response.data;
}
