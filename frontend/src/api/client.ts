import type {
  ClassRequest,
  ClassResponse,
  DropRequest,
  DropResponse,
  EnrollRequest,
  EnrollResponse,
  FacultyRequest,
  FacultyResponse,
  LoginResponse,
  ScheduleEntryResponse,
  StudentRequest,
  StudentResponse,
} from './types'

export async function apiFetch<T>(path: string, token: string | null, init?: RequestInit): Promise<T> {
  const res = await fetch(path, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(init?.headers ?? {}),
    },
  })
  if (!res.ok) {
    let message = `Request failed (${res.status})`
    try {
      const body = await res.json()
      if (body?.error) message = body.error
    } catch {
      /* ignore */
    }
    throw new Error(message)
  }
  if (res.status === 204) return undefined as T
  return res.json() as Promise<T>
}

// ---- Auth ----
export function login(username: string, password: string): Promise<LoginResponse> {
  return apiFetch<LoginResponse>('/auth-api/auth/login', null, {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  })
}

// ---- Students ----
export function listStudents(token: string | null): Promise<StudentResponse[]> {
  return apiFetch<StudentResponse[]>('/student-api/students', token)
}

export function getStudent(token: string | null, id: string): Promise<StudentResponse> {
  return apiFetch<StudentResponse>(`/student-api/students/${id}`, token)
}

export function createStudent(token: string | null, body: StudentRequest): Promise<StudentResponse> {
  return apiFetch<StudentResponse>('/student-api/students', token, {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

export function updateStudent(token: string | null, id: string, body: StudentRequest): Promise<StudentResponse> {
  return apiFetch<StudentResponse>(`/student-api/students/${id}`, token, {
    method: 'PUT',
    body: JSON.stringify(body),
  })
}

export function deleteStudent(token: string | null, id: string): Promise<void> {
  return apiFetch<void>(`/student-api/students/${id}`, token, { method: 'DELETE' })
}

// ---- Faculty ----
export function listFaculty(token: string | null): Promise<FacultyResponse[]> {
  return apiFetch<FacultyResponse[]>('/faculty-api/faculty', token)
}

export function getFaculty(token: string | null, id: string): Promise<FacultyResponse> {
  return apiFetch<FacultyResponse>(`/faculty-api/faculty/${id}`, token)
}

export function createFaculty(token: string | null, body: FacultyRequest): Promise<FacultyResponse> {
  return apiFetch<FacultyResponse>('/faculty-api/faculty', token, {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

export function updateFaculty(token: string | null, id: string, body: FacultyRequest): Promise<FacultyResponse> {
  return apiFetch<FacultyResponse>(`/faculty-api/faculty/${id}`, token, {
    method: 'PUT',
    body: JSON.stringify(body),
  })
}

export function deleteFaculty(token: string | null, id: string): Promise<void> {
  return apiFetch<void>(`/faculty-api/faculty/${id}`, token, { method: 'DELETE' })
}

// ---- Classes ----
export function listClasses(token: string | null, semester: string): Promise<ClassResponse[]> {
  return apiFetch<ClassResponse[]>(`/classes-api/classes?semester=${encodeURIComponent(semester)}`, token)
}

export function getClass(token: string | null, id: string): Promise<ClassResponse> {
  return apiFetch<ClassResponse>(`/classes-api/classes/${id}`, token)
}

export function createClass(token: string | null, body: ClassRequest): Promise<ClassResponse> {
  return apiFetch<ClassResponse>('/classes-api/classes', token, {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

export function updateClass(token: string | null, id: string, body: ClassRequest): Promise<ClassResponse> {
  return apiFetch<ClassResponse>(`/classes-api/classes/${id}`, token, {
    method: 'PUT',
    body: JSON.stringify(body),
  })
}

export function deleteClass(token: string | null, id: string): Promise<void> {
  return apiFetch<void>(`/classes-api/classes/${id}`, token, { method: 'DELETE' })
}

// ---- Schedule ----
export function getStudentSchedule(token: string | null, studentId: string): Promise<ScheduleEntryResponse[]> {
  return apiFetch<ScheduleEntryResponse[]>(`/schedule-api/schedules/student/${studentId}`, token)
}

export function getStudentCurrentSchedule(token: string | null, studentId: string): Promise<ScheduleEntryResponse[]> {
  return apiFetch<ScheduleEntryResponse[]>(`/schedule-api/schedules/student/${studentId}/current`, token)
}

export function getStudentScheduleHistory(token: string | null, studentId: string): Promise<ScheduleEntryResponse[]> {
  return apiFetch<ScheduleEntryResponse[]>(`/schedule-api/schedules/student/${studentId}/history`, token)
}

// ---- Register ----
export function enroll(token: string | null, body: EnrollRequest): Promise<EnrollResponse> {
  return apiFetch<EnrollResponse>('/register-api/register/enroll', token, {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

export function drop(token: string | null, body: DropRequest): Promise<DropResponse> {
  return apiFetch<DropResponse>('/register-api/register/drop', token, {
    method: 'POST',
    body: JSON.stringify(body),
  })
}
