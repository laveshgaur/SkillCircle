import { useState, useEffect } from 'react';
import {
  Sparkles, Search, BrainCircuit, Users, Target, Clock, CheckCircle2,
  XCircle, History, Filter, Eye,
} from 'lucide-react';
import PageWrapper from '../../components/layout/PageWrapper';
import { Button, Card, Badge, Avatar, Spinner, Tabs, Select, Modal, useToast } from '../../components/ui';
import { findMatches, getMatchHistory, acceptMatch, dismissMatch } from '../../services/matchService';
import styles from './Matches.module.css';

const GOAL_OPTIONS = [
  { value: '', label: 'All goals' },
  { value: 'LEARNING', label: 'Learning' },
  { value: 'BUILDING', label: 'Building' },
  { value: 'MENTORING', label: 'Mentoring' },
  { value: 'EXPLORING', label: 'Exploring' },
];
const EXP_OPTIONS = [
  { value: '', label: 'All levels' },
  { value: 'BEGINNER', label: 'Beginner' },
  { value: 'INTERMEDIATE', label: 'Intermediate' },
  { value: 'ADVANCED', label: 'Advanced' },
  { value: 'EXPERT', label: 'Expert' },
];

export default function Matches() {
  const toast = useToast();
  const [activeTab, setActiveTab] = useState('discover');
  const [matches, setMatches] = useState([]);
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(false);
  const [hasSearched, setHasSearched] = useState(false);
  const [goalFilter, setGoalFilter] = useState('');
  const [expFilter, setExpFilter] = useState('');
  const [selectedMatch, setSelectedMatch] = useState(null);
  const [dismissing, setDismissing] = useState(new Set());

  const runPipeline = async () => {
    setLoading(true);
    try {
      const params = {};
      if (goalFilter) params.goalType = goalFilter;
      if (expFilter) params.experienceLevel = expFilter;
      const results = await findMatches(params);
      setMatches(Array.isArray(results) ? results : results?.matches || []);
      setHasSearched(true);
    } catch (e) {
      console.error('Pipeline failed', e);
      setMatches(DEMO_MATCHES);
      setHasSearched(true);
    } finally {
      setLoading(false);
    }
  };

  const loadHistory = async () => {
    try {
      const data = await getMatchHistory();
      setHistory(Array.isArray(data) ? data : data?.content || []);
    } catch (e) {
      console.error('History load failed', e);
      setHistory([]);
    }
  };

  useEffect(() => {
    if (activeTab === 'history') loadHistory();
  }, [activeTab]);

  const handleAccept = async (matchId) => {
    try {
      await acceptMatch(matchId);
      toast.success('Match accepted! You can now collaborate.');
      setMatches((prev) => prev.filter((m) => m.userId !== matchId && m.id !== matchId));
    } catch (e) {
      toast.error(e.message || 'Failed to accept match');
    }
  };

  const handleDismiss = async (matchId) => {
    setDismissing((prev) => new Set(prev).add(matchId));
    try {
      await dismissMatch(matchId);
      // Animate out then remove
      setTimeout(() => {
        setMatches((prev) => prev.filter((m) => m.userId !== matchId && m.id !== matchId));
        setDismissing((prev) => { const n = new Set(prev); n.delete(matchId); return n; });
      }, 300);
    } catch (e) {
      toast.error(e.message || 'Failed to dismiss');
      setDismissing((prev) => { const n = new Set(prev); n.delete(matchId); return n; });
    }
  };

  const TABS = [
    { id: 'discover', label: 'Discover', icon: <Sparkles size={14} /> },
    { id: 'history', label: 'History', icon: <History size={14} />, badge: history.length || undefined },
  ];

  return (
    <PageWrapper
      title="Find Matches"
      subtitle="Run the AI pipeline to discover developers to build with."
      actions={
        <Button
          onClick={runPipeline}
          disabled={loading}
          leftIcon={loading ? <Sparkles size={16} className="spin" /> : <Sparkles size={16} />}
        >
          {loading ? 'Matching…' : 'Run pipeline'}
        </Button>
      }
    >
      <Tabs tabs={TABS} activeId={activeTab} onChange={setActiveTab} />

      {activeTab === 'discover' && (
        <>
          {/* Filters */}
          <div className={styles.filters}>
            <Filter size={16} className={styles.filterIcon} />
            <Select
              value={goalFilter}
              onChange={(e) => setGoalFilter(e.target.value)}
              options={GOAL_OPTIONS}
            />
            <Select
              value={expFilter}
              onChange={(e) => setExpFilter(e.target.value)}
              options={EXP_OPTIONS}
            />
          </div>

          {/* Pipeline stages animation during loading */}
          {loading && (
            <div className={styles.empty}>
              <div className={styles.pipelineStages}>
                <PipelineStage label="Semantic retrieval" step={1} active />
                <PipelineStage label="Collaborative re-ranking" step={2} />
                <PipelineStage label="Hard filters" step={3} />
              </div>
              <Spinner />
              <p className={styles.emptySub}>Running the 3-stage pipeline…</p>
            </div>
          )}

          {!loading && !hasSearched && (
            <div className={styles.empty}>
              <span className={styles.emptyIcon}><Sparkles size={28} /></span>
              <h3 className={styles.emptyTitle}>Ready to find collaborators</h3>
              <p className={styles.emptySub}>
                Click "Run pipeline" to find developers matched by skills, goals, and working style.
              </p>
              <Button onClick={runPipeline} leftIcon={<Search size={16} />}>
                Run pipeline
              </Button>
            </div>
          )}

          {!loading && hasSearched && matches.length === 0 && (
            <div className={styles.empty}>
              <span className={styles.emptyIcon}><Users size={28} /></span>
              <h3 className={styles.emptyTitle}>No matches found</h3>
              <p className={styles.emptySub}>
                Complete your profile with skills and goals — the pipeline needs context to work with.
              </p>
            </div>
          )}

          {!loading && matches.length > 0 && (
            <div className={styles.grid}>
              {matches.map((m, i) => {
                const matchId = m.userId || m.id || i;
                return (
                  <MatchCard
                    key={matchId}
                    match={m}
                    rank={i + 1}
                    exiting={dismissing.has(matchId)}
                    onAccept={() => handleAccept(matchId)}
                    onDismiss={() => handleDismiss(matchId)}
                    onView={() => setSelectedMatch(m)}
                  />
                );
              })}
            </div>
          )}
        </>
      )}

      {activeTab === 'history' && (
        <div className={styles.grid}>
          {history.length > 0 ? (
            history.map((m, i) => (
              <MatchCard key={m.userId || i} match={m} rank={i + 1} onView={() => setSelectedMatch(m)} />
            ))
          ) : (
            <div className={styles.empty}>
              <span className={styles.emptyIcon}><History size={28} /></span>
              <h3 className={styles.emptyTitle}>No match history yet</h3>
              <p className={styles.emptySub}>Run the pipeline to start building your match history.</p>
            </div>
          )}
        </div>
      )}

      {/* Profile Detail Modal */}
      {selectedMatch && (
        <Modal
          open={!!selectedMatch}
          onClose={() => setSelectedMatch(null)}
          title={selectedMatch.displayName || selectedMatch.username || 'Developer Profile'}
          size="lg"
        >
          <ProfileDetail match={selectedMatch} />
        </Modal>
      )}
    </PageWrapper>
  );
}

