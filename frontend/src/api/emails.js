import client from './client';

export const emailsApi = {
  /** GET /api/emails?page=0&size=20 */
  getAll: (page = 0, size = 20) =>
    client.get('/api/emails', { params: { page, size } }).then((r) => r.data),

  /** GET /api/emails/new-count */
  getNewCount: () =>
    client.get('/api/emails/new-count').then((r) => r.data),

  /** POST /api/emails/analyze  — may throw {status:422, extra:{currentCount,requiredCount}} */
  analyze: () =>
    client.post('/api/emails/analyze').then((r) => r.data),

  /** GET /api/emails/stats */
  getStats: () =>
    client.get('/api/emails/stats').then((r) => r.data),

  /** GET /api/emails/category/:category */
  getByCategory: (category, page = 0, size = 20) =>
    client.get(`/api/emails/category/${category}`, { params: { page, size } }).then((r) => r.data),

  /** GET /api/emails/priority/:level */
  getByPriority: (level, page = 0, size = 20) =>
    client.get(`/api/emails/priority/${level}`, { params: { page, size } }).then((r) => r.data),
};
