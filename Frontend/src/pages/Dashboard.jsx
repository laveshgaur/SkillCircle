import { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import {
  Sparkles, Users, KanbanSquare, ArrowRight, UserCog,
  TrendingUp, Clock, CheckCircle2, Activity, Zap,
} from 'lucide-react';
import PageWrapper from '../components/layout/PageWrapper';
import { Button, Card, Badge, Avatar, ProgressBar } from '../components/ui';
import { GithubIcon } from '../components/icons/BrandIcons';
import { useAuthStore } from '../store/authStore';
import { getMyProfile } from '../services/profileService';
import { getMatchHistory } from '../services/matchService';
import { getSpaces } from '../services/communityService';
import { getProjects } from '../services/projectService';
import styles from './Dashboard.module.css';

function getGreeting() {
  const h = new Date().getHours();
  if (h < 12) return 'Good morning';
  if (h < 17) return 'Good afternoon';
  return 'Good evening';
}

function calcProfileCompleteness(profile) {
  if (!profile) return 0;
  let score = 0;
  const checks = [
    profile.displayName,
    profile.bio,
    profile.goals,
    profile.goalType,
    profile.experienceLevel,
    profile.timezone,
    profile.githubUsername,
    (profile.profileSkills || profile.skills || []).length > 0,
  ];
  checks.forEach((c) => { if (c) score += 1; });
  return Math.round((score / checks.length) * 100);
}

export default function Dashboard() {
  const user = useAuthStore((s) => s.user);
  const name = user?.username || 'there';
  const [stats, setStats] = useState({ matches: '—', spaces: '—', projects: '—' });
  const [profile, setProfile] = useState(null);
  const [recentMatches, setRecentMatches] = useState([]);

  const loadDashboardData = useCallback(async () => {
    // Load all in parallel, ignore individual failures
    const [profileRes, matchRes, spacesRes, projectsRes] = await Promise.allSettled([
      getMyProfile(),
      getMatchHistory(),
      getSpaces(),
      getProjects(),
    ]);

    if (profileRes.status === 'fulfilled') setProfile(profileRes.value);

    const matches = matchRes.status === 'fulfilled'
      ? (Array.isArray(matchRes.value) ? matchRes.value : matchRes.value?.content || [])
      : [];
    setRecentMatches(matches.slice(0, 3));

    setStats({
      matches: matches.length || '0',
      spaces: spacesRes.status === 'fulfilled'
        ? (Array.isArray(spacesRes.value) ? spacesRes.value.length : 0)
        : '—',
      projects: projectsRes.status === 'fulfilled'
        ? (Array.isArray(projectsRes.value) ? projectsRes.value.length : 0)
        : '—',
    });
  }, []);

  useEffect(() => { loadDashboardData(); }, [loadDashboardData]);

  const completeness = calcProfileCompleteness(profile);

  const STATS = [
    { icon: Sparkles, label: 'Matches', value: stats.matches, to: '/app/matches', color: 'brand' },
    { icon: Users, label: 'Spaces', value: stats.spaces, to: '/app/community', color: 'blue' },
    { icon: KanbanSquare, label: 'Projects', value: stats.projects, to: '/app/projects', color: 'green' },
  ];

  const steps = [
    {
      icon: UserCog,
      title: 'Complete your profile',
      text: 'Add your goals, experience, and availability so matching has something to work with.',
      to: '/app/profile',
      cta: 'Edit profile',
      done: completeness >= 75,
    },
    {
      icon: GithubIcon,
      title: 'Connect GitHub',
      text: 'Sync your repositories to auto-extract the skills you actually use.',
      to: '/app/profile',
      cta: 'Connect',
      done: !!profile?.githubUsername,
    },
    {
      icon: Sparkles,
      title: 'Find your first match',
      text: 'Run the matching pipeline and start a conversation with someone who fits.',
      to: '/app/matches',
      cta: 'View matches',
      done: recentMatches.length > 0,
    },
  ];

  const doneCount = steps.filter((s) => s.done).length;
  const allDone = doneCount === steps.length;

  return (
    <PageWrapper
      title={`${getGreeting()}, ${name}`}
      subtitle="Here's what's happening in your circle."
      actions={
        <Button as={Link} to="/app/matches" rightIcon={<ArrowRight size={16} />}>
          Find matches
        </Button>
      }
    >
      {/* Stats Row */}
      <div className={styles.stats}>
        {STATS.map(({ icon: Icon, label, value, to, color }) => (
          <Card key={label} as={Link} to={to} interactive padded className={styles.stat}>
            <span className={`${styles.statIcon} ${styles[`statIcon--${color}`]}`}>
              <Icon size={20} aria-hidden="true" />
            </span>
            <div>
              <div className={styles.statValue}>{value}</div>
              <div className={styles.statLabel}>{label}</div>
            </div>
          </Card>
        ))}
      </div>

      {/* Profile completeness */}
      {profile && completeness < 100 && (
        <Card padded className={styles.completenessCard}>
          <div className={styles.completenessHeader}>
            <TrendingUp size={18} />
            <span className={styles.completenessTitle}>Profile completeness</span>
            <Badge variant="brand" size="sm">{completeness}%</Badge>
          </div>
          <ProgressBar value={completeness} size="sm" variant={completeness >= 75 ? 'success' : 'brand'} />
          <p className={styles.completenessHint}>
            A complete profile improves match quality. Head to your{' '}
            <Link to="/app/profile">profile</Link> to fill in the gaps.
          </p>
        </Card>
      )}

      {/* Onboarding steps */}
      {!allDone && (
        <section className={styles.section}>
          <div className={styles.sectionHead}>
            <h2 className={styles.sectionTitle}>Get set up</h2>
            <Badge variant="brand" size="sm">
              {doneCount}/{steps.length} done
            </Badge>
          </div>
          <div className={styles.steps}>
            {steps.map(({ icon: Icon, title, text, to, cta, done }) => (
              <Card key={title} padded className={`${styles.stepCard} ${done ? styles.stepDone : ''}`}>
                <span className={styles.stepIcon}>
                  {done ? <CheckCircle2 size={20} /> : <Icon size={20} />}
                </span>
                <h3 className={styles.stepTitle}>{title}</h3>
                <p className={styles.stepText}>{text}</p>
                {!done && (
                  <Button as={Link} to={to} variant="outline" size="sm">
                    {cta}
                  </Button>
                )}
              </Card>
            ))}
          </div>
        </section>
      )}

      {/* Recent Matches */}
      {recentMatches.length > 0 && (
        <section className={styles.section}>
          <div className={styles.sectionHead}>
            <h2 className={styles.sectionTitle}>Recent matches</h2>
            <Button as={Link} to="/app/matches" variant="ghost" size="sm" rightIcon={<ArrowRight size={14} />}>
              View all
            </Button>
          </div>
          <div className={styles.matchRow}>
            {recentMatches.map((m, i) => {
              const score = Math.round((m.finalScore || m.score || 0) * 100);
              return (
                <Card key={m.userId || i} padded interactive className={styles.matchCard}>
                  <div className={styles.matchHead}>
                    <Avatar
                      name={m.displayName || m.username || `Match ${i + 1}`}
                      size="md"
                      status={m.availability === 'OPEN' ? 'online' : 'away'}
                    />
                    <div className={styles.matchInfo}>
                      <div className={styles.matchName}>{m.displayName || m.username}</div>
                      <div className={styles.matchMeta}>
                        {[m.goalType, m.experienceLevel].filter(Boolean).join(' · ')}
                      </div>
                    </div>
                    <Badge variant="brand" size="sm">{score}%</Badge>
                  </div>
                </Card>
              );
            })}
          </div>
        </section>
      )}

      {/* Quick Actions */}
      <section className={styles.section}>
        <div className={styles.sectionHead}>
          <h2 className={styles.sectionTitle}>Quick actions</h2>
        </div>
        <div className={styles.quickActions}>
          <Button as={Link} to="/app/matches" variant="outline" leftIcon={<Zap size={16} />}>
            Run pipeline
          </Button>
          <Button as={Link} to="/app/projects" variant="outline" leftIcon={<KanbanSquare size={16} />}>
            New project
          </Button>
          <Button as={Link} to="/app/community" variant="outline" leftIcon={<Users size={16} />}>
            Browse spaces
          </Button>
        </div>
      </section>
    </PageWrapper>
  );
}
