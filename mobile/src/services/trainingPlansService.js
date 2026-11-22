import apiClient from './apiClient';

export async function getPlans() {
  const response = await apiClient.get('/training-plans');
  return response.data;
}

export async function getPlan(planId) {
  const response = await apiClient.get(`/training-plans/${planId}`);
  return response.data;
}

export async function createPlan(data) {
  const response = await apiClient.post('/training-plans', data);
  return response.data;
}

export async function updatePlan(planId, data) {
  const response = await apiClient.put(`/training-plans/${planId}`, data);
  return response.data;
}

export async function deletePlan(planId) {
  const response = await apiClient.delete(`/training-plans/${planId}`);
  return response.data;
}