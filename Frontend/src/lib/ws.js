import { Client } from '@stomp/stompjs';

/**
 * WebSocket client for SkillCircle real-time features.
 *
 * Connects to the Spring WebSocket endpoint (`/ws/chat`) over SockJS-compatible
 * raw WebSocket and manages subscriptions for chat messages, typing indicators,
 * and presence.
 */

const WS_URL =
  (import.meta.env.VITE_API_URL || 'http://localhost:8080')
    .replace(/\/api\/v1\/?$/, '')
    .replace(/^http/, 'ws') + '/ws/chat/websocket';

let client = null;
let connectionPromise = null;
const subscriptions = new Map();

/**
 * Get or create the STOMP client and ensure it's connected.
 * Returns the connected Client instance.
 */
export function connect(token) {
  if (client?.connected) return Promise.resolve(client);
  if (connectionPromise) return connectionPromise;

  connectionPromise = new Promise((resolve, reject) => {
    client = new Client({
      brokerURL: WS_URL,
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        connectionPromise = null;
        resolve(client);
      },
      onStompError: (frame) => {
        console.error('[WS] STOMP error', frame);
        connectionPromise = null;
        reject(new Error(frame.headers?.message || 'WebSocket connection failed'));
      },
      onWebSocketClose: () => {
        connectionPromise = null;
      },
    });

    client.activate();
  });

  return connectionPromise;
}

/**
 * Subscribe to a STOMP destination.
 * Returns an unsubscribe function.
 */
export function subscribe(destination, callback) {
  if (!client?.connected) {
    console.warn('[WS] Not connected, queueing subscription for', destination);
    return () => {};
  }

  const sub = client.subscribe(destination, (message) => {
    try {
      const body = JSON.parse(message.body);
      callback(body);
    } catch {
      callback(message.body);
    }
  });

  subscriptions.set(destination, sub);
  return () => {
    sub.unsubscribe();
    subscriptions.delete(destination);
  };
}

/**
 * Send a message to a STOMP destination.
 */
export function send(destination, body) {
  if (!client?.connected) {
    console.warn('[WS] Not connected, cannot send to', destination);
    return;
  }
  client.publish({
    destination,
    body: typeof body === 'string' ? body : JSON.stringify(body),
  });
}

/**
 * Subscribe to thread messages: `/topic/thread.{threadId}`
 */
export function subscribeToThread(threadId, callback) {
  return subscribe(`/topic/thread.${threadId}`, callback);
}

/**
 * Send a chat message: `/app/chat.send`
 */
export function sendMessage(threadId, content) {
  send('/app/chat.send', { threadId, content });
}

/**
 * Send typing indicator: `/app/chat.typing`
 */
export function sendTyping(threadId) {
  send('/app/chat.typing', { threadId });
}

/**
 * Send stop typing: `/app/chat.stopTyping`
 */
export function sendStopTyping(threadId) {
  send('/app/chat.stopTyping', { threadId });
}

/**
 * Subscribe to typing events: `/topic/thread.{threadId}.typing`
 */
export function subscribeToTyping(threadId, callback) {
  return subscribe(`/topic/thread.${threadId}.typing`, callback);
}

/**
 * Disconnect the client and clean up all subscriptions.
 */
export function disconnect() {
  subscriptions.forEach((sub) => sub.unsubscribe());
  subscriptions.clear();
  if (client) {
    client.deactivate();
    client = null;
  }
  connectionPromise = null;
}
