import type { ReactNode } from 'react'
import { Navigate, NavLink, Route, Routes } from 'react-router-dom'
import { useAuth } from './auth/AuthContext'
import LoginPage from './pages/LoginPage'
import ClassCatalogPage from './pages/ClassCatalogPage'
import FacultyDirectoryPage from './pages/FacultyDirectoryPage'
import StudentsPage from './pages/StudentsPage'
import MySchedulePage from './pages/MySchedulePage'
import type { Role } from './api/types'

function ProtectedRoute({ children, allow }: { children: ReactNode; allow?: Role[] }) {
  const { token, role } = useAuth()
  if (!token) {
    return <Navigate to="/login" replace />
  }
  if (allow && (!role || !allow.includes(role))) {
    return <div className="banner banner-error">You are not authorized to view this page.</div>
  }
  return <>{children}</>
}

function NavBar() {
  const { token, username, role, logout } = useAuth()

  if (!token) return null

  return (
    <nav className="navbar">
      <div className="navbar-links">
        <NavLink to="/classes" className={({ isActive }) => (isActive ? 'active' : '')}>
          Class Catalog
        </NavLink>
        <NavLink to="/faculty" className={({ isActive }) => (isActive ? 'active' : '')}>
          Faculty Directory
        </NavLink>
        {role === 'STUDENT' && (
          <NavLink to="/schedule" className={({ isActive }) => (isActive ? 'active' : '')}>
            My Schedule
          </NavLink>
        )}
        {(role === 'ADMIN' || role === 'FACULTY') && (
          <NavLink to="/students" className={({ isActive }) => (isActive ? 'active' : '')}>
            Students
          </NavLink>
        )}
      </div>
      <div className="navbar-user">
        <span className="navbar-username">
          {username} <span className="navbar-role">({role})</span>
        </span>
        <button className="btn btn-secondary" onClick={logout}>
          Log out
        </button>
      </div>
    </nav>
  )
}

function HomeRedirect() {
  const { token } = useAuth()
  return <Navigate to={token ? '/classes' : '/login'} replace />
}

export default function App() {
  return (
    <div className="app-shell">
      <NavBar />
      <main className="app-main">
        <Routes>
          <Route path="/" element={<HomeRedirect />} />
          <Route path="/login" element={<LoginPage />} />
          <Route
            path="/classes"
            element={
              <ProtectedRoute>
                <ClassCatalogPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/faculty"
            element={
              <ProtectedRoute>
                <FacultyDirectoryPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/students"
            element={
              <ProtectedRoute allow={['ADMIN', 'FACULTY']}>
                <StudentsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/schedule"
            element={
              <ProtectedRoute allow={['STUDENT']}>
                <MySchedulePage />
              </ProtectedRoute>
            }
          />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </div>
  )
}
