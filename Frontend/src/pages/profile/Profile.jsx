import { useState, useEffect, useCallback, useRef } from 'react';
import {
  Clock, X, Pencil, Save, Loader2, Globe, Link as LinkIcon,
} from 'lucide-react';
import PageWrapper from '../../components/layout/PageWrapper';
import { Button, Card, Badge, Avatar, Input, Spinner, Select, ProgressBar, useToast } from '../../components/ui';
import { GithubIcon } from '../../components/icons/BrandIcons';
import { useAuthStore } from '../../store/authStore';
import {
  getMyProfile, updateProfile, addSkill, removeSkill, syncGitHub, autocompleteSkills,
} from '../../services/profileService';
import styles from './Profile.module.css';

const GOAL_TYPES = [
  { value: 'LEARNING', label: 'Learning' },
  { value: 'BUILDING', label: 'Building' },
  { value: 'MENTORING', label: 'Mentoring' },
  { value: 'EXPLORING', label: 'Exploring' },
];
const EXP_LEVELS = [
  { value: 'BEGINNER', label: 'Beginner' },
  { value: 'INTERMEDIATE', label: 'Intermediate' },
  { value: 'ADVANCED', label: 'Advanced' },
  { value: 'EXPERT', label: 'Expert' },
];
const AVAILABILITY = [
  { value: 'OPEN', label: 'Open' },
  { value: 'SELECTIVE', label: 'Selective' },
  { value: 'BUSY', label: 'Busy' },
];

function calcProfileCompleteness(profile) {
  if (!profile) return 0;
  let score = 0;
  const checks = [
    profile.displayName, profile.bio, profile.goals, profile.goalType,
    profile.experienceLevel, profile.timezone, profile.githubUsername,
    (profile.profileSkills || profile.skills || []).length > 0,
  ];
  checks.forEach((c) => { if (c) score += 1; });
  return Math.round((score / checks.length) * 100);
}

