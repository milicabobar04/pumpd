import apiClient from './apiClient';

// 12.5 GET /api/admin/workers  -> [{ workerId, name, surname, email }]
export async function getWorkers() {
  const response = await apiClient.get('/admin/workers');
  return Array.isArray(response.data) ? response.data : [];
}

// 12.6 GET /api/admin/workers/{workerId}  -> { workerId, name, surname, email }
export async function getWorker(workerId) {
  const response = await apiClient.get(`/admin/workers/${workerId}`);
  return response.data;
}

// 12.7 POST /api/admin/workers  -> 201 { workerId, temporaryPassword, message }
export async function addWorker(data) {
  const response = await apiClient.post('/admin/workers', data);
  return response.data;
}

// 12.8 DELETE /api/admin/workers/{workerId}  -> { message }
export async function deleteWorker(workerId) {
  const response = await apiClient.delete(`/admin/workers/${workerId}`);
  return response.data;
}
