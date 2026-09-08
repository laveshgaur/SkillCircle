import { Link } from 'react-router-dom';
import {
  Sparkles,
  Users,
  BrainCircuit,
  KanbanSquare,
  ShieldCheck,
  ArrowRight,
} from 'lucide-react';
import Navbar from '../components/layout/Navbar';
import { Button, Card, Badge, Avatar } from '../components/ui';
import { GithubIcon } from '../components/icons/BrandIcons';
import styles from './Landing.module.css';

const FEATURES = [
  {
    icon: Sparkles,
    title: 'Intelligent matching',
    text: 'A two-stage AI pipeline surfaces collaborators who actually fit — by skills, goals, and working style, not just keywords.',
  },
  {
    icon: Users,
    title: 'Real-time community',
    text: 'Topic spaces, threads, and live chat keep momentum going. Find your people and build in the open.',
  },
  {
    icon: BrainCircuit,
    title: 'AI insights',
    text: 'Automatic thread summaries and skill extraction from your GitHub keep profiles sharp and conversations digestible.',
  },
  {
    icon: KanbanSquare,
    title: 'Project boards',
    text: 'Turn a match into momentum. Spin up a project, invite collaborators, and track the work together.',
  },
  {
    icon: GithubIcon,
    title: 'GitHub sync',
    text: 'Connect your account and let SkillCircle read your languages, topics, and READMEs to map what you actually build.',
  },
  {
    icon: ShieldCheck,
    title: 'Secure by default',
    text: 'JWT sessions with rotating refresh tokens and OAuth sign-in. Your identity stays yours.',
  },
];

const STEPS = [
  {
    title: 'Semantic retrieval',
    text: 'We embed your profile and goals, then pull the most semantically relevant developers from the community.',
  },
  {
    title: 'Collaborative re-ranking',
    text: 'Candidates are re-scored on collaboration signals and goal alignment — the people you can genuinely build with rise to the top.',
  },
  {
    title: 'Hard filters',
    text: 'Timezone, availability, and experience constraints are applied last, so every match is realistic to act on.',
  },
];

const METRICS = [
  { num: '0.70+', label: 'Precision@5 on curated matches' },
  { num: '0.75+', label: 'NDCG@10 ranking quality' },
  { num: '30%+', label: 'Matches that turn into collaboration' },
];

