import client from './client';

export const userApi = {
  /** POST /api/user/ai-key */
  saveApiKey: (apiKey) =>
    client.post('/api/user/ai-key', { apiKey }).then((r) => r.data),

  /** GET /api/user/ai-key/status */
  getApiKeyStatus: () =>
    client.get('/api/user/ai-key/status').then((r) => r.data),

  /** DELETE /api/user/ai-key */
  deleteApiKey: () =>
    client.delete('/api/user/ai-key'),
};

export const accountsApi = {
  /** GET /api/accounts */
  list: () =>
    client.get('/api/accounts').then((r) => r.data),

  /** GET /api/accounts/connect  → { authUrl } */
  getConnectUrl: () =>
    client.get('/api/accounts/connect').then((r) => r.data),

  /** DELETE /api/accounts/:id */
  disconnect: (id) =>
    client.delete(`/api/accounts/${id}`),
};
