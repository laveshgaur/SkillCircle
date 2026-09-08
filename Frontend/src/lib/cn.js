/**
 * Tiny classNames joiner — filters out falsy values so conditional classes
 * read cleanly: cn(styles.base, isActive && styles.active, className).
 */
export function cn(...classes) {
  return classes.filter(Boolean).join(' ');
}
