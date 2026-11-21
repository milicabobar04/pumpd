import apiClient from './apiClient';

// 12.1 GET /api/admin/trainers  -> [{ trainerId, name, surname, email }]
export async function getTrainers() {
  const response = await apiClient.get('/admin/trainers');
  return Array.isArray(response.data) ? response.data : [];
}

// 12.2 GET /api/admin/trainers/{trainerId}
//   -> { trainerId, name, surname, email, biography, averageGrade, followerCount }
export async function getTrainer(trainerId) {
  const response = await apiClient.get(`/admin/trainers/${trainerId}`);
  return response.data;
}

// 12.3 POST /api/admin/trainers  -> 201 { trainerId, temporaryPassword, message }
export async function addTrainer(data) {
  const response = await apiClient.post('/admin/trainers', data);
  return response.data;
}

// 12.4 DELETE /api/admin/trainers/{trainerId}  -> { message }
export async function deleteTrainer(trainerId) {
  const response = await apiClient.delete(`/admin/trainers/${trainerId}`);
  return response.data;
}
