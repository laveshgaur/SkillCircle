import { cn } from '../../lib/cn';
import styles from './Card.module.css';

/**
 * Card — surface container.
 *
 * Props:
 *  - padded: apply standard inner padding (omit when using Card.Header/Body/Footer)
 *  - interactive: hover lift + focus ring (for clickable cards)
 *  - glass: frosted glassmorphism surface
 *  - as: element/component to render (defaults to 'div')
 *
 * Compose with Card.Header / Card.Body / Card.Footer for structured content.
 */
export default function Card({
  padded = false,
  interactive = false,
  glass = false,
  as: Component = 'div',
  className,
  children,
  ...props
}) {
  return (
    <Component
      className={cn(
        styles.card,
        padded && styles.padded,
        interactive && styles.interactive,
        glass && styles.glass,
        className,
      )}
      {...(interactive && Component !== 'button' ? { tabIndex: 0 } : {})}
      {...props}
    >
      {children}
    </Component>
  );
}

Card.Header = function CardHeader({ title, subtitle, className, children, ...props }) {
  return (
    <div className={cn(styles.header, className)} {...props}>
      {title && <h3 className={styles.title}>{title}</h3>}
      {subtitle && <p className={styles.subtitle}>{subtitle}</p>}
      {children}
    </div>
  );
};

Card.Body = function CardBody({ className, children, ...props }) {
  return (
    <div className={cn(styles.body, className)} {...props}>
      {children}
    </div>
  );
};

Card.Footer = function CardFooter({ className, children, ...props }) {
  return (
    <div className={cn(styles.footer, className)} {...props}>
      {children}
    </div>
  );
};
