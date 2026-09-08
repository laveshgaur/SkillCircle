import api, { unwrap } from '../lib/api';

/**
 * Community service — wraps `/api/v1/spaces/*`, `/api/v1/threads/*`.
 */

/** GET /spaces — list public + project spaces. */
export const getSpaces = () => unwrap(api.get('/spaces'));

/** POST /spaces — create a space. */
export const createSpace = (body) => unwrap(api.post('/spaces', body));

/** GET /spaces/{id} — get space details. */
export const getSpace = (id) => unwrap(api.get(`/spaces/${id}`));

/** GET /spaces/{id}/threads — list threads (paginated). */
export const getThreads = (spaceId, page = 0, size = 20) =>
  unwrap(api.get(`/spaces/${spaceId}/threads`, { params: { page, size } }));

/** POST /spaces/{id}/threads — create a thread. */
export const createThread = (spaceId, body) =>
  unwrap(api.post(`/spaces/${spaceId}/threads`, body));

/** GET /threads/{id}/messages — paginated messages. */
export const getMessages = (threadId, page = 0, size = 50) =>
  unwrap(api.get(`/threads/${threadId}/messages`, { params: { page, size } }));

/** GET /presence/online — online users. */
export const getOnlineUsers = () => unwrap(api.get('/presence/online'));
