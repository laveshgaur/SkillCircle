import api, { unwrap } from '../lib/api';

/**
 * Profile & skills service — wraps `/api/v1/profiles/*` and `/api/v1/skills/*`.
 * Consumed by profile/settings pages (Phase 8); included in the shell so the
 * data layer is complete and the auth flow can hydrate a profile.
 */

/** GET /profiles/me — current user's profile (created on first access). */
export const getMyProfile = () => unwrap(api.get('/profiles/me'));

/** GET /profiles/{id} — public profile by id. */
export const getProfile = (id) => unwrap(api.get(`/profiles/${id}`));

/** PUT /profiles/me — update the current user's profile. */
export const updateProfile = (body) => unwrap(api.put('/profiles/me', body));

/** POST /profiles/me/skills — add a skill. */
export const addSkill = (body) => unwrap(api.post('/profiles/me/skills', body));

/** DELETE /profiles/me/skills/{skillId} — remove a skill. */
export const removeSkill = (skillId) =>
  unwrap(api.delete(`/profiles/me/skills/${skillId}`));

/** POST /profiles/me/sync-github — import skills from GitHub repos. */
export const syncGitHub = () => unwrap(api.post('/profiles/me/sync-github'));

/** GET /skills/autocomplete?q= — skill picker suggestions. */
export const autocompleteSkills = (q) =>
  unwrap(api.get('/skills/autocomplete', { params: { q } }));

/** GET /skills/search?q= — broader skill search. */
export const searchSkills = (q) =>
  unwrap(api.get('/skills/search', { params: { q } }));
