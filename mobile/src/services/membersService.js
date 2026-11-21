import apiClient from './apiClient';

export async function getMembers(search) {
  const params = search ? { search } : {};
  const response = await apiClient.get('/worker/members', { params });
  return Array.isArray(response.data) ? response.data : [];
}

export async function getMember(memberId) {
  const response = await apiClient.get(`/worker/members/${memberId}`);
  return response.data;
}

export async function addMember(data) {
  const response = await apiClient.post('/worker/members', data);
  return response.data;
}

export async function deleteMember(memberId) {
  const response = await apiClient.delete(`/worker/members/${memberId}`);
  return response.data;
}