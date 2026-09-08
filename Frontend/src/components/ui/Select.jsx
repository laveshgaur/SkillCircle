import { forwardRef } from 'react';
import { ChevronDown } from 'lucide-react';
import { cn } from '../../lib/cn';
import styles from './Select.module.css';

/**
 * Select — styled dropdown matching the design system.
 *
 * Props: label, error, helpText, options (array of {value,label} or strings),
 * placeholder, value, onChange, ...rest
 */
const Select = forwardRef(function Select(
  { label, error, helpText, options = [], placeholder, className, id, ...props },
  ref,
) {
  return (
    <div className={cn(styles.field, error && styles.invalid, className)}>
      {label && (
        <label htmlFor={id} className={styles.label}>
          {label}
        </label>
      )}
      <div className={styles.wrap}>
        <select ref={ref} id={id} className={styles.select} {...props}>
          {placeholder && (
            <option value="" disabled>
              {placeholder}
            </option>
          )}
          {options.map((opt) => {
            const val = typeof opt === 'string' ? opt : opt.value;
            const lbl = typeof opt === 'string' ? opt : opt.label;
            return (
              <option key={val} value={val}>
                {lbl}
              </option>
            );
          })}
        </select>
        <ChevronDown size={16} className={styles.chevron} aria-hidden="true" />
      </div>
      {error && (
        <span className={styles.error} role="alert">
          {error}
        </span>
      )}
      {!error && helpText && <span className={styles.help}>{helpText}</span>}
    </div>
  );
});

export default Select;
