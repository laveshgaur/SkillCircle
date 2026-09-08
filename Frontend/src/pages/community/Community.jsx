import { useState, useEffect, useRef, useCallback } from 'react';
import {
  Users, Plus, Hash, Pin, MessageSquare, Send, Wifi, WifiOff, ChevronLeft,
} from 'lucide-react';
import PageWrapper from '../../components/layout/PageWrapper';
import { Button, Card, Badge, Input, Spinner, Avatar, useToast } from '../../components/ui';
import { useAuthStore } from '../../store/authStore';
import {
  getSpaces, createSpace, getThreads, createThread, getMessages, getOnlineUsers,
} from '../../services/communityService';
import {
  connect, subscribeToThread, sendMessage, subscribeToTyping,
  sendTyping, sendStopTyping, disconnect,
} from '../../lib/ws';
import styles from './Community.module.css';

export default function Community() {
  const user = useAuthStore((s) => s.user);
  const token = useAuthStore((s) => s.accessToken);
  const toast = useToast();

  const [spaces, setSpaces] = useState([]);
  const [selectedSpaceId, setSelectedSpaceId] = useState(null);
  const [threads, setThreads] = useState([]);
  const [selectedThread, setSelectedThread] = useState(null);
  const [messages, setMessages] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadingMessages, setLoadingMessages] = useState(false);
  const [newSpaceName, setNewSpaceName] = useState('');
  const [newThreadTitle, setNewThreadTitle] = useState('');
  const [messageInput, setMessageInput] = useState('');
  const [sending, setSending] = useState(false);
  const [creatingSpace, setCreatingSpace] = useState(false);
  const [onlineUsers, setOnlineUsers] = useState([]);
  const [typingUsers, setTypingUsers] = useState([]);
  const [wsConnected, setWsConnected] = useState(false);

  const messagesEndRef = useRef(null);
  const typingTimerRef = useRef(null);
  const unsubMsgRef = useRef(null);
  const unsubTypingRef = useRef(null);

  // Load spaces on mount
  useEffect(() => {
    loadSpaces();
    loadOnlineUsers();
    return () => disconnect();
  }, []);

  // Load threads when space changes
  useEffect(() => {
    if (selectedSpaceId) loadThreads(selectedSpaceId);
    setSelectedThread(null);
    setMessages([]);
  }, [selectedSpaceId]);

  // Load messages and subscribe to WebSocket when thread changes
  useEffect(() => {
    if (!selectedThread) return;
    loadMessages(selectedThread.id);
    connectAndSubscribe(selectedThread.id);
    return () => {
      if (unsubMsgRef.current) unsubMsgRef.current();
      if (unsubTypingRef.current) unsubTypingRef.current();
    };
  }, [selectedThread?.id]);

  // Auto-scroll to bottom on new messages
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const loadSpaces = async () => {
    try {
      const data = await getSpaces();
      const list = Array.isArray(data) ? data : [];
      setSpaces(list);
      if (list.length > 0 && !selectedSpaceId) setSelectedSpaceId(list[0].id);
    } catch (e) {
      console.error('Failed to load spaces', e);
      setSpaces(DEMO_SPACES);
      setSelectedSpaceId(DEMO_SPACES[0].id);
    } finally {
      setLoading(false);
    }
  };

  const loadThreads = async (spaceId) => {
    try {
      const data = await getThreads(spaceId);
      setThreads(Array.isArray(data) ? data : data?.content || []);
    } catch (e) {
      console.error('Failed to load threads', e);
      setThreads(DEMO_THREADS);
    }
  };

  const loadMessages = async (threadId) => {
    setLoadingMessages(true);
    try {
      const data = await getMessages(threadId);
      const list = Array.isArray(data) ? data : data?.content || [];
      setMessages(list.reverse()); // API returns newest first, we want oldest first
    } catch (e) {
      console.error('Failed to load messages', e);
      setMessages(DEMO_MESSAGES);
    } finally {
      setLoadingMessages(false);
    }
  };

  const loadOnlineUsers = async () => {
    try {
      const data = await getOnlineUsers();
      setOnlineUsers(Array.isArray(data) ? data : []);
    } catch { /* ignore */ }
  };

  const connectAndSubscribe = async (threadId) => {
    try {
      await connect(token);
      setWsConnected(true);
      unsubMsgRef.current = subscribeToThread(threadId, (msg) => {
        setMessages((prev) => [...prev, msg]);
      });
      unsubTypingRef.current = subscribeToTyping(threadId, (data) => {
        if (data.username !== user?.username) {
          setTypingUsers((prev) => {
            const filtered = prev.filter((u) => u !== data.username);
            if (data.typing !== false) filtered.push(data.username);
            return filtered;
          });
        }
      });
    } catch (e) {
      console.error('WebSocket connection failed', e);
      setWsConnected(false);
    }
  };

  const handleCreateSpace = async () => {
    if (!newSpaceName.trim()) return;
    setCreatingSpace(true);
    try {
      await createSpace({ name: newSpaceName.trim() });
      setNewSpaceName('');
      toast.success('Space created');
      await loadSpaces();
    } catch (e) {
      toast.error(e.message || 'Failed to create space');
    } finally {
      setCreatingSpace(false);
    }
  };

  const handleCreateThread = async () => {
    if (!newThreadTitle.trim() || !selectedSpaceId) return;
    try {
      await createThread(selectedSpaceId, { title: newThreadTitle.trim() });
      setNewThreadTitle('');
      toast.success('Thread created');
      await loadThreads(selectedSpaceId);
    } catch (e) {
      toast.error(e.message || 'Failed to create thread');
    }
  };

  const handleSendMessage = async () => {
    if (!messageInput.trim() || !selectedThread) return;
    const content = messageInput.trim();
    setSending(true);
    setMessageInput('');

    if (wsConnected) {
      sendMessage(selectedThread.id, content);
      sendStopTyping(selectedThread.id);
      setSending(false);
    } else {
      // Fallback: optimistic + REST would go here. For now, add optimistically.
      const optimistic = {
        id: `temp-${Date.now()}`,
        content,
        senderUsername: user?.username,
        createdAt: new Date().toISOString(),
      };
      setMessages((prev) => [...prev, optimistic]);
      setSending(false);
    }
  };

  const handleTyping = () => {
    if (!selectedThread || !wsConnected) return;
    clearTimeout(typingTimerRef.current);
    sendTyping(selectedThread.id);
    typingTimerRef.current = setTimeout(() => {
      sendStopTyping(selectedThread.id);
    }, 2000);
  };

  if (loading) return <PageWrapper title="Community"><Spinner center /></PageWrapper>;

  const selectedSpace = spaces.find((s) => s.id === selectedSpaceId);

  return (
    <PageWrapper title="Community" subtitle="Spaces, threads, and real-time discussion.">
      <div className={styles.layout}>
        {/* Panel 1: Spaces */}
        <div className={`${styles.panel} ${styles.spacesPanel} ${selectedThread ? styles.hideMobile : ''}`}>
          <div className={styles.createForm}>
            <Input
              placeholder="New space name…"
              value={newSpaceName}
              onChange={(e) => setNewSpaceName(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && handleCreateSpace()}
            />
            <Button onClick={handleCreateSpace} size="sm" disabled={creatingSpace}>
              <Plus size={16} />
            </Button>
          </div>
          <div className={styles.spaceList}>
            {spaces.map((space) => (
              <div
                key={space.id}
                className={`${styles.spaceItem} ${space.id === selectedSpaceId ? styles.spaceItemActive : ''}`}
                onClick={() => setSelectedSpaceId(space.id)}
              >
                <span className={styles.spaceIcon}>
                  {space.name?.charAt(0).toUpperCase()}
                </span>
                <div className={styles.spaceInfo}>
                  <div className={styles.spaceName}>{space.name}</div>
                  <div className={styles.spaceMeta}>
                    {space.type || 'PUBLIC'} · {space.threadCount || 0} threads
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Panel 2: Threads */}
        <div className={`${styles.panel} ${styles.threadPanel} ${selectedThread ? styles.hideMobile : ''}`}>
          {selectedSpace && (
            <>
              <div className={styles.panelHeader}>
                <h2 className={styles.panelTitle}>{selectedSpace.name}</h2>
                <Badge size="sm" variant="outline">{selectedSpace.type || 'PUBLIC'}</Badge>
                <div className={styles.wsStatus}>
                  {wsConnected ? <Wifi size={14} /> : <WifiOff size={14} />}
                </div>
              </div>
              {selectedSpace.description && (
                <p className={styles.panelDesc}>{selectedSpace.description}</p>
              )}

              <div className={styles.createForm}>
                <Input
                  placeholder="Start a new thread…"
                  value={newThreadTitle}
                  onChange={(e) => setNewThreadTitle(e.target.value)}
                  onKeyDown={(e) => e.key === 'Enter' && handleCreateThread()}
                />
                <Button onClick={handleCreateThread} size="sm">
                  <Plus size={16} />
                </Button>
              </div>

              {threads.length > 0 ? (
                threads.map((thread) => (
                  <div
                    key={thread.id}
                    className={`${styles.threadItem} ${selectedThread?.id === thread.id ? styles.threadItemActive : ''}`}
                    onClick={() => setSelectedThread(thread)}
                  >
                    <Hash size={16} className={styles.threadHash} />
                    <span className={styles.threadTitle}>{thread.title}</span>
                    {thread.isPinned && <Pin size={14} className={styles.pinBadge} />}
                    <Badge size="sm" variant="default">
                      <MessageSquare size={12} /> {thread.messageCount || 0}
                    </Badge>
                  </div>
                ))
              ) : (
                <div className={styles.empty}>
                  <MessageSquare size={24} />
                  <p>No threads yet. Start a conversation!</p>
                </div>
              )}
            </>
          )}
          {!selectedSpace && (
            <div className={styles.empty}>
              <Users size={24} />
              <p>Select a space or create one to get started.</p>
            </div>
          )}
        </div>

        {/* Panel 3: Messages / Chat */}
        <div className={`${styles.panel} ${styles.chatPanel} ${selectedThread ? styles.showMobile : ''}`}>
          {selectedThread ? (
            <>
              <div className={styles.chatHeader}>
                <button
                  type="button"
                  className={styles.backBtn}
                  onClick={() => setSelectedThread(null)}
                >
                  <ChevronLeft size={18} />
                </button>
                <Hash size={16} />
                <h3 className={styles.chatTitle}>{selectedThread.title}</h3>
              </div>

              <div className={styles.messageList}>
                {loadingMessages && <Spinner center />}
                {!loadingMessages && messages.length === 0 && (
                  <div className={styles.empty}>
                    <MessageSquare size={24} />
                    <p>No messages yet. Say hello!</p>
                  </div>
                )}
                {messages.map((msg, i) => {
                  const isOwn = msg.senderUsername === user?.username;
                  const showAvatar = i === 0 || messages[i - 1]?.senderUsername !== msg.senderUsername;
                  return (
                    <div key={msg.id || i} className={`${styles.message} ${isOwn ? styles.messageOwn : ''}`}>
                      {showAvatar && !isOwn && (
                        <Avatar name={msg.senderUsername || '?'} size="sm" />
                      )}
                      {!showAvatar && !isOwn && <div className={styles.avatarSpacer} />}
                      <div className={styles.msgBody}>
                        {showAvatar && (
                          <div className={styles.msgMeta}>
                            <span className={styles.msgSender}>{msg.senderUsername}</span>
                            <span className={styles.msgTime}>
                              {msg.createdAt ? new Date(msg.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : ''}
                            </span>
                          </div>
                        )}
                        <div className={styles.msgContent}>{msg.content}</div>
                      </div>
                    </div>
                  );
                })}
                <div ref={messagesEndRef} />
              </div>

              {/* Typing indicator */}
              {typingUsers.length > 0 && (
                <div className={styles.typing}>
                  <span className={styles.typingDots}>
                    <span /><span /><span />
                  </span>
                  {typingUsers.join(', ')} {typingUsers.length === 1 ? 'is' : 'are'} typing…
                </div>
              )}

              {/* Compose bar */}
              <div className={styles.compose}>
                <Input
                  placeholder="Type a message…"
                  value={messageInput}
                  onChange={(e) => {
                    setMessageInput(e.target.value);
                    handleTyping();
                  }}
                  onKeyDown={(e) => e.key === 'Enter' && !e.shiftKey && (e.preventDefault(), handleSendMessage())}
                />
                <Button
                  onClick={handleSendMessage}
                  disabled={sending || !messageInput.trim()}
                  leftIcon={<Send size={16} />}
                  size="sm"
                >
                  Send
                </Button>
              </div>
            </>
          ) : (
            <div className={styles.emptyChatPlaceholder}>
              <MessageSquare size={32} />
              <h3>Select a thread</h3>
              <p>Pick a thread from the left to start chatting.</p>
            </div>
          )}
        </div>
      </div>
    </PageWrapper>
  );
}

const DEMO_SPACES = [
  { id: 'd1', name: 'General', type: 'PUBLIC', description: 'General discussion about SkillCircle', threadCount: 3 },
  { id: 'd2', name: 'Java Devs', type: 'PUBLIC', description: 'Everything Java, Spring Boot, JVM', threadCount: 5 },
  { id: 'd3', name: 'Frontend', type: 'PUBLIC', description: 'React, Vue, CSS, and UI/UX', threadCount: 2 },
];

const DEMO_THREADS = [
  { id: 't1', title: 'Best practices for REST API versioning', isPinned: true, messageCount: 12 },
  { id: 't2', title: 'Spring Boot 3.x migration tips', isPinned: false, messageCount: 8 },
  { id: 't3', title: 'Intro thread — say hi! 👋', isPinned: true, messageCount: 24 },
];

const DEMO_MESSAGES = [
  { id: 'm1', content: 'Hey everyone! Welcome to this thread.', senderUsername: 'alice', createdAt: '2026-09-05T10:00:00Z' },
  { id: 'm2', content: 'Thanks! Excited to be here. Anyone working on Spring Boot?', senderUsername: 'bob', createdAt: '2026-09-05T10:05:00Z' },
  { id: 'm3', content: 'Yes! I just migrated to 4.1 — let me know if you have questions.', senderUsername: 'alice', createdAt: '2026-09-05T10:08:00Z' },
];
