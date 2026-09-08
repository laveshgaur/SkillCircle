import { Construction } from 'lucide-react';
import PageWrapper from '../components/layout/PageWrapper';
import { Card } from '../components/ui';
import styles from './Placeholder.module.css';

/**
 * Placeholder — empty-state scaffold for app sections whose full UI arrives in
 * a later phase. Keeps navigation coherent while the shell is in place.
 */
export default function Placeholder({ title, subtitle, icon: Icon = Construction, note }) {
  return (
    <PageWrapper title={title} subtitle={subtitle}>
      <Card padded className={styles.empty}>
        <span className={styles.icon}>
          <Icon size={28} aria-hidden="true" />
        </span>
        <h2 className={styles.title}>Coming soon</h2>
        <p className={styles.text}>
          {note || 'This area is part of the SkillCircle shell — its full experience lands in an upcoming phase.'}
        </p>
      </Card>
    </PageWrapper>
  );
}
