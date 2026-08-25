import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { useAuth } from '../auth/AuthContext'
import {
  createClass,
  deleteClass,
  enroll,
  listClasses,
  listFaculty,
  updateClass,
} from '../api/client'
import type { ClassRequest, ClassResponse, FacultyResponse } from '../api/types'

const SEMESTERS = ['FALL2026', 'SPRING2026']

const emptyForm: ClassRequest = {
  courseCode: '',
  title: '',
  description: '',
  semester: 'FALL2026',
  facultyId: '',
  capacity: 30,
  room: '',
  dayOfWeek: '',
  startTime: '',
  endTime: '',
}

export default function ClassCatalogPage() {
  const { token, role, studentId } = useAuth()
  const [semester, setSemester] = useState('FALL2026')
  const [classes, setClasses] = useState<ClassResponse[]>([])
  const [faculty, setFaculty] = useState<FacultyResponse[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)

  const [showForm, setShowForm] = useState(false)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [form, setForm] = useState<ClassRequest>(emptyForm)
  const [formError, setFormError] = useState<string | null>(null)

  const [busyRow, setBusyRow] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await listClasses(token, semester)
      setClasses(data)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load classes')
    } finally {
      setLoading(false)
    }
  }, [token, semester])

  useEffect(() => {
    load()
  }, [load])

  useEffect(() => {
    if (role === 'ADMIN') {
      listFaculty(token)
        .then(setFaculty)
        .catch(() => {
          /* faculty list is only needed for the add/edit form; ignore failures here */
        })
    }
  }, [token, role])

  async function handleEnroll(classId: string) {
    if (!studentId) return
    setBusyRow(classId)
    setError(null)
    setNotice(null)
    try {
      await enroll(token, { studentId, classId })
      setNotice('Enrolled successfully.')
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Enrollment failed')
    } finally {
      setBusyRow(null)
    }
  }

  function openAddForm() {
    setEditingId(null)
    setForm({ ...emptyForm, semester })
    setFormError(null)
    setShowForm(true)
  }

  function openEditForm(c: ClassResponse) {
    setEditingId(c.id)
    setForm({
      courseCode: c.courseCode,
      title: c.title,
      description: c.description ?? '',
      semester: c.semester,
      facultyId: c.facultyId,
      capacity: c.capacity,
      room: c.room ?? '',
      dayOfWeek: c.dayOfWeek ?? '',
      startTime: c.startTime ?? '',
      endTime: c.endTime ?? '',
    })
    setFormError(null)
    setShowForm(true)
  }

  async function handleFormSubmit(e: FormEvent) {
    e.preventDefault()
    setFormError(null)
    try {
      if (editingId) {
        await updateClass(token, editingId, form)
      } else {
        await createClass(token, form)
      }
      setShowForm(false)
      await load()
    } catch (err) {
      setFormError(err instanceof Error ? err.message : 'Save failed')
    }
  }

  async function handleDelete(id: string) {
    if (!confirm('Delete this class?')) return
    setBusyRow(id)
    setError(null)
    try {
      await deleteClass(token, id)
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
        <h1>Class Catalog</h1>
        <div className="page-header-actions">
          <label className="field field-inline">
            <span>Semester</span>
            <select value={semester} onChange={(e) => setSemester(e.target.value)}>
              {SEMESTERS.map((s) => (
                <option key={s} value={s}>
                  {s}
                </option>
              ))}
            </select>
          </label>
          {role === 'ADMIN' && (
            <button className="btn btn-primary" onClick={openAddForm}>
              Add Class
            </button>
          )}
        </div>
      </div>

      {error && <div className="banner banner-error">{error}</div>}
      {notice && <div className="banner banner-success">{notice}</div>}

      {showForm && (
        <form className="card form" onSubmit={handleFormSubmit}>
          <h2>{editingId ? 'Edit Class' : 'Add Class'}</h2>
          {formError && <div className="banner banner-error">{formError}</div>}
          <div className="form-grid">
            <label className="field">
              <span>Course code</span>
              <input
                value={form.courseCode}
                onChange={(e) => setForm({ ...form, courseCode: e.target.value })}
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
            <label className="field field-wide">
              <span>Description</span>
              <input
                value={form.description}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
              />
            </label>
            <label className="field">
              <span>Semester</span>
              <select
                value={form.semester}
                onChange={(e) => setForm({ ...form, semester: e.target.value })}
              >
                {SEMESTERS.map((s) => (
                  <option key={s} value={s}>
                    {s}
                  </option>
                ))}
              </select>
            </label>
            <label className="field">
              <span>Faculty</span>
              <select
                value={form.facultyId}
                onChange={(e) => setForm({ ...form, facultyId: e.target.value })}
                required
              >
                <option value="" disabled>
                  Select faculty…
                </option>
                {faculty.map((f) => (
                  <option key={f.id} value={f.id}>
                    {f.firstName} {f.lastName}
                  </option>
                ))}
              </select>
            </label>
            <label className="field">
              <span>Capacity</span>
              <input
                type="number"
                min={1}
                value={form.capacity}
                onChange={(e) => setForm({ ...form, capacity: Number(e.target.value) })}
                required
              />
            </label>
            <label className="field">
              <span>Room</span>
              <input value={form.room} onChange={(e) => setForm({ ...form, room: e.target.value })} />
            </label>
            <label className="field">
              <span>Day of week</span>
              <input
                placeholder="MONDAY"
                value={form.dayOfWeek}
                onChange={(e) => setForm({ ...form, dayOfWeek: e.target.value })}
              />
            </label>
            <label className="field">
              <span>Start time</span>
              <input
                placeholder="09:00"
                value={form.startTime}
                onChange={(e) => setForm({ ...form, startTime: e.target.value })}
              />
            </label>
            <label className="field">
              <span>End time</span>
              <input
                placeholder="10:15"
                value={form.endTime}
                onChange={(e) => setForm({ ...form, endTime: e.target.value })}
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
                <th>Course</th>
                <th>Title</th>
                <th>Faculty</th>
                <th>Schedule</th>
                <th>Seats</th>
                {role === 'STUDENT' && <th></th>}
                {role === 'ADMIN' && <th></th>}
              </tr>
            </thead>
            <tbody>
              {classes.map((c) => (
                <tr key={c.id}>
                  <td>{c.courseCode}</td>
                  <td>{c.title}</td>
                  <td>{c.facultyName}</td>
                  <td>
                    {c.room ?? '—'} {c.dayOfWeek ?? ''} {c.startTime ?? ''}
                    {c.endTime ? `–${c.endTime}` : ''}
                  </td>
                  <td>
                    {c.seatsTaken}/{c.capacity}{' '}
                    {c.seatsAvailable === 0 && <span className="badge badge-full">Full</span>}
                  </td>
                  {role === 'STUDENT' && (
                    <td>
                      <button
                        className="btn btn-primary btn-small"
                        disabled={c.seatsAvailable === 0 || busyRow === c.id}
                        onClick={() => handleEnroll(c.id)}
                      >
                        {c.seatsAvailable === 0 ? 'Full' : busyRow === c.id ? 'Enrolling…' : 'Enroll'}
                      </button>
                    </td>
                  )}
                  {role === 'ADMIN' && (
                    <td className="table-actions">
                      <button className="btn btn-secondary btn-small" onClick={() => openEditForm(c)}>
                        Edit
                      </button>
                      <button
                        className="btn btn-danger btn-small"
                        disabled={busyRow === c.id}
                        onClick={() => handleDelete(c.id)}
                      >
                        Delete
                      </button>
                    </td>
                  )}
                </tr>
              ))}
              {classes.length === 0 && (
                <tr>
                  <td colSpan={6} className="table-empty">
                    No classes found for this semester.
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