export default function Landing() {
  return (
    <>
      <Navbar />

      {/* ---------------- Hero ---------------- */}
      <section className={styles.hero}>
        <div className={styles.heroInner}>
          <div>
            <Badge variant="brand" dot className={styles.eyebrow}>
              AI-driven developer matching
            </Badge>
            <h1 className={styles.h1}>
              Find the people worth <span className="text-gradient">building with</span>.
            </h1>
            <p className={styles.lede}>
              SkillCircle pairs developers by skills, goals, and working style — then gives
              you the community and tools to turn a match into a shipped project.
            </p>
            <div className={styles.ctaRow}>
              <Button as={Link} to="/register" size="lg" rightIcon={<ArrowRight size={18} />}>
                Get started free
              </Button>
              <Button as="a" href="#how" variant="outline" size="lg">
                See how it works
              </Button>
            </div>
            <div className={styles.proof}>
              <div className={styles.proofItem}>
                <span className={styles.proofNum}>2-stage</span>
                <span className={styles.proofLabel}>matching pipeline</span>
              </div>
              <div className={styles.proofItem}>
                <span className={styles.proofNum}>Real-time</span>
                <span className={styles.proofLabel}>community chat</span>
              </div>
              <div className={styles.proofItem}>
                <span className={styles.proofNum}>GitHub</span>
                <span className={styles.proofLabel}>skill sync</span>
              </div>
            </div>
          </div>

          {/* Stylized match preview */}
          <div className={styles.heroVisual} aria-hidden="true">
            <Card glass padded className={`${styles.matchCard} ${styles.floatA}`}>
              <Avatar name="Ada Lovelace" size="lg" status="online" ring />
              <div className={styles.matchInfo}>
                <div className={styles.matchName}>Ada Lovelace</div>
                <div className={styles.matchMeta}>Backend · UTC+0 · Senior</div>
                <div className={styles.matchTags}>
                  <Badge size="sm" variant="outline">Rust</Badge>
                  <Badge size="sm" variant="outline">Postgres</Badge>
                </div>
              </div>
              <div className={styles.score}>
                <span className={styles.scoreNum}>94</span>
                <span className={styles.scoreLabel}>match</span>
              </div>
            </Card>

            <Card glass padded className={`${styles.matchCard} ${styles.floatB}`}>
              <Avatar name="Grace Hopper" size="lg" status="busy" ring />
              <div className={styles.matchInfo}>
                <div className={styles.matchName}>Grace Hopper</div>
                <div className={styles.matchMeta}>Full-stack · UTC-5 · Lead</div>
                <div className={styles.matchTags}>
                  <Badge size="sm" variant="outline">React</Badge>
                  <Badge size="sm" variant="outline">Go</Badge>
                </div>
              </div>
              <div className={styles.score}>
                <span className={styles.scoreNum}>89</span>
                <span className={styles.scoreLabel}>match</span>
              </div>
            </Card>

            <Card glass padded className={`${styles.matchCard} ${styles.floatC}`}>
              <Avatar name="Alan Turing" size="lg" status="online" ring />
              <div className={styles.matchInfo}>
                <div className={styles.matchName}>Alan Turing</div>
                <div className={styles.matchMeta}>ML · UTC+1 · Mid</div>
                <div className={styles.matchTags}>
                  <Badge size="sm" variant="outline">Python</Badge>
                  <Badge size="sm" variant="outline">PyTorch</Badge>
                </div>
              </div>
              <div className={styles.score}>
                <span className={styles.scoreNum}>86</span>
                <span className={styles.scoreLabel}>match</span>
              </div>
            </Card>
          </div>
        </div>
      </section>

      {/* ---------------- Features ---------------- */}
      <section id="features" className={styles.section}>
        <div className={styles.sectionHead}>
          <h2 className={styles.sectionTitle}>Everything you need to collaborate</h2>
          <p className={styles.sectionSub}>
            From the first match to the finished project — SkillCircle is built for the
            whole journey.
          </p>
        </div>
        <div className={styles.features}>
          {FEATURES.map(({ icon: Icon, title, text }) => (
            <Card key={title} padded className={styles.feature}>
              <span className={styles.featureIcon}>
                <Icon size={22} aria-hidden="true" />
              </span>
              <h3 className={styles.featureTitle}>{title}</h3>
              <p className={styles.featureText}>{text}</p>
            </Card>
          ))}
        </div>
      </section>

      {/* ---------------- How it works ---------------- */}
      <section id="how" className={styles.section}>
        <div className={styles.sectionHead}>
          <h2 className={styles.sectionTitle}>How matching works</h2>
          <p className={styles.sectionSub}>
            A transparent, three-stage pipeline — retrieval, re-ranking, then hard
            constraints — so every recommendation is both relevant and realistic.
          </p>
        </div>
        <div className={styles.steps}>
          {STEPS.map((step, i) => (
            <div key={step.title} className={styles.step}>
              <span className={styles.stepNum}>{i + 1}</span>
              <h3 className={styles.stepTitle}>{step.title}</h3>
              <p className={styles.stepText}>{step.text}</p>
            </div>
          ))}
        </div>
      </section>

      {/* ---------------- Metrics ---------------- */}
      <section id="metrics" className={styles.section}>
        <div className={styles.sectionHead}>
          <h2 className={styles.sectionTitle}>Built to actually connect people</h2>
          <p className={styles.sectionSub}>
            We hold matching to measurable quality targets — not vanity metrics.
          </p>
        </div>
        <div className={styles.metrics}>
          {METRICS.map((m) => (
            <Card key={m.label} padded className={styles.metric}>
              <div className={`${styles.metricNum} text-gradient`}>{m.num}</div>
              <div className={styles.metricLabel}>{m.label}</div>
            </Card>
          ))}
        </div>
      </section>

      {/* ---------------- CTA ---------------- */}
      <section className={styles.ctaSection}>
        <div className={styles.ctaBox}>
          <h2 className={styles.ctaTitle}>Ready to find your circle?</h2>
          <p className={styles.ctaText}>
            Join SkillCircle, connect your GitHub, and get your first matches in minutes.
          </p>
          <Button
            as={Link}
            to="/register"
            variant="secondary"
            size="lg"
            rightIcon={<ArrowRight size={18} />}
          >
            Create your account
          </Button>
        </div>
      </section>

      {/* ---------------- Footer ---------------- */}
      <footer className={styles.footer}>
        <div className={styles.footerInner}>
          <span className={styles.footerCopy}>
            © 2026 SkillCircle. Built for developers, by developers.
          </span>
          <nav className={styles.footerLinks} aria-label="Footer">
            <a href="#features">Features</a>
            <a href="#how">How it works</a>
            <Link to="/login">Log in</Link>
            <Link to="/register">Get started</Link>
          </nav>
        </div>
      </footer>
    </>
  );
}
