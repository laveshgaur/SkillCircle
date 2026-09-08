import { cn } from '../../lib/cn';

/**
 * SkillCircle brand mark — interlocking circles suggesting connection/matching,
 * filled with the brand gradient. Decorative; the wordmark carries the label.
 */
export default function Logo({ size = 28, withWordmark = true, className }) {
  const gradId = 'sc-logo-grad';
  return (
    <span
      className={cn('row', className)}
      style={{ gap: 'var(--space-sm)', color: 'var(--color-text)' }}
    >
      <svg
        width={size}
        height={size}
        viewBox="0 0 32 32"
        fill="none"
        role="img"
        aria-label="SkillCircle"
      >
        <defs>
          <linearGradient id={gradId} x1="0" y1="0" x2="32" y2="32" gradientUnits="userSpaceOnUse">
            <stop stopColor="#aa3bff" />
            <stop offset="1" stopColor="#7312bb" />
          </linearGradient>
        </defs>
        <circle cx="12" cy="16" r="9" stroke={`url(#${gradId})`} strokeWidth="3" />
        <circle cx="20" cy="16" r="9" stroke={`url(#${gradId})`} strokeWidth="3" opacity="0.55" />
      </svg>
      {withWordmark && (
        <span
          style={{
            fontWeight: 'var(--weight-bold)',
            fontSize: 'var(--text-lg)',
            letterSpacing: '-0.02em',
          }}
        >
          Skill<span className="text-gradient">Circle</span>
        </span>
      )}
    </span>
  );
}
