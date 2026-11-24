import apiClient from './apiClient';

export async function getNotifications() {
  const response = await apiClient.get('/notifications');
  return Array.isArray(response.data) ? response.data : [];
}

export async function markNotificationAsRead(notificationId) {
  const response = await apiClient.patch(`/notifications/${notificationId}/read`);
  return response.data;
}

export async function markAllNotificationsAsRead() {
  const response = await apiClient.patch('/notifications/read-all');
  return response.data;
}
