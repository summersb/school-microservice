import { useCallback, useEffect, useState } from 'react'
import { useAuth } from '../auth/AuthContext'
import { drop, getStudentCurrentSchedule, getStudentScheduleHistory } from '../api/client'
import type { ScheduleEntryResponse } from '../api/types'

export default function MySchedulePage() {
  const { token, studentId } = useAuth()
  const [current, setCurrent] = useState<ScheduleEntryResponse[]>([])
  const [history, setHistory] = useState<ScheduleEntryResponse[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [busyRow, setBusyRow] = useState<string | null>(null)

  const load = useCallback(async () => {
    if (!studentId) return
    setLoading(true)
    setError(null)
    try {
      const [curr, hist] = await Promise.all([
        getStudentCurrentSchedule(token, studentId),
        getStudentScheduleHistory(token, studentId),
      ])
      setCurrent(curr)
      setHistory(hist)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load schedule')
    } finally {
      setLoading(false)
    }
  }, [token, studentId])

  useEffect(() => {
    load()
  }, [load])

  async function handleDrop(entry: ScheduleEntryResponse) {
    if (!studentId) return
    setBusyRow(entry.id)
    setError(null)
    setNotice(null)
    try {
      await drop(token, { studentId, classId: entry.classId })
      setNotice(`Dropped ${entry.courseCode}.`)
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Drop failed')
    } finally {
      setBusyRow(null)
    }
  }

  if (!studentId) {
    return (
      <div className="page">
        <div className="banner banner-error">No student record is associated with this account.</div>
      </div>
    )
  }

  return (
    <div className="page">
      <h1>My Schedule</h1>
      {error && <div className="banner banner-error">{error}</div>}
      {notice && <div className="banner banner-success">{notice}</div>}

      {loading ? (
        <p>Loading…</p>
      ) : (
        <>
          <section>
            <h2>Current classes</h2>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Course</th>
                    <th>Title</th>
                    <th>Semester</th>
                    <th>Status</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {current.map((entry) => (
                    <tr key={entry.id}>
                      <td>{entry.courseCode}</td>
                      <td>{entry.courseTitle}</td>
                      <td>{entry.semester}</td>
                      <td>{entry.status}</td>
                      <td>
                        <button
                          className="btn btn-danger btn-small"
                          disabled={busyRow === entry.id}
                          onClick={() => handleDrop(entry)}
                        >
                          {busyRow === entry.id ? 'Dropping…' : 'Drop'}
                        </button>
                      </td>
                    </tr>
                  ))}
                  {current.length === 0 && (
                    <tr>
                      <td colSpan={5} className="table-empty">
                        No current classes.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </section>

          <section>
            <h2>Past classes</h2>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Course</th>
                    <th>Title</th>
                    <th>Semester</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {history.map((entry) => (
                    <tr key={entry.id}>
                      <td>{entry.courseCode}</td>
                      <td>{entry.courseTitle}</td>
                      <td>{entry.semester}</td>
                      <td>
                        <span className={`badge badge-status-${entry.status.toLowerCase()}`}>
                          {entry.status}
                        </span>
                      </td>
                    </tr>
                  ))}
                  {history.length === 0 && (
                    <tr>
                      <td colSpan={4} className="table-empty">
                        No past classes.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </section>
        </>
      )}
    </div>
  )
}