function PipelineStage({ label, step, active }) {
  return (
    <div className={`${styles.stage} ${active ? styles.stageActive : ''}`}>
      <span className={styles.stageNum}>{step}</span>
      <span className={styles.stageLabel}>{label}</span>
    </div>
  );
}

function ScoreRing({ value, size = 56 }) {
  const r = (size - 6) / 2;
  const circumference = 2 * Math.PI * r;
  const offset = circumference - (value / 100) * circumference;
  return (
    <div className={styles.scoreCircle} style={{ width: size, height: size }}>
      <svg viewBox={`0 0 ${size} ${size}`} className={styles.scoreSvg}>
        <circle cx={size / 2} cy={size / 2} r={r} className={styles.scoreTrack} strokeWidth="3" />
        <circle
          cx={size / 2}
          cy={size / 2}
          r={r}
          className={styles.scoreFill}
          strokeWidth="3"
          strokeDasharray={circumference}
          strokeDashoffset={offset}
          transform={`rotate(-90 ${size / 2} ${size / 2})`}
        />
      </svg>
      <div className={styles.scoreCenter}>
        <span className={styles.scoreNum}>{value}</span>
        <span className={styles.scoreLabel}>match</span>
      </div>
    </div>
  );
}

function MatchCard({ match, rank, exiting, onAccept, onDismiss, onView }) {
  const score = Math.round((match.finalScore || match.score || 0) * 100);
  const skills = match.skills || match.topSkills || [];

  return (
    <Card className={`${styles.matchCard} ${exiting ? styles.cardExit : ''}`}>
      <div className={styles.cardHeader}>
        <Avatar
          name={match.username || match.displayName || `Match ${rank}`}
          size="lg"
          status={match.availability === 'OPEN' ? 'online' : 'away'}
          ring
        />
        <div className={styles.headerInfo}>
          <div className={styles.matchName}>{match.displayName || match.username || `Match #${rank}`}</div>
          <div className={styles.matchMeta}>
            {[match.experienceLevel, match.timezone, match.goalType]
              .filter(Boolean).join(' · ')}
          </div>
        </div>
        <ScoreRing value={score} />
      </div>

      {match.bio && <p className={styles.matchBio}>{match.bio}</p>}

      {skills.length > 0 && (
        <div className={styles.skills}>
          {skills.slice(0, 6).map((s) => (
            <Badge key={typeof s === 'string' ? s : s.name} size="sm" variant="outline">
              {typeof s === 'string' ? s : s.name}
            </Badge>
          ))}
          {skills.length > 6 && <Badge size="sm" variant="default">+{skills.length - 6}</Badge>}
        </div>
      )}

      {(match.semanticScore != null || match.collabScore != null || match.goalScore != null) && (
        <div className={styles.breakdownRow}>
          {match.semanticScore != null && (
            <span className={styles.breakdownItem}><BrainCircuit size={12} /> Semantic {Math.round(match.semanticScore * 100)}</span>
          )}
          {match.collabScore != null && (
            <span className={styles.breakdownItem}><Users size={12} /> Collab {Math.round(match.collabScore * 100)}</span>
          )}
          {match.goalScore != null && (
            <span className={styles.breakdownItem}><Target size={12} /> Goal {Math.round(match.goalScore * 100)}</span>
          )}
        </div>
      )}

      <div className={styles.cardActions}>
        {onView && (
          <Button variant="ghost" size="sm" onClick={onView} leftIcon={<Eye size={14} />}>
            Profile
          </Button>
        )}
        {onDismiss && (
          <Button variant="outline" size="sm" onClick={onDismiss} leftIcon={<XCircle size={14} />}>
            Pass
          </Button>
        )}
        {onAccept && (
          <Button size="sm" onClick={onAccept} leftIcon={<CheckCircle2 size={14} />}>
            Connect
          </Button>
        )}
      </div>
    </Card>
  );
}

