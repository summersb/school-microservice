// TypeScript interfaces matching the backend microservices' response shapes exactly.

export type Role = 'ADMIN' | 'FACULTY' | 'STUDENT'

export interface LoginResponse {
  token: string
  expiresInSeconds: number
  username: string
  role: Role
  studentId: string | null
  facultyId: string | null
}

export type EnrollmentStatus = 'ACTIVE' | 'INACTIVE' | 'GRADUATED' | 'WITHDRAWN'

export interface StudentResponse {
  id: string
  firstName: string
  lastName: string
  email: string
  dateOfBirth: string
  enrollmentStatus: EnrollmentStatus
  createdAt: string
  updatedAt: string
}

export interface StudentRequest {
  firstName: string
  lastName: string
  email: string
  dateOfBirth: string
  enrollmentStatus?: EnrollmentStatus
}

export interface FacultyResponse {
  id: string
  firstName: string
  lastName: string
  email: string
  department: string
  title: string
  hireDate: string
  createdAt: string
  updatedAt: string
}

export interface FacultyRequest {
  firstName: string
  lastName: string
  email: string
  department: string
  title: string
  hireDate: string
}

export interface ClassResponse {
  id: string
  courseCode: string
  title: string
  description: string | null
  semester: string
  facultyId: string
  facultyName: string
  capacity: number
  seatsTaken: number
  seatsAvailable: number
  room: string | null
  dayOfWeek: string | null
  startTime: string | null
  endTime: string | null
}

export interface ClassRequest {
  courseCode: string
  title: string
  description?: string
  semester: string
  facultyId: string
  capacity: number
  room?: string
  dayOfWeek?: string
  startTime?: string
  endTime?: string
}

export type ScheduleStatus = 'ENROLLED' | 'DROPPED' | 'COMPLETED'

export interface ScheduleEntryResponse {
  id: string
  studentId: string
  classId: string
  courseCode: string
  courseTitle: string
  semester: string
  status: ScheduleStatus
  enrolledAt: string
  updatedAt: string
}

export interface EnrollRequest {
  studentId: string
  classId: string
}

export interface EnrollResponse {
  scheduleEntryId: string
  studentId: string
  classId: string
  courseCode: string
  courseTitle: string
  semester: string
  status: ScheduleStatus
}

export interface DropRequest {
  studentId: string
  classId: string
}

export interface DropResponse {
  scheduleEntryId: string
  status: ScheduleStatus
}

export interface ApiErrorBody {
  error: string
  status: number
}
