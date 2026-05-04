import axios from 'axios';

const BASE_URL = '/api';

const api = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' }
});

export const getIncidents = () => api.get('/incidents');
export const getIncident = (id) => api.get(`/incidents/${id}`);
export const getSignals = (id) => api.get(`/incidents/${id}/signals`);
export const investigateIncident = (id) => api.put(`/incidents/${id}/investigate`);
export const resolveIncident = (id) => api.put(`/incidents/${id}/resolve`);
export const closeIncident = (id) => api.put(`/incidents/${id}/close`);
export const submitRca = (workItemId, rca) => api.post(`/rca/${workItemId}`, rca);
export const getRca = (workItemId) => api.get(`/rca/${workItemId}`);
export const getHealth = () => api.get('http://localhost:8080/health');
