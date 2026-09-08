import { forwardRef, useId } from 'react';
import { cn } from '../../lib/cn';
import styles from './Input.module.css';

/**
 * Shared field chrome: label, control slot, help/error text with correct
 * `aria-describedby` / `aria-invalid` wiring. Used by both Input and Textarea.
 */
function Field({
  id,
  label,
  required,
  error,
  helpText,
  leftIcon,
  rightIcon,
  className,
  children,
}) {
  const helpId = helpText ? `${id}-help` : undefined;
  const errorId = error ? `${id}-error` : undefined;
  return (
    <div
      className={cn(
        styles.field,
        error && styles.invalid,
        leftIcon && styles.hasLeft,
        rightIcon && styles.hasRight,
        className,
      )}
    >
      {label && (
        <label htmlFor={id} className={styles.label}>
          {label}
          {required && (
            <span className={styles.required} aria-hidden="true">
              *
            </span>
          )}
        </label>
      )}
      <div className={styles.wrap}>
        {leftIcon && (
          <span className={cn(styles.icon, styles.iconLeft)} aria-hidden="true">
            {leftIcon}
          </span>
        )}
        {children({ helpId, errorId })}
        {rightIcon && (
          <span className={cn(styles.icon, styles.iconRight)}>{rightIcon}</span>
        )}
      </div>
      {error ? (
        <span id={errorId} className={styles.error} role="alert">
          {error}
        </span>
      ) : (
        helpText && (
          <span id={helpId} className={styles.help}>
            {helpText}
          </span>
        )
      )}
    </div>
  );
}

/**
 * Input — labelled text field with inline validation support.
 * Pass `error` (string) to render the invalid state and an alert message.
 */
export const Input = forwardRef(function Input(
  {
    label,
    error,
    helpText,
    required,
    leftIcon,
    rightIcon,
    className,
    id: providedId,
    ...props
  },
  ref,
) {
  const autoId = useId();
  const id = providedId || autoId;
  return (
    <Field
      id={id}
      label={label}
      required={required}
      error={error}
      helpText={helpText}
      leftIcon={leftIcon}
      rightIcon={rightIcon}
      className={className}
    >
      {({ helpId, errorId }) => (
        <input
          ref={ref}
          id={id}
          className={styles.control}
          required={required}
          aria-invalid={error ? 'true' : undefined}
          aria-describedby={errorId || helpId}
          {...props}
        />
      )}
    </Field>
  );
});

/** Textarea — same chrome as Input, multi-line control. */
export const Textarea = forwardRef(function Textarea(
  { label, error, helpText, required, className, id: providedId, rows = 4, ...props },
  ref,
) {
  const autoId = useId();
  const id = providedId || autoId;
  return (
    <Field
      id={id}
      label={label}
      required={required}
      error={error}
      helpText={helpText}
      className={className}
    >
      {({ helpId, errorId }) => (
        <textarea
          ref={ref}
          id={id}
          rows={rows}
          className={cn(styles.control, styles.textarea)}
          required={required}
          aria-invalid={error ? 'true' : undefined}
          aria-describedby={errorId || helpId}
          {...props}
        />
      )}
    </Field>
  );
});

export default Input;
