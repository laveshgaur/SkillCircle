import { useState, useEffect, useCallback } from 'react';
import {
  Settings as SettingsIcon, Moon, Globe, Bell, Shield, LogOut,
  User, Wifi, Trash2,
} from 'lucide-react';
import { GithubIcon } from '../../components/icons/BrandIcons';
import PageWrapper from '../../components/layout/PageWrapper';
import { Button, Badge, Select, Modal, useToast } from '../../components/ui';
import { useAuthStore } from '../../store/authStore';
import { useUIStore } from '../../store/uiStore';
import { logout } from '../../services/authService';
import { getMyProfile, updateProfile } from '../../services/profileService';
import { useNavigate } from 'react-router-dom';
import styles from './Settings.module.css';

const AVAILABILITY_OPTIONS = [
  { value: 'OPEN', label: 'Open — actively looking for collaborators' },
  { value: 'SELECTIVE', label: 'Selective — open to specific opportunities' },
  { value: 'BUSY', label: 'Busy — not available right now' },
];

const TIMEZONE_OPTIONS = [
  'UTC-12', 'UTC-11', 'UTC-10', 'UTC-9', 'UTC-8', 'UTC-7', 'UTC-6', 'UTC-5',
  'UTC-4', 'UTC-3', 'UTC-2', 'UTC-1', 'UTC+0', 'UTC+1', 'UTC+2', 'UTC+3',
  'UTC+4', 'UTC+5', 'UTC+5:30', 'UTC+6', 'UTC+7', 'UTC+8', 'UTC+9', 'UTC+10',
  'UTC+11', 'UTC+12',
].map((tz) => ({ value: tz, label: tz }));

