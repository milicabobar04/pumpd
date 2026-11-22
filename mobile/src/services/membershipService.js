import apiClient from './apiClient';

export async function getMyMembership() {
  const response = await apiClient.get('/membership/my-status');
  return response.data;
}

export async function getMemberMembership(memberId) {
  const response = await apiClient.get(`/worker/members/${memberId}/membership`);
  return response.data;
}

export async function updateMemberMembership(memberId, data) {
  const response = await apiClient.put(`/worker/members/${memberId}/membership`, data);
  return response.data;
}

export async function extendMembership(memberId, numMonths) {
  const response = await apiClient.post(`/worker/members/${memberId}/membership/extend`, { numMonths });
  return response.data;
}