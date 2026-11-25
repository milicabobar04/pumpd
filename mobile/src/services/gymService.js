import apiClient from './apiClient';

// 5.1 GET /api/gym/info  -> { name, description, membershipPrice, dateOfEstablishment, workingHours }
// Ista ruta se koristi i na javnom ekranu (publicService.getGymInfo).
export { getGymInfo } from './publicService';

// 5.2 PUT /api/gym/info  (SU-50, samo ADMIN)
// Tijelo: { name, description, membershipPrice, dateOfEstablishment: 'gggg-mm-dd', workingHours }
// -> { message }
export async function updateGymInfo(payload) {
  const response = await apiClient.put('/gym/info', payload);
  return response.data;
}