export default function Profile() {
  const user = useAuthStore((s) => s.user);
  const toast = useToast();
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState(false);
  const [saving, setSaving] = useState(false);
  const [syncing, setSyncing] = useState(false);
  const [newSkill, setNewSkill] = useState('');
  const [suggestions, setSuggestions] = useState([]);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [form, setForm] = useState({});
  const suggestRef = useRef(null);
  const debounceRef = useRef(null);

  const load = useCallback(async () => {
    try {
      const p = await getMyProfile();
      setProfile(p);
      setForm({
        displayName: p.displayName || '',
        bio: p.bio || '',
        goals: p.goals || '',
        goalType: p.goalType || 'BUILDING',
        experienceLevel: p.experienceLevel || 'INTERMEDIATE',
        availability: p.availability || 'OPEN',
        timezone: p.timezone || Intl.DateTimeFormat().resolvedOptions().timeZone,
        githubUsername: p.githubUsername || '',
        linkedinUrl: p.linkedinUrl || '',
        websiteUrl: p.websiteUrl || '',
      });
    } catch (e) {
      console.error('Failed to load profile', e);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  // Close suggestion dropdown on outside click
  useEffect(() => {
    const onClickOutside = (e) => {
      if (suggestRef.current && !suggestRef.current.contains(e.target)) {
        setShowSuggestions(false);
      }
    };
    document.addEventListener('mousedown', onClickOutside);
    return () => document.removeEventListener('mousedown', onClickOutside);
  }, []);

  const handleSave = async () => {
    setSaving(true);
    try {
      const updated = await updateProfile(form);
      setProfile(updated);
      setEditing(false);
      toast.success('Profile updated successfully');
    } catch (e) {
      toast.error(e.message || 'Failed to save profile');
    } finally {
      setSaving(false);
    }
  };

  const handleSkillSearch = (value) => {
    setNewSkill(value);
    clearTimeout(debounceRef.current);
    if (value.trim().length < 2) {
      setSuggestions([]);
      setShowSuggestions(false);
      return;
    }
    debounceRef.current = setTimeout(async () => {
      try {
        const results = await autocompleteSkills(value.trim());
        const list = Array.isArray(results) ? results : [];
        setSuggestions(list.slice(0, 8));
        setShowSuggestions(list.length > 0);
      } catch {
        setSuggestions([]);
      }
    }, 250);
  };

  const handleAddSkill = async (skillName) => {
    const name = (skillName || newSkill).trim();
    if (!name) return;
    try {
      await addSkill({ name });
      setNewSkill('');
      setSuggestions([]);
      setShowSuggestions(false);
      toast.success(`Added "${name}"`);
      await load();
    } catch (e) {
      toast.error(e.message || 'Failed to add skill');
    }
  };

  const handleRemoveSkill = async (skillId) => {
    try {
      await removeSkill(skillId);
      toast.info('Skill removed');
      await load();
    } catch (e) {
      toast.error(e.message || 'Failed to remove skill');
    }
  };

  const handleSync = async () => {
    setSyncing(true);
    try {
      await syncGitHub();
      toast.success('GitHub sync complete — skills updated');
      await load();
    } catch (e) {
      toast.error(e.message || 'GitHub sync failed');
    } finally {
      setSyncing(false);
    }
  };

  if (loading) return <PageWrapper title="Profile"><Spinner center /></PageWrapper>;

  const skills = profile?.profileSkills || profile?.skills || [];
  const completeness = calcProfileCompleteness(profile);

  return (
    <PageWrapper
      title="Profile"
      subtitle="Your skills, goals, and developer identity."
      actions={
        !editing && (
          <Button onClick={() => setEditing(true)} variant="outline" leftIcon={<Pencil size={16} />}>
            Edit profile
          </Button>
        )
      }
    >
      <div className={styles.profileGrid}>
        {/* --- Sidebar --- */}
        <Card className={styles.sidebar}>
          <div className={styles.avatarWrap}>
            <Avatar
              name={profile?.displayName || user?.username || '?'}
              size="xl"
              status={profile?.availability === 'OPEN' ? 'online' : profile?.availability === 'BUSY' ? 'busy' : 'away'}
              ring
            />
          </div>
          <div className={styles.name}>{profile?.displayName || user?.username}</div>
          <div className={styles.username}>@{user?.username}</div>
          {profile?.bio && <p className={styles.bio}>{profile.bio}</p>}

          <div className={styles.metaRow}>
            {profile?.timezone && (
              <span className={styles.metaItem}><Clock size={14} /> {profile.timezone}</span>
            )}
            {profile?.experienceLevel && (
              <Badge size="sm" variant="outline">{profile.experienceLevel}</Badge>
            )}
            {profile?.goalType && (
              <Badge size="sm" variant="brand">{profile.goalType}</Badge>
            )}
            {profile?.availability && (
              <Badge size="sm" variant={profile.availability === 'OPEN' ? 'success' : 'outline'}>
                {profile.availability}
              </Badge>
            )}
          </div>

          {/* Profile completeness */}
          <div className={styles.completenessWrap}>
            <span className={styles.completenessLabel}>Profile completeness</span>
            <ProgressBar value={completeness} size="sm" showLabel />
          </div>

          {profile?.githubUsername && (
            <a href={`https://github.com/${profile.githubUsername}`} target="_blank" rel="noopener noreferrer" className={styles.metaItem}>
              <GithubIcon size={14} /> {profile.githubUsername}
            </a>
          )}
          {profile?.linkedinUrl && (
            <a href={profile.linkedinUrl} target="_blank" rel="noopener noreferrer" className={styles.metaItem}>
              <LinkIcon size={14} /> LinkedIn
            </a>
          )}
          {profile?.websiteUrl && (
            <a href={profile.websiteUrl} target="_blank" rel="noopener noreferrer" className={styles.metaItem}>
              <Globe size={14} /> Website
            </a>
          )}
        </Card>

        {/* --- Content --- */}
        <div className={styles.content}>
          {/* Skills */}
          <section>
            <h2 className={styles.sectionTitle}>Skills</h2>
            {skills.length > 0 ? (
              <div className={styles.skills}>
                {skills.map((ps) => (
                  <Badge key={ps.id || ps.skill?.id} variant="outline" className={styles.skillTag}>
                    {ps.skill?.name || ps.name}
                    {ps.proficiency && (
                      <span className={styles.proficiency}>{ps.proficiency}</span>
                    )}
                    {editing && (
                      <button className={styles.removeSkill} onClick={() => handleRemoveSkill(ps.id)}>
                        <X size={12} />
                      </button>
                    )}
                  </Badge>
                ))}
              </div>
            ) : (
              <p className={styles.emptySkills}>No skills added yet. Edit your profile to add some.</p>
            )}
            {editing && (
              <div className={styles.skillAdder} ref={suggestRef}>
                <div className={styles.autocompleteWrap}>
                  <Input
                    placeholder="e.g. React, Python, Docker..."
                    value={newSkill}
                    onChange={(e) => handleSkillSearch(e.target.value)}
                    onKeyDown={(e) => e.key === 'Enter' && handleAddSkill()}
                    onFocus={() => suggestions.length > 0 && setShowSuggestions(true)}
                  />
                  {showSuggestions && suggestions.length > 0 && (
                    <div className={styles.suggestions}>
                      {suggestions.map((s) => {
                        const name = typeof s === 'string' ? s : s.name;
                        return (
                          <button
                            key={name}
                            type="button"
                            className={styles.suggestionItem}
                            onClick={() => handleAddSkill(name)}
                          >
                            {name}
                            {s.category && <span className={styles.suggestionCat}>{s.category}</span>}
                          </button>
                        );
                      })}
                    </div>
                  )}
                </div>
                <Button onClick={() => handleAddSkill()} size="sm">Add</Button>
              </div>
            )}
          </section>

          {/* GitHub Sync */}
          <section>
            <h2 className={styles.sectionTitle}>GitHub Sync</h2>
            <div className={styles.syncRow}>
              <GithubIcon size={24} />
              <span className={styles.syncText}>
                {profile?.githubUsername
                  ? `Connected as ${profile.githubUsername}. Sync to refresh skills from your repos.`
                  : 'Set your GitHub username in your profile to enable skill sync.'}
              </span>
              <Button
                onClick={handleSync}
                variant="outline"
                size="sm"
                disabled={syncing || !profile?.githubUsername}
                leftIcon={syncing ? <Loader2 size={14} className="spin" /> : <GithubIcon size={14} />}
              >
                {syncing ? 'Syncing…' : 'Sync now'}
              </Button>
            </div>
          </section>

          {/* Edit Form */}
          {editing && (
            <section>
              <h2 className={styles.sectionTitle}>Edit Profile</h2>
              <div className={styles.editForm}>
                <Input
                  label="Display Name"
                  value={form.displayName}
                  onChange={(e) => setForm({ ...form, displayName: e.target.value })}
                />
                <Input
                  label="Bio"
                  value={form.bio}
                  onChange={(e) => setForm({ ...form, bio: e.target.value })}
                  placeholder="Tell others about yourself..."
                />
                <Input
                  label="Goals"
                  value={form.goals}
                  onChange={(e) => setForm({ ...form, goals: e.target.value })}
                  placeholder="What do you want to build or learn?"
                />
                <div className={styles.formRow}>
                  <Select
                    label="Goal Type"
                    value={form.goalType}
                    onChange={(e) => setForm({ ...form, goalType: e.target.value })}
                    options={GOAL_TYPES}
                  />
                  <Select
                    label="Experience"
                    value={form.experienceLevel}
                    onChange={(e) => setForm({ ...form, experienceLevel: e.target.value })}
                    options={EXP_LEVELS}
                  />
                </div>
                <div className={styles.formRow}>
                  <Select
                    label="Availability"
                    value={form.availability}
                    onChange={(e) => setForm({ ...form, availability: e.target.value })}
                    options={AVAILABILITY}
                  />
                  <Input
                    label="Timezone"
                    value={form.timezone}
                    onChange={(e) => setForm({ ...form, timezone: e.target.value })}
                  />
                </div>
                <Input
                  label="GitHub Username"
                  value={form.githubUsername}
                  onChange={(e) => setForm({ ...form, githubUsername: e.target.value })}
                  placeholder="e.g. octocat"
                />
                <Input
                  label="LinkedIn URL"
                  value={form.linkedinUrl}
                  onChange={(e) => setForm({ ...form, linkedinUrl: e.target.value })}
                  placeholder="https://linkedin.com/in/..."
                />
                <Input
                  label="Website URL"
                  value={form.websiteUrl}
                  onChange={(e) => setForm({ ...form, websiteUrl: e.target.value })}
                  placeholder="https://..."
                />
                <div className={styles.formActions}>
                  <Button variant="ghost" onClick={() => setEditing(false)}>Cancel</Button>
                  <Button onClick={handleSave} disabled={saving} leftIcon={saving ? <Loader2 size={14} className="spin" /> : <Save size={14} />}>
                    {saving ? 'Saving…' : 'Save changes'}
                  </Button>
                </div>
              </div>
            </section>
          )}
        </div>
      </div>
    </PageWrapper>
  );
}
