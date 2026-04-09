import { create } from 'zustand'
import { persist } from 'zustand/middleware'

/**
 * WHY Zustand over Redux?
 *
 * Redux requires: store + action + reducer + selector + dispatch + Provider.
 * Zustand requires: create() + useStore().
 * For auth state (a few fields) Redux is massive overkill.
 *
 * WHY `persist` middleware?
 * Survives page refresh — reads from localStorage on mount.
 * Agent stays logged in without re-authenticating on every reload.
 *
 * WHY Zustand over React Context?
 * Context re-renders ALL consumers on every state change.
 * Zustand is selector-based: a component that reads `agent.name` only
 * re-renders when `agent.name` changes, not when `agent.id` changes.
 */
interface Agent {
  id: string
  name: string
  email: string
  role: 'AGENT' | 'ADMIN'
}

interface AuthState {
  agent: Agent | null
  isAuthenticated: boolean
  login: (agent: Agent) => void
  logout: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      agent: null,
      isAuthenticated: false,
      login: (agent) => set({ agent, isAuthenticated: true }),
      logout: () => set({ agent: null, isAuthenticated: false }),
    }),
    { name: 'pulseai-auth' }
  )
)
