import apiClient from './apiClient';

// Status vrijednosti iz ugovora (openapi: UNPROCESSED, IN_PROGRESS, PROCESSED)
export const ISSUE_STATUSES = ['UNPROCESSED', 'IN_PROGRESS', 'PROCESSED'];

// 4.2 GET /api/admin/issues  (SU-59)
// -> [{ issueId, title, description, status, reportDate }], najnoviji prvi; prazan niz ako nema problema
export async function getAdminIssues() {
  const response = await apiClient.get('/admin/issues');
  return Array.isArray(response.data) ? response.data : [];
}

// 4.3 PATCH /api/admin/issues/{issueId}  (SU-59)
// Tijelo: { status } -> { issueId, status, message }
export async function updateIssueStatus(issueId, payload) {
  const response = await apiClient.patch(`/admin/issues/${issueId}`, payload);
  return response.data;
}
