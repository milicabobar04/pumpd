import apiClient from './apiClient';

export async function getMyProfile() {
  const response = await apiClient.get('/users/my-profile');
  return response.data;
}

export async function updateMyProfile(data) {
  const response = await apiClient.put('/users/my-profile', data);
  return response.data;
}

export async function changePassword(oldPassword, newPassword) {
  const response = await apiClient.put('/users/my-password', { oldPassword, newPassword });
  return response.data;
}
