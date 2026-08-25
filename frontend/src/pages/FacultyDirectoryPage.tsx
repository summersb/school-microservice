import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { useAuth } from '../auth/AuthContext'
import { createFaculty, deleteFaculty, listFaculty, updateFaculty } from '../api/client'
import type { FacultyRequest, FacultyResponse } from '../api/types'

const emptyForm: FacultyRequest = {
  firstName: '',
  lastName: '',
  email: '',
  department: '',
  title: '',
  hireDate: '',
}

export default function FacultyDirectoryPage() {
  const { token, role } = useAuth()
  const [faculty, setFaculty] = useState<FacultyResponse[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const [showForm, setShowForm] = useState(false)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [form, setForm] = useState<FacultyRequest>(emptyForm)
  const [formError, setFormError] = useState<string | null>(null)
  const [busyRow, setBusyRow] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await listFaculty(token)
      setFaculty(data)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load faculty')
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

  function openEditForm(f: FacultyResponse) {
    setEditingId(f.id)
    setForm({
      firstName: f.firstName,
      lastName: f.lastName,
      email: f.email,
      department: f.department,
      title: f.title,
      hireDate: f.hireDate,
    })
    setFormError(null)
    setShowForm(true)
  }

  async function handleFormSubmit(e: FormEvent) {
    e.preventDefault()
    setFormError(null)
    try {
      if (editingId) {
        await updateFaculty(token, editingId, form)
      } else {
        await createFaculty(token, form)
      }
      setShowForm(false)
      await load()
    } catch (err) {
      setFormError(err instanceof Error ? err.message : 'Save failed')
    }
  }

  async function handleDelete(id: string) {
    if (!confirm('Delete this faculty member?')) return
    setBusyRow(id)
    setError(null)
    try {
      await deleteFaculty(token, id)
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
        <h1>Faculty Directory</h1>
        {role === 'ADMIN' && (
          <button className="btn btn-primary" onClick={openAddForm}>
            Add Faculty
          </button>
        )}
      </div>

      {error && <div className="banner banner-error">{error}</div>}

      {showForm && (
        <form className="card form" onSubmit={handleFormSubmit}>
          <h2>{editingId ? 'Edit Faculty' : 'Add Faculty'}</h2>
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
              <span>Department</span>
              <input
                value={form.department}
                onChange={(e) => setForm({ ...form, department: e.target.value })}
                required
              />
            </label>
            <label className="field">
              <span>Title</span>
              <input
                value={form.title}
                onChange={(e) => setForm({ ...form, title: e.target.value })}
                required
              />
            </label>
            <label className="field">
              <span>Hire date</span>
              <input
                type="date"
                value={form.hireDate}
                onChange={(e) => setForm({ ...form, hireDate: e.target.value })}
                required
              />
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
                <th>Department</th>
                <th>Title</th>
                <th>Hire date</th>
                {role === 'ADMIN' && <th></th>}
              </tr>
            </thead>
            <tbody>
              {faculty.map((f) => (
                <tr key={f.id}>
                  <td>
                    {f.firstName} {f.lastName}
                  </td>
                  <td>{f.email}</td>
                  <td>{f.department}</td>
                  <td>{f.title}</td>
                  <td>{f.hireDate}</td>
                  {role === 'ADMIN' && (
                    <td className="table-actions">
                      <button className="btn btn-secondary btn-small" onClick={() => openEditForm(f)}>
                        Edit
                      </button>
                      <button
                        className="btn btn-danger btn-small"
                        disabled={busyRow === f.id}
                        onClick={() => handleDelete(f.id)}
                      >
                        Delete
                      </button>
                    </td>
                  )}
                </tr>
              ))}
              {faculty.length === 0 && (
                <tr>
                  <td colSpan={6} className="table-empty">
                    No faculty found.
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
