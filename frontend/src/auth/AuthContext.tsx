import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react'
import { login as apiLogin } from '../api/client'
import type { Role } from '../api/types'

interface AuthState {
  token: string | null
  username: string | null
  role: Role | null
  studentId: string | null
  facultyId: string | null
}

interface AuthContextValue extends AuthState {
  login: (username: string, password: string) => Promise<void>
  logout: () => void
}

const emptyState: AuthState = {
  token: null,
  username: null,
  role: null,
  studentId: null,
  facultyId: null,
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  // NOTE: token is kept only in-memory React state (no localStorage/sessionStorage) so a page
  // refresh logs the user out. This is intentional for this sample: it avoids relying on browser
  // storage, which can be unreliable in sandboxed preview contexts.
  const [state, setState] = useState<AuthState>(emptyState)

  const login = useCallback(async (username: string, password: string) => {
    const res = await apiLogin(username, password)
    setState({
      token: res.token,
      username: res.username,
      role: res.role,
      studentId: res.studentId,
      facultyId: res.facultyId,
    })
  }, [])

  const logout = useCallback(() => {
    setState(emptyState)
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({ ...state, login, logout }),
    [state, login, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return ctx
}
