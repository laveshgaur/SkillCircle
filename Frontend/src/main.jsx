import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import './index.css';
import App from './App.jsx';
import { useUIStore } from './store/uiStore';
import { ToastProvider } from './components/ui';
import ErrorBoundary from './components/layout/ErrorBoundary';

// Apply the persisted (or system) theme before first paint to avoid a flash.
useUIStore.getState().applyTheme();

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <BrowserRouter>
      <ErrorBoundary>
        <ToastProvider>
          <App />
        </ToastProvider>
      </ErrorBoundary>
    </BrowserRouter>
  </StrictMode>,
);
