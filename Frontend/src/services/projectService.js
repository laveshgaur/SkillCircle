import api, { unwrap } from '../lib/api';

/**
 * Project service — wraps `/api/v1/projects/*`, `/api/v1/tasks/*`.
 */

/** GET /projects — list current user's projects. */
export const getProjects = () => unwrap(api.get('/projects'));

/** POST /projects — create a project. */
export const createProject = (body) => unwrap(api.post('/projects', body));

/** GET /projects/{id} — project details. */
export const getProject = (id) => unwrap(api.get(`/projects/${id}`));

/** PUT /projects/{id} — update project. */
export const updateProject = (id, body) =>
  unwrap(api.put(`/projects/${id}`, body));

/** GET /projects/{id}/members — list members. */
export const getMembers = (id) => unwrap(api.get(`/projects/${id}/members`));

/** POST /projects/{id}/members — add member. */
export const addMember = (id, body) =>
  unwrap(api.post(`/projects/${id}/members`, body));

/** GET /projects/{id}/tasks — list tasks. */
export const getTasks = (id) => unwrap(api.get(`/projects/${id}/tasks`));

/** GET /projects/{id}/kanban — Kanban board. */
export const getKanban = (id) => unwrap(api.get(`/projects/${id}/kanban`));

/** POST /projects/{id}/tasks — create task. */
export const createTask = (id, body) =>
  unwrap(api.post(`/projects/${id}/tasks`, body));

/** PATCH /tasks/{id}/status — move task. */
export const updateTaskStatus = (id, status) =>
  unwrap(api.patch(`/tasks/${id}/status`, null, { params: { status } }));

/** PATCH /tasks/{id}/assign — assign task. */
export const assignTask = (id, assigneeId) =>
  unwrap(api.patch(`/tasks/${id}/assign`, null, { params: { assigneeId } }));
