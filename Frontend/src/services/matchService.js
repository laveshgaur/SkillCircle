import api, { unwrap } from '../lib/api';

/**
 * Matching service — wraps `/api/v1/match/*` endpoints.
 */

/** POST /match/find — run the 3-stage matching pipeline. */
export const findMatches = (params = {}) =>
  unwrap(api.post('/match/find', params));

/** GET /match/history — past match results. */
export const getMatchHistory = () => unwrap(api.get('/match/history'));

/** POST /match/{id}/accept — accept a match. */
export const acceptMatch = (id) => unwrap(api.post(`/match/${id}/accept`));

/** POST /match/{id}/dismiss — dismiss a match. */
export const dismissMatch = (id) => unwrap(api.post(`/match/${id}/dismiss`));
