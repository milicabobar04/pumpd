import apiClient from './apiClient';

// Pregled trenera iz ugla člana (7.x). Admin pozivi su u trainersAdminService.js.

// SU-11 GET /api/trainers
//   -> [{ trainerId, name, surname, averageGrade }]  (averageGrade je null ako nema recenzija)
export async function getTrainers() {
  const response = await apiClient.get('/trainers');
  return Array.isArray(response.data) ? response.data : [];
}

// SU-12 GET /api/trainers/{trainerId}
//   -> { trainerId, name, surname, biography, averageGrade,
//        followStatus: 'NONE' | 'PENDING' | 'ACCEPTED',
//        reviews: [{ reviewId, memberName, grade, comment, date }] }
export async function getTrainer(trainerId) {
  const response = await apiClient.get(`/trainers/${trainerId}`);
  const data = response.data ?? {};
  return { ...data, reviews: Array.isArray(data.reviews) ? data.reviews : [] };
}

// SU-13 POST /api/trainers/{trainerId}/follow  -> 201 { followRequestId, status: 'PENDING', message }
export async function followTrainer(trainerId) {
  const response = await apiClient.post(`/trainers/${trainerId}/follow`);
  return response.data;
}

// SU-15 DELETE /api/trainers/{trainerId}/follow  -> { message }
// Povlači zahtjev koji čeka ili prekida aktivno praćenje.
export async function unfollowTrainer(trainerId) {
  const response = await apiClient.delete(`/trainers/${trainerId}/follow`);
  return response.data;
}

// SU-14 POST /api/trainers/{trainerId}/reviews  { grade: 1–5, comment?: string (≤ 500) }
//   -> 201 { reviewId, averageGrade, message }  (ponovna recenzija zamjenjuje staru)
export async function reviewTrainer(trainerId, { grade, comment }) {
  const response = await apiClient.post(`/trainers/${trainerId}/reviews`, {
    grade,
    comment: comment ? comment : null,
  });
  return response.data;
}
