import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { useAuth } from '../auth/AuthContext'
import { createStudent, deleteStudent, listStudents, updateStudent } from '../api/client'
import type { EnrollmentStatus, StudentRequest, StudentResponse } from '../api/types'

const STATUSES: EnrollmentStatus[] = ['ACTIVE', 'INACTIVE', 'GRADUATED', 'WITHDRAWN']

const emptyForm: StudentRequest = {
  firstName: '',
  lastName: '',
  email: '',
  dateOfBirth: '',
  enrollmentStatus: 'ACTIVE',
}

export default function StudentsPage() {
  const { token, role } = useAuth()
  const [students, setStudents] = useState<StudentResponse[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const [showForm, setShowForm] = useState(false)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [form, setForm] = useState<StudentRequest>(emptyForm)
  const [formError, setFormError] = useState<string | null>(null)
  const [busyRow, setBusyRow] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await listStudents(token)
      setStudents(data)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load students')
    } finally {
      setLoading(false)
    }
  }, [token])

  useEffect(() => {
    load()
  }, [load])

  function openAddForm() {
    setEditingId(null)
    setForm(emptyForm)
    setFormError(null)
    setShowForm(true)
  }

  function openEditForm(s: StudentResponse) {
    setEditingId(s.id)
    setForm({
      firstName: s.firstName,
      lastName: s.lastName,
      email: s.email,
      dateOfBirth: s.dateOfBirth,
      enrollmentStatus: s.enrollmentStatus,
    })
    setFormError(null)
    setShowForm(true)
  }

  async function handleFormSubmit(e: FormEvent) {
    e.preventDefault()
    setFormError(null)
    try {
      if (editingId) {
        await updateStudent(token, editingId, form)
      } else {
        await createStudent(token, form)
      }
      setShowForm(false)
      await load()
    } catch (err) {
      setFormError(err instanceof Error ? err.message : 'Save failed')
    }
  }

  async function handleDelete(id: string) {
    if (!confirm('Delete this student?')) return
    setBusyRow(id)
    setError(null)
    try {
      await deleteStudent(token, id)
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Delete failed')
    } finally {
      setBusyRow(null)
    }
  }

  return (
    <div className="page">
      <div className="page-header">
        <h1>Students</h1>
        {role === 'ADMIN' && (
          <button className="btn btn-primary" onClick={openAddForm}>
            Add Student
          </button>
        )}
      </div>

      {error && <div className="banner banner-error">{error}</div>}

      {showForm && (
        <form className="card form" onSubmit={handleFormSubmit}>
          <h2>{editingId ? 'Edit Student' : 'Add Student'}</h2>
          {formError && <div className="banner banner-error">{formError}</div>}
          <div className="form-grid">
            <label className="field">
              <span>First name</span>
              <input
                value={form.firstName}
                onChange={(e) => setForm({ ...form, firstName: e.target.value })}
                required
              />
            </label>
            <label className="field">
              <span>Last name</span>
              <input
                value={form.lastName}
                onChange={(e) => setForm({ ...form, lastName: e.target.value })}
                required
              />
            </label>
            <label className="field">
              <span>Email</span>
              <input
                type="email"
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
                required
              />
            </label>
            <label className="field">
              <span>Date of birth</span>
              <input
                type="date"
                value={form.dateOfBirth}
                onChange={(e) => setForm({ ...form, dateOfBirth: e.target.value })}
                required
              />
            </label>
            <label className="field">
              <span>Enrollment status</span>
              <select
                value={form.enrollmentStatus}
                onChange={(e) =>
                  setForm({ ...form, enrollmentStatus: e.target.value as EnrollmentStatus })
                }
              >
                {STATUSES.map((s) => (
                  <option key={s} value={s}>
                    {s}
                  </option>
                ))}
              </select>
            </label>
          </div>
          <div className="form-actions">
            <button className="btn btn-primary" type="submit">
              Save
            </button>
            <button className="btn btn-secondary" type="button" onClick={() => setShowForm(false)}>
              Cancel
            </button>
          </div>
        </form>
      )}

      {loading ? (
        <p>Loading…</p>
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>Email</th>
                <th>Date of birth</th>
                <th>Status</th>
                {role === 'ADMIN' && <th></th>}
              </tr>
            </thead>
            <tbody>
              {students.map((s) => (
                <tr key={s.id}>
                  <td>
                    {s.firstName} {s.lastName}
                  </td>
                  <td>{s.email}</td>
                  <td>{s.dateOfBirth}</td>
                  <td>
                    <span className={`badge badge-status-${s.enrollmentStatus.toLowerCase()}`}>
                      {s.enrollmentStatus}
                    </span>
                  </td>
                  {role === 'ADMIN' && (
                    <td className="table-actions">
                      <button className="btn btn-secondary btn-small" onClick={() => openEditForm(s)}>
                        Edit
                      </button>
                      <button
                        className="btn btn-danger btn-small"
                        disabled={busyRow === s.id}
                        onClick={() => handleDelete(s.id)}
                      >
                        Delete
                      </button>
                    </td>
                  )}
                </tr>
              ))}
              {students.length === 0 && (
                <tr>
                  <td colSpan={5} className="table-empty">
                    No students found.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
