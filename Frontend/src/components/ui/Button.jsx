import { forwardRef } from 'react';
import { cn } from '../../lib/cn';
import styles from './Button.module.css';

/**
 * Button — the primary action component.
 *
 * Props:
 *  - variant: 'primary' | 'secondary' | 'ghost' | 'outline' | 'danger'
 *  - size: 'sm' | 'md' | 'lg'
 *  - loading: shows a spinner and disables interaction
 *  - fullWidth: stretches to container width
 *  - leftIcon / rightIcon: React nodes (SVG icons, not emoji)
 *  - as: render a different element/component (e.g. Link) while keeping styles
 */
const Button = forwardRef(function Button(
  {
    variant = 'primary',
    size = 'md',
    loading = false,
    fullWidth = false,
    leftIcon,
    rightIcon,
    className,
    children,
    as: Component = 'button',
    disabled,
    type,
    ...props
  },
  ref,
) {
  const isNativeButton = Component === 'button';
  return (
    <Component
      ref={ref}
      className={cn(
        styles.btn,
        styles[variant],
        styles[size],
        fullWidth && styles.fullWidth,
        className,
      )}
      // Only a native <button> takes `type`/`disabled`; links use aria-disabled.
      {...(isNativeButton
        ? { type: type || 'button', disabled: disabled || loading }
        : { 'aria-disabled': disabled || loading || undefined })}
      aria-busy={loading || undefined}
      {...props}
    >
      {loading ? (
        <span className={styles.loadingLabel}>
          <span className={styles.spinner} aria-hidden="true" />
          {children}
        </span>
      ) : (
        <>
          {leftIcon}
          {children}
          {rightIcon}
        </>
      )}
    </Component>
  );
});

export default Button;
