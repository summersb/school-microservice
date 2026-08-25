import { useState, type FormEvent } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export default function LoginPage() {
  const { token, login } = useAuth()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  if (token) {
    return <Navigate to="/classes" replace />
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setLoading(true)
    try {
      await login(username, password)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Login failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="page page-narrow">
      <h1>Student Enrollment System</h1>
      <form className="card form" onSubmit={handleSubmit}>
        <h2>Log in</h2>
        {error && <div className="banner banner-error">{error}</div>}
        <label className="field">
          <span>Username</span>
          <input
            type="text"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            autoFocus
            required
          />
        </label>
        <label className="field">
          <span>Password</span>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
        </label>
        <button className="btn btn-primary" type="submit" disabled={loading}>
          {loading ? 'Logging in…' : 'Log in'}
        </button>
      </form>
      <p className="hint">
        Demo accounts (password <code>password123</code>): <code>admin</code> (ADMIN),{' '}
        <code>susan.miller</code> / <code>james.anderson</code> (FACULTY),{' '}
        <code>alice.johnson</code>, <code>brian.smith</code>, <code>carla.diaz</code>,{' '}
        <code>david.kim</code>, <code>emma.wilson</code> (STUDENT).
      </p>
    </div>
  )
}