export default function Settings() {
  const user = useAuthStore((s) => s.user);
  const theme = useUIStore((s) => s.theme);
  const toggleTheme = useUIStore((s) => s.toggleTheme);
  const navigate = useNavigate();
  const toast = useToast();

  const [availability, setAvailability] = useState('OPEN');
  const [timezone, setTimezone] = useState(Intl.DateTimeFormat().resolvedOptions().timeZone);
  const [showLogoutConfirm, setShowLogoutConfirm] = useState(false);
  const [profile, setProfile] = useState(null);

  const loadProfile = useCallback(async () => {
    try {
      const p = await getMyProfile();
      setProfile(p);
      if (p.availability) setAvailability(p.availability);
      if (p.timezone) setTimezone(p.timezone);
    } catch { /* ignore */ }
  }, []);

  useEffect(() => { loadProfile(); }, [loadProfile]);

  const handleAvailabilityChange = async (e) => {
    const val = e.target.value;
    setAvailability(val);
    try {
      await updateProfile({ availability: val });
      toast.success(`Availability set to ${val}`);
    } catch (err) {
      toast.error(err.message || 'Failed to update availability');
    }
  };

  const handleTimezoneChange = async (e) => {
    const val = e.target.value;
    setTimezone(val);
    try {
      await updateProfile({ timezone: val });
      toast.success('Timezone updated');
    } catch (err) {
      toast.error(err.message || 'Failed to update timezone');
    }
  };

  const handleLogout = async () => {
    setShowLogoutConfirm(false);
    await logout();
    navigate('/');
  };

  const currentThemeLabel = theme || (window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light');

  return (
    <PageWrapper title="Settings" subtitle="Account preferences and configuration.">
      <div className={styles.grid}>
        {/* Account */}
        <div className={styles.section}>
          <div className={styles.sectionHeader}>
            <User size={18} />
            <h2 className={styles.sectionTitle}>Account</h2>
          </div>
          <div className={styles.row}>
            <div>
              <div className={styles.rowLabel}>Email</div>
              <div className={styles.rowSub}>{user?.email || '—'}</div>
            </div>
            <Badge size="sm" variant="success" dot>Verified</Badge>
          </div>
          <div className={styles.row}>
            <div>
              <div className={styles.rowLabel}>Username</div>
              <div className={styles.rowSub}>@{user?.username || '—'}</div>
            </div>
          </div>
          <div className={styles.row}>
            <div>
              <div className={styles.rowLabel}>Auth provider</div>
              <div className={styles.rowSub}>{user?.oauthProvider || 'LOCAL'}</div>
            </div>
            <Badge size="sm" variant="brand"><Shield size={12} /> Secure</Badge>
          </div>
        </div>

        {/* Preferences */}
        <div className={styles.section}>
          <div className={styles.sectionHeader}>
            <SettingsIcon size={18} />
            <h2 className={styles.sectionTitle}>Preferences</h2>
          </div>
          <div className={styles.row}>
            <div>
              <div className={styles.rowLabel}>Theme</div>
              <div className={styles.rowSub}>Currently: {currentThemeLabel}</div>
            </div>
            <Button variant="outline" size="sm" onClick={toggleTheme} leftIcon={<Moon size={14} />}>
              Toggle
            </Button>
          </div>
          <div className={styles.row}>
            <div style={{ flex: 1 }}>
              <div className={styles.rowLabel}>Availability</div>
              <div className={styles.rowSub}>How visible you are to the matching engine.</div>
            </div>
            <Select
              value={availability}
              onChange={handleAvailabilityChange}
              options={AVAILABILITY_OPTIONS}
            />
          </div>
          <div className={styles.row}>
            <div style={{ flex: 1 }}>
              <div className={styles.rowLabel}>Timezone</div>
              <div className={styles.rowSub}>Used for matching overlap calculations.</div>
            </div>
            <Select
              value={timezone}
              onChange={handleTimezoneChange}
              options={[
                { value: Intl.DateTimeFormat().resolvedOptions().timeZone, label: `${Intl.DateTimeFormat().resolvedOptions().timeZone} (detected)` },
                ...TIMEZONE_OPTIONS,
              ]}
            />
          </div>
          <div className={styles.row}>
            <div>
              <div className={styles.rowLabel}>Notifications</div>
              <div className={styles.rowSub}>Email + in-app notifications</div>
            </div>
            <Bell size={16} style={{ color: 'var(--color-text-muted)' }} />
          </div>
        </div>

        {/* Connected Accounts */}
        <div className={styles.section}>
          <div className={styles.sectionHeader}>
            <Wifi size={18} />
            <h2 className={styles.sectionTitle}>Connected Accounts</h2>
          </div>
          <div className={styles.row}>
            <div>
              <div className={styles.rowLabel}><Github size={14} /> GitHub</div>
              <div className={styles.rowSub}>
                {profile?.githubUsername
                  ? `Connected as ${profile.githubUsername}`
                  : 'Not connected'}
              </div>
            </div>
            <Badge size="sm" variant={profile?.githubUsername ? 'success' : 'outline'}>
              {profile?.githubUsername ? 'Linked' : 'Not linked'}
            </Badge>
          </div>
          <div className={styles.row}>
            <div>
              <div className={styles.rowLabel}>Google</div>
              <div className={styles.rowSub}>
                {user?.oauthProvider === 'GOOGLE' ? 'Connected via OAuth' : 'Not connected'}
              </div>
            </div>
            <Badge size="sm" variant={user?.oauthProvider === 'GOOGLE' ? 'success' : 'outline'}>
              {user?.oauthProvider === 'GOOGLE' ? 'Linked' : 'Not linked'}
            </Badge>
          </div>
        </div>
      </div>

      {/* Danger zone */}
      <div className={styles.danger}>
        <div className={styles.sectionHeader}>
          <LogOut size={18} />
          <h2 className={styles.sectionTitle}>Session</h2>
        </div>
        <div className={styles.row}>
          <div>
            <div className={styles.rowLabel}>Sign out</div>
            <div className={styles.rowSub}>End your current session on this device.</div>
          </div>
          <Button variant="outline" size="sm" onClick={() => setShowLogoutConfirm(true)} leftIcon={<LogOut size={14} />}>
            Sign out
          </Button>
        </div>
      </div>

      {/* Logout confirmation modal */}
      <Modal
        open={showLogoutConfirm}
        onClose={() => setShowLogoutConfirm(false)}
        title="Sign out?"
        description="You'll need to log in again to access your account."
        size="sm"
      >
        <div style={{ display: 'flex', gap: 'var(--space-sm)', justifyContent: 'flex-end', marginTop: 'var(--space-md)' }}>
          <Button variant="ghost" onClick={() => setShowLogoutConfirm(false)}>Cancel</Button>
          <Button variant="danger" onClick={handleLogout} leftIcon={<LogOut size={14} />}>Sign out</Button>
        </div>
      </Modal>
    </PageWrapper>
  );
}