function ProfileDetail({ match }) {
  const skills = match.skills || match.topSkills || [];
  return (
    <div className={styles.profileDetail}>
      <div className={styles.detailHeader}>
        <Avatar
          name={match.displayName || match.username}
          size="xl"
          status={match.availability === 'OPEN' ? 'online' : 'away'}
          ring
        />
        <div>
          <h3>{match.displayName || match.username}</h3>
          {match.bio && <p className={styles.detailBio}>{match.bio}</p>}
        </div>
      </div>
      <div className={styles.detailGrid}>
        {match.experienceLevel && (
          <div className={styles.detailField}>
            <span className={styles.detailLabel}>Experience</span>
            <Badge variant="outline">{match.experienceLevel}</Badge>
          </div>
        )}
        {match.goalType && (
          <div className={styles.detailField}>
            <span className={styles.detailLabel}>Goal</span>
            <Badge variant="brand">{match.goalType}</Badge>
          </div>
        )}
        {match.availability && (
          <div className={styles.detailField}>
            <span className={styles.detailLabel}>Availability</span>
            <Badge variant={match.availability === 'OPEN' ? 'success' : 'outline'}>{match.availability}</Badge>
          </div>
        )}
        {match.timezone && (
          <div className={styles.detailField}>
            <span className={styles.detailLabel}>Timezone</span>
            <span><Clock size={14} /> {match.timezone}</span>
          </div>
        )}
      </div>
      {skills.length > 0 && (
        <div>
          <span className={styles.detailLabel}>Skills</span>
          <div className={styles.skills} style={{ marginTop: 'var(--space-sm)' }}>
            {skills.map((s) => (
              <Badge key={typeof s === 'string' ? s : s.name} variant="outline">
                {typeof s === 'string' ? s : s.name}
              </Badge>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

const DEMO_MATCHES = [
  {
    userId: '1', displayName: 'Ada Lovelace', username: 'ada',
    bio: 'Backend engineer passionate about systems programming and algorithms.',
    experienceLevel: 'EXPERT', timezone: 'UTC+0', goalType: 'BUILDING', availability: 'OPEN',
    skills: ['Rust', 'PostgreSQL', 'Docker', 'Linux'],
    finalScore: 0.94, semanticScore: 0.91, collabScore: 0.96, goalScore: 0.95,
  },
  {
    userId: '2', displayName: 'Grace Hopper', username: 'grace',
    bio: 'Full-stack lead building tools for developer collaboration.',
    experienceLevel: 'ADVANCED', timezone: 'UTC-5', goalType: 'BUILDING', availability: 'SELECTIVE',
    skills: ['React', 'Go', 'GraphQL', 'Kubernetes'],
    finalScore: 0.89, semanticScore: 0.87, collabScore: 0.92, goalScore: 0.88,
  },
  {
    userId: '3', displayName: 'Alan Turing', username: 'alan',
    bio: 'ML engineer exploring the intersection of AI and developer tools.',
    experienceLevel: 'ADVANCED', timezone: 'UTC+1', goalType: 'LEARNING', availability: 'OPEN',
    skills: ['Python', 'PyTorch', 'FastAPI', 'TypeScript'],
    finalScore: 0.86, semanticScore: 0.92, collabScore: 0.78, goalScore: 0.88,
  },
  {
    userId: '4', displayName: 'Linus Torvalds', username: 'linus',
    bio: 'Open-source enthusiast, kernel hacker, Git maintainer.',
    experienceLevel: 'EXPERT', timezone: 'UTC-8', goalType: 'MENTORING', availability: 'BUSY',
    skills: ['C', 'Linux', 'Git', 'Bash', 'Make'],
    finalScore: 0.82, semanticScore: 0.85, collabScore: 0.75, goalScore: 0.86,
  },
];
