import { useEffect, useRef, useCallback } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { Client, type IMessage } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useAuthStore } from '../store/authStore'

/**
 * WHY @stomp/stompjs + SockJS?
 * @stomp/stompjs: STOMP client for JavaScript. Handles: connect, subscribe,
 *   reconnect, heartbeats, frame parsing. Zero boilerplate vs raw WebSocket.
 * SockJS: fallback transport for corporate networks that block WebSocket.
 *
 * WHY reconnect logic inside the hook?
 * Network blips happen. Without auto-reconnect, agents lose real-time updates
 * silently — they don't know they've disconnected. The STOMP client handles
 * reconnect automatically with exponential backoff.
 *
 * WHY integrate with React Query?
 * When a "ticket.assigned" notification arrives → invalidate the tickets query.
 * The dashboard instantly reflects the new assignment without manual refresh.
 * Real-time + cache invalidation = always-fresh UI with no polling.
 */
export function useNotifications() {
  const agent = useAuthStore((s) => s.agent)
  const queryClient = useQueryClient()
  const clientRef = useRef<Client | null>(null)

  const handleMessage = useCallback((message: IMessage) => {
    try {
      const payload = JSON.parse(message.body)

      switch (payload.type) {
        case 'TICKET_ASSIGNED':
        case 'TICKET_RESOLVED':
        case 'TICKET_CREATED':
          // Invalidate relevant queries → React Query refetches → UI updates
          queryClient.invalidateQueries({ queryKey: ['tickets'] })
          showBrowserNotification(payload)
          break

        case 'AI_SUGGESTION_READY':
          // Invalidate just this ticket's AI suggestion
          queryClient.invalidateQueries({ queryKey: ['ai-suggestion', payload.ticketId] })
          break
      }
    } catch (e) {
      console.error('Failed to parse notification:', e)
    }
  }, [queryClient])

  useEffect(() => {
    if (!agent?.id) return

    const client = new Client({
      // WHY SockJS factory instead of direct WebSocket URL?
      // Enables SockJS fallback transports for environments that block WebSocket.
      webSocketFactory: () => new SockJS('/ws'),
      reconnectDelay: 5000,        // Wait 5s before reconnecting after disconnect
      heartbeatIncoming: 10000,    // Expect a heartbeat from server every 10s
      heartbeatOutgoing: 10000,    // Send a heartbeat to server every 10s

      onConnect: () => {
        console.log('[WebSocket] Connected')

        // Subscribe to broadcast channel (all agents)
        client.subscribe('/topic/notifications', handleMessage)

        // Subscribe to private channel (only this agent)
        client.subscribe('/user/queue/notifications', handleMessage)
      },

      onDisconnect: () => {
        console.log('[WebSocket] Disconnected — will reconnect in 5s')
      },

      onStompError: (frame) => {
        console.error('[WebSocket] STOMP error:', frame.headers['message'])
      },
    })

    client.activate()
    clientRef.current = client

    // Cleanup on unmount or agent change
    return () => {
      client.deactivate()
      clientRef.current = null
    }
  }, [agent?.id, handleMessage])
}

function showBrowserNotification(payload: { type: string; title?: string }) {
  // WHY browser notifications?
  // Agents may have the dashboard in a background tab.
  // Browser notification ensures they see critical ticket assignments
  // even when not actively looking at the app.
  if ('Notification' in window && Notification.permission === 'granted') {
    new Notification('PulseAI', {
      body: formatNotificationBody(payload),
      icon: '/favicon.ico',
    })
  }
}

function formatNotificationBody(payload: { type: string; title?: string }): string {
  switch (payload.type) {
    case 'TICKET_ASSIGNED': return `New ticket assigned to you`
    case 'TICKET_CREATED': return `New ticket: ${payload.title ?? ''}`
    case 'TICKET_RESOLVED': return `Ticket resolved`
    default: return 'New notification'
  }
}
