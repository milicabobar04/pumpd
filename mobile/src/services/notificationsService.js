import apiClient from './apiClient';

export async function getNotifications() {
  const response = await apiClient.get('/notifications');
  return Array.isArray(response.data) ? response.data : [];
}
