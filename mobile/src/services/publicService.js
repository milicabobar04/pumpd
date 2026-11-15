import apiClient from './apiClient';

export async function getGymInfo() {
  const response = await apiClient.get('/gym/info');
  return response.data;
}

export async function reportIssue({ title, description }) {
  const response = await apiClient.post('/issues', { title, description });
  return response.data;
}
