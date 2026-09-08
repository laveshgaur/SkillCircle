import { Component } from 'react';
import { AlertTriangle, RefreshCw } from 'lucide-react';
import { Button } from '../ui';
import styles from './ErrorBoundary.module.css';

/**
 * ErrorBoundary — catches React render errors and shows a friendly recovery UI.
 * Place this high in the tree (wrapping <AppLayout> or <App>).
 */
export default class ErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, info) {
    console.error('[ErrorBoundary]', error, info.componentStack);
  }

  handleRetry = () => {
    this.setState({ hasError: false, error: null });
  };

  render() {
    if (this.state.hasError) {
      return (
        <div className={styles.container}>
          <div className={styles.card}>
            <span className={styles.icon}>
              <AlertTriangle size={32} aria-hidden="true" />
            </span>
            <h2 className={styles.title}>Something went wrong</h2>
            <p className={styles.message}>
              An unexpected error occurred. Try refreshing the page or navigating back.
            </p>
            {this.state.error?.message && (
              <code className={styles.detail}>{this.state.error.message}</code>
            )}
            <div className={styles.actions}>
              <Button onClick={this.handleRetry} leftIcon={<RefreshCw size={16} />}>
                Try again
              </Button>
              <Button
                variant="ghost"
                onClick={() => (window.location.href = '/app')}
              >
                Go to Dashboard
              </Button>
            </div>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}
