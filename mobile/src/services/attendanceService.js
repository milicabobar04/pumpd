import apiClient from './apiClient';

export async function registerEntry(qrCode) {
  const response = await apiClient.post('/worker/attendance/entry', { qrCode });
  return response.data;
}

export async function registerExit(qrCode) {
  const response = await apiClient.post('/worker/attendance/exit', { qrCode });
  return response.data;
}

export async function getOccupancy() {
  const response = await apiClient.get('/gym/occupancy');
  return response.data;
}