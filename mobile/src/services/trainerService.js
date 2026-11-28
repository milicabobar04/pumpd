import apiClient from './apiClient';

// Pozivi prijavljenog trenera (TRAINER). Pregled trenera iz ugla člana je u trainersService.js.

// SU-07 GET /api/trainer/reviews
//   -> { averageGrade: number | null, reviews: [{ reviewId, memberName, grade, comment, date }] }
//   averageGrade je null ako nema recenzija; recenzije su od najnovije.
export async function getMyReviews() {
  const response = await apiClient.get('/trainer/reviews');
  const data = response.data ?? {};
  const reviews = Array.isArray(data.reviews) ? data.reviews : [];
  return {
    averageGrade: data.averageGrade ?? null,
    // Server već šalje od najnovije; sortiranje je samo osigurač (isti datum zadržava redoslijed servera)
    reviews: [...reviews].sort((a, b) => String(b.date ?? '').localeCompare(String(a.date ?? ''))),
  };
}

// SU-32 GET /api/trainer/follow-requests
//   -> [{ requestId, memberId, memberName, requestDate }]  (zahtjevi na čekanju, najstariji prvi)
export async function getFollowRequests() {
  const response = await apiClient.get('/trainer/follow-requests');
  return Array.isArray(response.data) ? response.data : [];
}

export const FOLLOW_REQUEST_STATUS = {
  ACCEPTED: 'ACCEPTED',
  REJECTED: 'REJECTED',
};

// SU-33 PATCH /api/trainer/follow-requests/{requestId}  { status: 'ACCEPTED' | 'REJECTED' }
//   -> { message }
//   404 zahtjev ne postoji / povučen / član obrisan, 409 zahtjev je već obrađen
export async function respondToFollowRequest(requestId, status) {
  const response = await apiClient.patch(`/trainer/follow-requests/${requestId}`, { status });
  return response.data ?? {};
}

export const acceptFollowRequest = (requestId) =>
  respondToFollowRequest(requestId, FOLLOW_REQUEST_STATUS.ACCEPTED);

export const rejectFollowRequest = (requestId) =>
  respondToFollowRequest(requestId, FOLLOW_REQUEST_STATUS.REJECTED);
